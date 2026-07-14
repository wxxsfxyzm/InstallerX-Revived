// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import androidx.annotation.RequiresApi
import com.rosan.installer.R
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.data.session.resolver.ConfigResolver
import com.rosan.installer.data.session.resolver.SourceResolver
import com.rosan.installer.data.session.resolver.UnarchiveResolver
import com.rosan.installer.data.session.resolver.UninstallResolver
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.exception.AuthenticationFailedException
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.install.sourcePath
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.usecase.AnalyzePackageUseCase
import com.rosan.installer.domain.engine.usecase.ApproveSessionUseCase
import com.rosan.installer.domain.engine.usecase.ClearAppIconCacheUseCase
import com.rosan.installer.domain.engine.usecase.GetSessionConfirmationDetailsUseCase
import com.rosan.installer.domain.engine.usecase.ProcessInstallationUseCase
import com.rosan.installer.domain.engine.usecase.ProcessUninstallUseCase
import com.rosan.installer.domain.privileged.provider.ShellExecutionProvider
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.session.model.InstallResult
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.model.UnarchiveErrorInfo
import com.rosan.installer.domain.session.repository.NetworkResolver
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.domain.settings.model.config.ConfigModel.Companion.default
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.domain.settings.model.config.ToastMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.framework.auth.safeBiometricAuthOrThrow
import com.rosan.installer.framework.packageupdate.SelfUpdateRecoveryManager
import com.rosan.installer.framework.service.AutoLockService
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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

