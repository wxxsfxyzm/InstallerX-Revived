// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import android.app.Activity
import android.content.Context
import android.os.Build
import com.rosan.installer.R
import com.rosan.installer.data.privileged.service.AutoLockService
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.data.session.resolver.ConfigResolver
import com.rosan.installer.data.session.resolver.SourceResolver
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.domain.engine.exception.AuthenticationFailedException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.SessionMode
import com.rosan.installer.domain.engine.model.sourcePath
import com.rosan.installer.domain.engine.usecase.AnalyzePackageUseCase
import com.rosan.installer.domain.engine.usecase.ApproveSessionUseCase
import com.rosan.installer.domain.engine.usecase.ClearAppIconCacheUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.engine.usecase.GetSessionConfirmationDetailsUseCase
import com.rosan.installer.domain.engine.usecase.ProcessInstallationUseCase
import com.rosan.installer.domain.engine.usecase.ProcessUninstallUseCase
import com.rosan.installer.domain.privileged.provider.ShellExecutionProvider
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.model.UninstallInfo
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.session.repository.NetworkResolver
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel.Companion.default
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.ui.common.auth.safeBiometricAuthOrThrow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File

class ActionHandler(scope: CoroutineScope, session: InstallerSessionRepository) :
    Handler(scope, session), KoinComponent {
    override val session: InstallerSessionRepositoryImpl = super.session as InstallerSessionRepositoryImpl
    private val mutableProgressFlow: MutableSharedFlow<ProgressEntity>
        get() = session.progress

    private var job: Job? = null

    // A separate job for the current heavy task (Resolve, Install, etc.)
    private var processingJob: Job? = null

    // Helper property to get ID for logging
    private val sessionId get() = session.id

    private val context by inject<Context>()
    private val appSettingsRepo by inject<AppSettingsRepo>()
    private val shellExecutionProvider by inject<ShellExecutionProvider>()
    private val deviceCapabilityProvider by inject<DeviceCapabilityProvider>()
    private val autoLockService by inject<AutoLockService>()
    private val configResolver by inject<ConfigResolver>()
    private val networkResolver by inject<NetworkResolver>()
    private val analyzePackage by inject<AnalyzePackageUseCase>()
    private val getAppColor by inject<GetAppIconColorUseCase>()
    private val clearAppIconCache by inject<ClearAppIconCacheUseCase>()
    private val processInstallation by inject<ProcessInstallationUseCase>()
    private val processUninstall by inject<ProcessUninstallUseCase>()
    private val getSessionConfirmationDetails by inject<GetSessionConfirmationDetailsUseCase>()
    private val approveSession by inject<ApproveSessionUseCase>()

    // Cache directory
    private val cacheDirectory = File(context.cacheDir, "installer_sessions/$sessionId")
        .apply { mkdirs() }
        .absolutePath

    // Initializing helpers without passing ID
    private val sourceResolver = SourceResolver(
        context = context,
        networkResolver = networkResolver,
        cacheDirectory = cacheDirectory,
        progressFlow = mutableProgressFlow
    )

    override suspend fun onStart() {
        Timber.d("[id=$sessionId] onStart: Starting to collect actions.")
        job = scope.launch {
            session.action.collect { action ->
                Timber.d("[id=$sessionId] Received action: ${action::class.simpleName}")

                // If the action is Cancel, we handle it immediately by cancelling the processing job.
                when (action) {
                    is InstallerSessionRepositoryImpl.Action.Cancel -> {
                        handleCancel()
                    }

                    is InstallerSessionRepositoryImpl.Action.Finish -> {
                        // Finish should also stop any ongoing work
                        processingJob?.cancel()
                        session.progress.emit(ProgressEntity.Finish)
                    }

                    else -> {
                        // For other actions, we launch a new job to process them.
                        // This prevents the collector from being blocked, allowing Action.Cancel to be received.
                        startProcessingJob(action)
                    }
                }
            }
        }
    }

    private suspend fun handleCancel() {
        Timber.d("[id=$sessionId] handleCancel: Cancelling current processing job.")
        // 1. Cancel the current task
        processingJob?.cancel("User requested cancellation")
        processingJob = null

        // 2. Perform cleanup (same as onFinish/close)
        Timber.d("[id=$sessionId] handleCancel: Cleaning up resources.")
        clearCache()

        // 3. Emit Finish instead of Ready. This effectively closes the session.
        Timber.d("[id=$sessionId] handleCancel: Emitting ProgressEntity.Finish.")
        session.progress.emit(ProgressEntity.Finish)
    }

    private fun startProcessingJob(action: InstallerSessionRepositoryImpl.Action) {
        // Cancel previous job if exists
        processingJob?.cancel("New action received, cancelling old one")

        processingJob = scope.launch {
            runCatching {
                handleAction(action)
            }.onFailure { e ->
                if (e is CancellationException) {
                    Timber.d("[id=$sessionId] Action ${action::class.simpleName} was cancelled.")
                    // Usually we don't need to emit error on cancellation, just stop.
                } else {
                    Timber.e(e, "[id=$sessionId] Action ${action::class.simpleName} failed")
                    session.error = e

                    val errorState = when (action) {
                        is InstallerSessionRepositoryImpl.Action.Install -> ProgressEntity.InstallFailed
                        is InstallerSessionRepositoryImpl.Action.Analyse -> ProgressEntity.InstallAnalysedFailed
                        is InstallerSessionRepositoryImpl.Action.Uninstall -> ProgressEntity.UninstallFailed
                        else -> ProgressEntity.InstallResolvedFailed
                    }

                    val currentState = session.progress.first()
                    // Avoid overwriting a Finish state or existing error loop
                    if (currentState != errorState && currentState !is ProgressEntity.InstallFailed) {
                        Timber.d("[id=$sessionId] Emitting error state: $errorState")
                        session.progress.emit(errorState)
                    }
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=$sessionId] onFinish: Cleaning up resources and cancelling job.")
        clearCache()
        processingJob?.cancel()
        job?.cancel()
    }

    private suspend fun handleAction(action: InstallerSessionRepositoryImpl.Action) {
        // Check for cancellation before starting
        if (!currentCoroutineContext().isActive) return

        when (action) {
            is InstallerSessionRepositoryImpl.Action.ResolveInstall -> resolve(action.activity)
            is InstallerSessionRepositoryImpl.Action.Analyse -> analyse()
            is InstallerSessionRepositoryImpl.Action.Install -> handleSingleInstall(action.triggerAuth)
            is InstallerSessionRepositoryImpl.Action.InstallMultiple -> handleMultiInstall()
            is InstallerSessionRepositoryImpl.Action.ResolveUninstall -> resolveUninstall(action.activity, action.packageName)
            is InstallerSessionRepositoryImpl.Action.Uninstall -> uninstall(action.packageName)
            is InstallerSessionRepositoryImpl.Action.ResolveConfirmInstall -> resolveConfirm(action.activity, action.sessionId)
            // Handle Session Confirmation
            is InstallerSessionRepositoryImpl.Action.ApproveSession -> handleConfirm(action.sessionId, action.granted)
            // Handle Reboot Action
            is InstallerSessionRepositoryImpl.Action.Reboot -> handleReboot(action.reason)
            // Cancel and Finish are handled in the collector directly
            is InstallerSessionRepositoryImpl.Action.Cancel,
            is InstallerSessionRepositoryImpl.Action.Finish -> {
            }
        }
    }

    private suspend fun resolve(activity: Activity) {
        Timber.d("[id=$sessionId] resolve: Starting new task.")
        resetState()
        Timber.d("[id=$sessionId] resolve: State has been reset. Emitting ProgressEntity.InstallResolving.")
        session.progress.emit(ProgressEntity.InstallResolving)

        // Resolve Config
        session.config = configResolver.resolve(activity)

        if (session.config.installMode.isNotification) {
            Timber.d("[id=$sessionId] Notification mode detected. Switching to background.")
            session.background(true)
        }

        // Resolve Data (IO Heavy - Cancellable via SourceResolver)
        Timber.d("[id=$sessionId] resolve: Resolving data URIs...")
        val data = sourceResolver.resolve(activity.intent)

        // Check active after IO
        if (!currentCoroutineContext().isActive) throw CancellationException()

        session.data = data
        Timber.d("[id=$sessionId] resolve: Data resolved successfully (${session.data.size} items).")

        // Post-Resolution Logic
        val forceDialog = data.size > 1 || data.any { it.sourcePath()?.endsWith(".zip", true) == true }
        if (forceDialog) {
            Timber.d("[id=$sessionId] resolve: Batch share or module file detected. Forcing install mode to Dialog.")
            session.config = session.config.copy(installMode = InstallMode.Dialog)
        }

        Timber.d("[id=$sessionId] resolve: Requesting AutoLockManager check.")
        autoLockService.onResolveInstall(session.config.authorizer)

        if (session.config.installMode.isNotification) {
            Timber.d("[id=$sessionId] Notification mode detected early. Switching to background.")
            session.background(true)
        }

        if (session.config.installMode == InstallMode.Ignore) {
            Timber.d("[id=$sessionId] resolve: InstallMode is Ignore. Finishing task.")
            session.progress.emit(ProgressEntity.Finish)
            return
        }

        Timber.d("[id=$sessionId] resolve: Emitting ProgressEntity.InstallResolveSuccess.")
        Timber.d("[id=$sessionId] Final InstallMode before emitting success: ${session.config.installMode}")
        session.progress.emit(ProgressEntity.InstallResolveSuccess)
    }

    private suspend fun analyse() {
        Timber.d("[id=$sessionId] analyse: Starting. Emitting ProgressEntity.InstallAnalysing.")
        session.progress.emit(ProgressEntity.InstallAnalysing)

        val isModuleEnabled = appSettingsRepo.getBoolean(BooleanSetting.LabEnableModuleFlash, false).first()
        Timber.d("[id=$sessionId] Module flashing enabled: $isModuleEnabled")

        val extra = AnalyseExtraEntity(cacheDirectory, isModuleFlashEnabled = isModuleEnabled)

        val results = analyzePackage(
            sessionId = session.id,
            config = session.config,
            data = session.data,
            extra = extra
        )

        if (results.isEmpty()) {
            throw AnalyseFailedAllFilesUnsupportedException("No valid installation entities found in the provided sources.")
        }

        session.analysisResults = results

        Timber.d("[id=$sessionId] analyse: Emitting ProgressEntity.InstallAnalysedSuccess.")
        session.progress.emit(ProgressEntity.InstallAnalysedSuccess)
    }

    /**
     * Requests the user to perform biometric authentication.
     *
     * This function displays the biometric prompt to the user (fingerprint, face, device credential,
     * or other supported biometrics) and suspends until the user successfully authenticates.
     * The prompt's subtitle changes depending on whether this is for an install or uninstall action.
     *
     * @param isInstall `true` if authentication is for an install operation, `false` for uninstall.
     *
     * @throws AuthenticationFailedException Thrown if the user fails or cancels biometric authentication.
     */
    private suspend fun requestUserBiometricAuthentication(
        isInstall: Boolean
    ) {
        val requireBiometricAuth =
            if (isInstall) appSettingsRepo.getBoolean(BooleanSetting.InstallerRequireBiometricAuth, false).first()
            else appSettingsRepo.getBoolean(BooleanSetting.UninstallerRequireBiometricAuth, false).first()

        if (!requireBiometricAuth) return

        return context.safeBiometricAuthOrThrow(
            title = context.getString(R.string.auth_to_continue_work),
            subTitle = context.getString(
                if (isInstall)
                    R.string.auth_summary_install
                else
                    R.string.auth_summary_uninstall
            )
        )
    }

    private suspend fun handleSingleInstall(triggerAuth: Boolean) {
        if (triggerAuth) {
            requestUserBiometricAuthentication(true)
        }
        session.moduleLog = emptyList()
        performInstallLogic()
    }

    private suspend fun handleMultiInstall() {
        requestUserBiometricAuthentication(true)
        val queue = session.multiInstallQueue
        if (queue.isEmpty()) return

        val groupedQueue: List<List<SelectInstallEntity>> = queue
            .groupBy { it.app.packageName }
            .values
            .toList()

        // Clear previous logs
        session.moduleLog = emptyList()

        // Loop through the queue
        while (session.currentMultiInstallIndex < groupedQueue.size) {
            if (!currentCoroutineContext().isActive) break

            val appEntities = groupedQueue[session.currentMultiInstallIndex]
            val firstEntity = appEntities.first()

            val currentProgressIndex = session.currentMultiInstallIndex + 1
            val totalCount = groupedQueue.size

            try {
                // Construct a temporary result list for the processor
                val originalResults = session.analysisResults
                val targetResult = findResultForEntity(firstEntity, originalResults)

                if (targetResult != null) {
                    val entitiesToInstall = appEntities.map { it.copy(selected = true) }
                    val tempResults = listOf(targetResult.copy(appEntities = entitiesToInstall))

                    // Perform install
                    processInstallation(
                        config = session.config,
                        analysisResults = tempResults,
                        current = currentProgressIndex,
                        total = totalCount
                    ).collect { progress ->
                        if (progress is ProgressEntity.InstallingModule) {
                            session.moduleLog = progress.output
                        }
                        session.progress.emit(progress)
                    }

                    appEntities.forEach { entity ->
                        session.multiInstallResults.add(InstallResult(entity, true))
                    }
                } else {
                    throw IllegalStateException("Original package info not found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Batch install failed for ${firstEntity.app.packageName}")
                appEntities.forEach { entity ->
                    session.multiInstallResults.add(InstallResult(entity, false, e))
                }
                // Continue to next app even if one fails
            }

            session.currentMultiInstallIndex++
        }

        // Emit final completion state with results
        session.progress.emit(ProgressEntity.InstallCompleted(session.multiInstallResults.toList()))
    }

    /**
     * Finds the [PackageAnalysisResult] for a given [SelectInstallEntity].
     * Returns null if not found.
     * @param target The [SelectInstallEntity] to search for.
     * @param allResults The list of [PackageAnalysisResult] to search in.
     * @return The [PackageAnalysisResult] if found, null otherwise.
     */
    private fun findResultForEntity(
        target: SelectInstallEntity,
        allResults: List<PackageAnalysisResult>
    ): PackageAnalysisResult? {
        return allResults.find { it.packageName == target.app.packageName }
    }

    /**
     * Performs the installation logic.
     */
    private suspend fun performInstallLogic() {
        Timber.d("[id=$sessionId] install: Starting installation process via UseCase.")

        processInstallation(
            config = session.config,
            analysisResults = session.analysisResults
        ).collect { progress ->
            // Sync module logs back to the session repository if applicable
            if (progress is ProgressEntity.InstallingModule) {
                session.moduleLog = progress.output
            }
            // Emit progress to the UI layer
            session.progress.emit(progress)
        }

        // Cache cleanup strategy
        val mode = session.analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
        if (mode == SessionMode.Single) {
            Timber.d("[id=$sessionId] Single-app install succeeded. Clearing cache now.")
            clearCache()
        } else {
            Timber.d("[id=$sessionId] Multi-app install step succeeded. Deferring cache cleanup.")
        }
    }

    private suspend fun resolveConfirm(activity: Activity, sessionId: Int) {
        Timber.d("[id=$sessionId] resolveConfirmInstall: Starting for session $sessionId.")
        session.config = configResolver.resolve(activity)

        val details = getSessionConfirmationDetails(sessionId, session.config)
        session.confirmationDetails.value = details

        Timber.d("[id=$sessionId] resolveConfirmInstall: Success. Emitting InstallConfirming.")
        session.progress.emit(ProgressEntity.InstallConfirming)
    }

    private suspend fun resolveUninstall(activity: Activity, packageName: String) {
        Timber.d("[id=$sessionId] resolveUninstall: Starting for $packageName.")
        session.config = configResolver.resolve(activity)
        session.progress.emit(ProgressEntity.UninstallResolving)

        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val pInfo = pm.getPackageInfo(packageName, 0)

        val color = if (appSettingsRepo.getBoolean(BooleanSetting.UiDynColorFollowPkgIcon, false).first()) {
            getAppColor(
                sessionId = sessionId,
                packageName = packageName,
                preferSystemIcon = true
            )
        } else null

        session.uninstallInfo.update {
            UninstallInfo(
                packageName = packageName,
                appLabel = pm.getApplicationLabel(appInfo).toString(),
                versionName = pInfo.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode
                else @Suppress("DEPRECATION") pInfo.versionCode.toLong(),
                seedColor = color
            )
        }

        Timber.d("[id=$sessionId] resolveUninstall: Success. Emitting UninstallReady.")
        session.progress.emit(ProgressEntity.UninstallReady)
    }

    private suspend fun uninstall(packageName: String) {
        requestUserBiometricAuthentication(false)
        Timber.d("[id=$sessionId] uninstall: Starting for $packageName. Emitting ProgressEntity.Uninstalling.")
        session.progress.emit(ProgressEntity.Uninstalling)

        processUninstall(
            config = session.config,
            packageName = packageName
        )
        Timber.d("[id=$sessionId] uninstall: Succeeded for $packageName. Emitting ProgressEntity.UninstallSuccess.")
        session.progress.emit(ProgressEntity.UninstallSuccess)
    }

    private suspend fun handleConfirm(sessionId: Int, granted: Boolean) {
        Timber.d("[id=$sessionId] ApproveSession: $granted for session $sessionId")
        approveSession(
            sessionId = sessionId,
            granted = granted,
            config = session.config
        )
        session.progress.emit(ProgressEntity.Finish)
    }

    private suspend fun handleReboot(reason: String) {
        Timber.d("[id=$sessionId] handleReboot: Starting cleanup before reboot.")
        val systemUseRoot = deviceCapabilityProvider.isSystemApp && appSettingsRepo.getBoolean(BooleanSetting.LabModuleAlwaysRoot, false).first()
        if (systemUseRoot) session.config = session.config.copy(authorizer = Authorizer.Root)
        // Execute cleanup immediately
        // Call clearCache() explicitly to ensure temporary files are removed
        // before the system goes down
        clearCache()

        Timber.d("[id=$sessionId] handleReboot: Cleanup finished. Executing reboot command.")

        // Execute the reboot command
        withContext(Dispatchers.IO) {
            val cmd = if (reason == "recovery") {
                // KEYCODE_POWER = 26. Hides incorrect "Factory data reset" message in recovery
                "input keyevent 26 ; svc power reboot $reason || reboot $reason"
            } else {
                val reasonArg = if (reason.isNotEmpty()) " $reason" else ""
                "svc power reboot$reasonArg || reboot$reasonArg"
            }

            val commandArray = arrayOf("sh", "-c", cmd)

            shellExecutionProvider.executeCommandArray(session.config, commandArray)
        }

        session.progress.emit(ProgressEntity.Finish)
    }

    private fun resetState() {
        session.error = Throwable()
        session.config = default
        session.data = emptyList()
        session.analysisResults = emptyList()
        session.progress.tryEmit(ProgressEntity.Ready)
    }

    private suspend fun clearCache() {
        Timber.d("[id=$sessionId] clearCacheDirectory: Clearing cache...")

        // 1. Clear file system trackers
        sourceResolver.getTrackedCloseables().forEach { runCatching { it.close() } }

        // 2. Clear physical cache files
        File(cacheDirectory).runCatching {
            if (exists()) deleteRecursively()
        }

        // 3. Use UseCase to clear memory icon cache for this specific session
        clearAppIconCache(sessionId = sessionId)

        // 4. Ensure the global system cache is also refreshed
        val lastProgress = session.progress.replayCache.firstOrNull()
        when (lastProgress) {
            is ProgressEntity.InstallSuccess, is ProgressEntity.UninstallSuccess -> {
                session.analysisResults.firstOrNull()?.packageName?.let {
                    clearAppIconCache(packageName = it)
                }
            }

            is ProgressEntity.InstallCompleted -> {
                session.multiInstallResults
                    .filter { it.success }
                    .forEach { result ->
                        clearAppIconCache(packageName = result.entity.app.packageName)
                    }
            }

            else -> {}
        }
    }

    private val InstallMode.isNotification get() = this == InstallMode.Notification || this == InstallMode.AutoNotification
}