class ActionHandler(
    override val scope: CoroutineScope,
    override val session: InstallerSessionRepositoryImpl
) : Handler, KoinComponent {
    private val mutableProgressFlow: MutableSharedFlow<ProgressEntity>
        get() = session.progress

    private var job: Job? = null

    // A separate job for the current heavy task (Resolve, Install, etc.)
    private var processingJob: Job? = null

    // Helper property to get ID for logging
    private val sessionId get() = session.id

    private val context by inject<Context>()
    private val appSettingsRepo by inject<AppSettingsRepository>()
    private val shellExecutionProvider by inject<ShellExecutionProvider>()
    private val deviceCapabilityProvider by inject<DeviceCapabilityProvider>()
    private val autoLockService by inject<AutoLockService>()
    private val selfUpdateRecoveryManager by inject<SelfUpdateRecoveryManager>()
    private val configResolver by inject<ConfigResolver>()
    private val uninstallResolver by inject<UninstallResolver>()
    private val unarchiveResolver by inject<UnarchiveResolver>()
    private val networkResolver by inject<NetworkResolver>()
    private val analyzePackage by inject<AnalyzePackageUseCase>()
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

                when (action) {
                    is InstallerSessionRepositoryImpl.Action.Cancel -> {
                        handleCancel()
                    }

                    is InstallerSessionRepositoryImpl.Action.Finish -> {
                        processingJob?.cancel()
                        session.progress.emit(ProgressEntity.Finish)
                    }

                    // Handle Confirmation Actions Concurrently
                    is InstallerSessionRepositoryImpl.Action.ResolveConfirmInstall -> {
                        // Launch concurrently, DO NOT cancel the main processingJob (which is likely suspended waiting for commit)
                        scope.launch {
                            runCatching { handleAction(action) }
                                .onFailure { Timber.e(it, "ResolveConfirmInstall failed") }
                        }
                    }

                    is InstallerSessionRepositoryImpl.Action.ApproveSession -> {
                        // Launch concurrently
                        scope.launch {
                            val error = runCatching { handleAction(action) }.exceptionOrNull()
                            if (error != null) {
                                Timber.e(error, "ApproveSession failed")
                                val message = error.getErrorMessage(context)
                                val emitted = session.toastEvents.tryEmit(message)
                                Timber.d("[id=$sessionId] ApproveSession failure toast emitted=$emitted, message=$message")
                                session.close()
                            }
                        }
                    }

                    else -> {
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
                        is InstallerSessionRepositoryImpl.Action.Install ->
                            if ((e as? InstallException)?.errorType == InstallErrorType.MISSING_INSTALL_PERMISSION) {
                                ProgressEntity.InstallWaitingUnknownSource
                            } else {
                                ProgressEntity.InstallFailed
                            }

                        is InstallerSessionRepositoryImpl.Action.Analyse -> ProgressEntity.InstallAnalysedFailed
                        is InstallerSessionRepositoryImpl.Action.Uninstall -> ProgressEntity.UninstallFailed
                        is InstallerSessionRepositoryImpl.Action.ResolveUnarchive,
                        is InstallerSessionRepositoryImpl.Action.StartUnarchive,
                        is InstallerSessionRepositoryImpl.Action.ResolveUnarchiveError,
                        is InstallerSessionRepositoryImpl.Action.OpenUnarchiveErrorAction -> ProgressEntity.UnarchiveFailed

                        else -> ProgressEntity.InstallResolvedFailed
                    }

                    val currentState = session.progress.first()
                    // Avoid overwriting a Finish state or existing error loop
                    if (currentState != errorState &&
                        (errorState is ProgressEntity.InstallWaitingUnknownSource ||
                                currentState !is ProgressEntity.InstallFailed)
                    ) {
                        Timber.d("[id=$sessionId] Emitting error state: $errorState")
                        session.progress.emit(errorState)
                    }
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=$sessionId] onFinish: Cleaning up resources and cancelling job.")
        clearActionReplayCache()
        clearCache()
        processingJob?.cancel()
        job?.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun clearActionReplayCache() {
        session.action.resetReplayCache()
        Timber.d("[id=$sessionId] Cleared action replay cache on finish")
    }

    private suspend fun handleAction(action: InstallerSessionRepositoryImpl.Action) {
        // Check for cancellation before starting
        if (!currentCoroutineContext().isActive) return

        when (action) {
            is InstallerSessionRepositoryImpl.Action.ResolveInstall -> resolve(action.activity)
            is InstallerSessionRepositoryImpl.Action.Analyse -> analyse()
            is InstallerSessionRepositoryImpl.Action.Install -> handleSingleInstall(action.triggerAuth)
            is InstallerSessionRepositoryImpl.Action.InstallMultiple -> handleMultiInstall(action.triggerAuth)
            is InstallerSessionRepositoryImpl.Action.ResolveUninstall -> resolveUninstall(action.activity, action.packageName)
            is InstallerSessionRepositoryImpl.Action.Uninstall -> uninstall(action.packageName)
            is InstallerSessionRepositoryImpl.Action.ResolveConfirmInstall -> resolveConfirm(
                action.activity,
                action.sessionId,
                action.requestType
            )

            is InstallerSessionRepositoryImpl.Action.ResolveUnarchive -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    resolveUnarchive(
                        action.activity,
                        action.packageName,
                        action.intentSender
                    )
                } else unsupportedUnarchive()
            }

            is InstallerSessionRepositoryImpl.Action.StartUnarchive -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    startUnarchive()
                } else unsupportedUnarchive()
            }

            is InstallerSessionRepositoryImpl.Action.ResolveUnarchiveError -> resolveUnarchiveError(action.info)
            is InstallerSessionRepositoryImpl.Action.OpenUnarchiveErrorAction -> openUnarchiveErrorAction()
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
        val resolveResult = sourceResolver.resolve(activity.intent)

        // Check active after IO
        if (!currentCoroutineContext().isActive) throw CancellationException()

        // Store both stringified URIs and parsed data into the session
        session.sourceUris = resolveResult.uris
        session.referrerUri = activity.referrer?.toString()
        session.data = resolveResult.data

        Timber.d("[id=$sessionId] resolve: Data resolved successfully (${session.data.size} items).")

        // Post-Resolution Logic
        val forceDialog = session.data.size > 1 || session.data.any { it.sourcePath()?.endsWith(".zip", true) == true }
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

        val checkAppSignature = appSettingsRepo.getBoolean(BooleanSetting.CheckAppSignature, true).first()
        Timber.d("[id=$sessionId] App signature checks enabled: $checkAppSignature")

        val extra = AnalyseExtraEntity(
            cacheDirectory = cacheDirectory,
            isModuleFlashEnabled = isModuleEnabled,
            checkAppSignature = checkAppSignature
        )

        val results = analyzePackage(
            sessionId = session.id,
            config = session.config,
            data = session.data,
            extra = extra
        )

        if (results.isEmpty()) {
            throw AnalyseException(
                errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
                message = "No valid installation entities found in the provided sources."
            )
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
        val requireBiometricAuth = if (isInstall) {
            val globalMode = appSettingsRepo.getString(
                StringSetting.InstallerBiometricAuthMode,
                BiometricAuthMode.FollowConfig.value
            ).first().let { BiometricAuthMode.fromValueOrDefault(it) }

            when (globalMode) {
                BiometricAuthMode.Disable -> false
                BiometricAuthMode.Enable -> true
                BiometricAuthMode.FollowConfig -> session.config.requireBiometricAuth
            }
        } else {
            appSettingsRepo.getBoolean(BooleanSetting.UninstallerRequireBiometricAuth, false).first()
        }

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

    private suspend fun handleMultiInstall(triggerAuth: Boolean) {
        if (triggerAuth) {
            requestUserBiometricAuthentication(true)
        }
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

                    runWithSelfUpdateRecovery(tempResults) {
                        // Perform install
                        processInstallation(
                            config = session.config,
                            analysisResults = tempResults,
                            metadata = installMetadata(),
                            current = currentProgressIndex,
                            total = totalCount
                        ).collect { progress ->
                            if (progress is ProgressEntity.InstallingModule) {
                                session.moduleLog = progress.output
                            }
                            session.progress.emit(progress)
                        }
                    }

                    appEntities.forEach { entity ->
                        session.multiInstallResults.add(InstallResult(entity, true))
                    }
                } else {
                    throw IllegalStateException("Original package info not found")
                }
            } catch (e: Exception) {
                if ((e as? InstallException)?.errorType == InstallErrorType.MISSING_INSTALL_PERMISSION) {
                    Timber.w(e, "Batch install is waiting for unknown source permission.")
                    session.error = e
                    session.multiInstallResults.clear()
                    session.currentMultiInstallIndex = 0
                    session.progress.emit(ProgressEntity.InstallWaitingUnknownSource)
                    return
                }
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

        runWithSelfUpdateRecovery(session.analysisResults) {
            processInstallation(
                config = session.config,
                analysisResults = session.analysisResults,
                metadata = installMetadata()
            ).collect { progress ->
                // Sync module logs back to the session repository if applicable
                if (progress is ProgressEntity.InstallingModule) {
                    session.moduleLog = progress.output
                }
                // Emit progress to the UI layer
                session.progress.emit(progress)
            }
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

    private suspend fun runWithSelfUpdateRecovery(
        analysisResults: List<PackageAnalysisResult>,
        install: suspend () -> Unit
    ) {
        val selfUpdate = analysisResults.firstOrNull { result ->
            result.packageName == context.packageName && result.appEntities.any { it.selected }
        }
        // Android 17 may kill this process before a successful self-update call returns.
        // Persist the expected package state immediately before handing control to PackageManager.
        val recoveryArmed = selfUpdate != null && selfUpdateRecoveryManager.arm(sessionId)

        try {
            install()
        } catch (error: Throwable) {
            if (recoveryArmed) {
                withContext(NonCancellable) {
                    selfUpdateRecoveryManager.clear(sessionId)
                }
            }
            throw error
        }
    }

    private suspend fun resolveConfirm(
        activity: Activity,
        sysSessionId: Int,
        requestType: ConfirmationRequestType
    ) {
        Timber.d("[id=$sessionId] resolveConfirmInstall: Starting for system session $sysSessionId, type=$requestType.")

        // 1. Capture the exact Installing state before we override it
        val previousState = session.progress.replayCache.firstOrNull()
        val installingState = previousState as? ProgressEntity.Installing
        val isSelfSession = installingState != null

        // Extract the progress, defaulting to 1 if it wasn't an Installing state
        val currentProgress = installingState?.current ?: 1
        val totalProgress = installingState?.total ?: 1

        if (!isSelfSession) {
            session.config = configResolver.resolve(activity)
        }

        // Pass the captured progress into the UseCase
        val details = getSessionConfirmationDetails(
            sessionId = sysSessionId,
            config = session.config,
            requestType = requestType,
            isSelfSession = isSelfSession,
            currentProgress = currentProgress,
            totalProgress = totalProgress
        )

        val externalInstallerPackageName = details.installerPackageName.takeIf { !isSelfSession }
        if (externalInstallerPackageName != null) {
            session.config = configResolver.resolveForPackage(externalInstallerPackageName)
        }
        val hasResolvedSessionDetails = details.packageName.isNotBlank() || details.installerPackageName != null

        if (requestType == ConfirmationRequestType.PRE_APPROVAL && hasResolvedSessionDetails && !details.isPreApprovalRequested) {
            Timber.w("[id=$sessionId] resolveConfirmInstall: Session $sysSessionId is not requesting pre-approval. Rejecting.")
            approveSession(
                sessionId = sysSessionId,
                granted = false,
                config = session.config,
                details = details
            )
            session.close()
            return
        }

        session.confirmationDetails.value = details

        val canAutoApproveSession =
            session.config.autoApproveSession &&
                    !appSettingsRepo.getBoolean(BooleanSetting.LabRespectPlatformInstallPolicy, false).first()

        if (canAutoApproveSession) {
            Timber.d("[id=$sessionId] resolveConfirmInstall: Auto approving system session $sysSessionId.")
            val error = runCatching { handleConfirm(sysSessionId, true) }.exceptionOrNull()
            if (error != null) {
                Timber.e(error, "[id=$sessionId] Auto approve session failed")
                val message = error.getErrorMessage(context)
                val emitted = session.toastEvents.tryEmit(message)
                Timber.d("[id=$sessionId] Auto approve failure toast emitted=$emitted, message=$message")
                session.close()
            }
            return
        }

        Timber.d("[id=$sessionId] resolveConfirmInstall: Success. Emitting InstallConfirming.")
        session.progress.emit(ProgressEntity.InstallConfirming)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun resolveUnarchive(
        activity: Activity,
        packageName: String,
        intentSender: IntentSender
    ) {
        Timber.d("[id=$sessionId] resolveUnarchive: Resolving $packageName.")
        session.progress.emit(ProgressEntity.UnarchiveResolving)

        session.unarchiveInfo.value = unarchiveResolver.resolve(
            activity = activity,
            sessionId = sessionId,
            packageName = packageName,
            intentSender = intentSender
        )
        session.progress.emit(ProgressEntity.UnarchiveReady)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun startUnarchive() {
        val info = session.unarchiveInfo.value ?: error("Unarchive info is not resolved")
        Timber.d("[id=$sessionId] startUnarchive: Requesting unarchive for ${info.packageName}.")

        session.progress.emit(ProgressEntity.Unarchiving)
        unarchiveResolver.start(info)
        session.progress.emit(ProgressEntity.Finish)
    }

    private suspend fun unsupportedUnarchive() {
        session.error = IllegalStateException("Archived app restore requires Android 15 or later")
        session.progress.emit(ProgressEntity.UnarchiveFailed)
    }

    private suspend fun resolveUnarchiveError(info: UnarchiveErrorInfo) {
        Timber.d("[id=$sessionId] resolveUnarchiveError: status=${info.status}.")
        session.unarchiveErrorInfo.value = info
        session.progress.emit(ProgressEntity.UnarchiveErrorReady)
    }

    private fun openUnarchiveErrorAction() {
        val info = session.unarchiveErrorInfo.value ?: return
        unarchiveResolver.openErrorAction(info)
    }

    private suspend fun resolveUninstall(activity: Activity, packageName: String) {
        Timber.d("[id=$sessionId] resolveUninstall: Starting for $packageName.")
        session.progress.emit(ProgressEntity.UninstallResolving)

        val result = uninstallResolver.resolve(
            activity = activity,
            sessionId = sessionId,
            packageName = packageName
        )

        session.config = result.config
        session.uninstallInfo.update {
            result.uninstallInfo
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
        val detailsBeforeApprove = session.confirmationDetails.value
        Timber.d("[id=${this.sessionId}] ApproveSession: $granted for session $sessionId")
        approveSession(
            sessionId = sessionId,
            granted = granted,
            config = session.config,
            details = detailsBeforeApprove
        )

        val details = session.confirmationDetails.value
        if (granted && session.config.toastMode != ToastMode.Disable) {
            val sourceLabel = details?.sourceAppLabel
                ?: session.config.initiatorPackageName
                ?: context.getString(R.string.installer_label_unknown)
            val message = context.getString(R.string.install_confirm_approved_toast, sourceLabel)
            val emitted = session.toastEvents.tryEmit(message)
            Timber.d("[id=${this.sessionId}] Approve success toast emitted=$emitted")
        }

        val isSelfSession = details?.isSelfSession == true

        if (!isSelfSession) {
            // For external apps, approving/denying the session is the end of our job.
            session.close()
        } else {
            // For our own installations, we need to wait for the system callback.
            if (granted) {
                // Restore the Installing UI while we wait for LocalIntentReceiver.
                val current = details.currentProgress
                val total = details.totalProgress
                val label = details.appLabel.toString()

                session.progress.emit(ProgressEntity.Installing(current, total, label))
            } else {
                // DO NOT emit InstallFailed manually here!
                // The system will abort the session and send an INSTALL_FAILED_ABORTED intent
                // to our LocalIntentReceiver. The suspended ProcessInstallationUseCase will catch it,
                // throw the correct InstallException, update session.error, and naturally emit InstallFailed.
                Timber.d("[id=$sessionId] Self-session rejected. Waiting for system abort callback to trigger InstallException.")
            }
        }
    }

    private suspend fun handleReboot(reason: String) {
        Timber.d("[id=$sessionId] handleReboot: Starting cleanup before reboot.")
        val systemUseRoot =
            deviceCapabilityProvider.isSystemApp && appSettingsRepo.getBoolean(BooleanSetting.AlwaysUseRootInSystem, false).first()
        if (systemUseRoot) session.config = session.config.copy(authorizer = Authorizer.Root)
        // Execute cleanup immediately
        // Call clearCache() explicitly to ensure temporary files are removed
        // before the system goes down
        clearCache()

        Timber.d("[id=$sessionId] handleReboot: Cleanup finished. Executing reboot command.")

        // Execute the reboot command
        withContext(Dispatchers.IO) {
            val cmd = when (reason) {
                "ksud_soft_reboot" -> {
                    "ksud soft-reboot"
                }

                "recovery" -> {
                    // KEYCODE_POWER = 26. Hides incorrect "Factory data reset" message in recovery
                    "input keyevent 26 ; svc power reboot $reason || reboot $reason"
                }

                else -> {
                    val reasonArg = if (reason.isNotEmpty()) " $reason" else ""
                    "svc power reboot$reasonArg || reboot$reasonArg"
                }
            }

            val commandArray = arrayOf("sh", "-c", cmd)

            shellExecutionProvider.executeCommandArray(session.config, commandArray)
        }

        session.progress.emit(ProgressEntity.Finish)
    }

    private fun resetState() {
        session.error = Throwable()
        session.config = default
        session.sourceUris = emptyList()
        session.referrerUri = null
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

    private fun installMetadata(): InstallMetadata =
        InstallMetadata(
            sourceUris = session.sourceUris,
            referrerUri = session.referrerUri,
            operationSessionKey = session.id
        )

    private val InstallMode.isNotification get() = this == InstallMode.Notification || this == InstallMode.AutoNotification
}
