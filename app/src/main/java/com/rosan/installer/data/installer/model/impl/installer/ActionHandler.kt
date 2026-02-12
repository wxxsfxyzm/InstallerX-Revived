package com.rosan.installer.data.installer.model.impl.installer

import android.app.Activity
import android.content.Context
import android.os.Build
import android.system.Os
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.enums.SessionMode
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.exception.AuthenticationFailedException
import com.rosan.installer.data.app.model.impl.AnalyserRepoImpl
import com.rosan.installer.data.app.util.IconColorExtractor
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.model.impl.installer.helper.ConfigResolver
import com.rosan.installer.data.installer.model.impl.installer.helper.SourceResolver
import com.rosan.installer.data.installer.model.impl.installer.processor.InstallationProcessor
import com.rosan.installer.data.installer.model.impl.installer.processor.SessionProcessor
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.model.impl.AutoLockManager
import com.rosan.installer.data.recycle.model.impl.PrivilegedManager
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.util.doBiometricAuthOrThrow
import com.rosan.installer.util.OSUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
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

class ActionHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    override val installer: InstallerRepoImpl = super.installer as InstallerRepoImpl
    private val mutableProgressFlow: MutableSharedFlow<ProgressEntity>
        get() = installer.progress
    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()
    private val iconColorExtractor by inject<IconColorExtractor>()

    private var job: Job? = null

    // A separate job for the current heavy task (Resolve, Install, etc.)
    private var processingJob: Job? = null

    // Helper property to get ID for logging
    private val installerId get() = installer.id

    // Cache directory
    private val cacheDirectory = File(context.cacheDir, "installer_sessions/$installerId")
        .apply { mkdirs() }
        .absolutePath

    // Initializing helpers without passing ID
    private val sourceResolver = SourceResolver(cacheDirectory, mutableProgressFlow)
    private val sessionProcessor = SessionProcessor()
    private val installationProcessor = InstallationProcessor(installer, mutableProgressFlow)

    override suspend fun onStart() {
        Timber.d("[id=$installerId] onStart: Starting to collect actions.")
        job = scope.launch {
            installer.action.collect { action ->
                Timber.d("[id=$installerId] Received action: ${action::class.simpleName}")

                // If the action is Cancel, we handle it immediately by cancelling the processing job.
                when (action) {
                    is InstallerRepoImpl.Action.Cancel -> {
                        handleCancel()
                    }

                    is InstallerRepoImpl.Action.Finish -> {
                        // Finish should also stop any ongoing work
                        processingJob?.cancel()
                        installer.progress.emit(ProgressEntity.Finish)
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
        Timber.d("[id=$installerId] handleCancel: Cancelling current processing job.")
        // 1. Cancel the current task
        processingJob?.cancel("User requested cancellation")
        processingJob = null

        // 2. Perform cleanup (same as onFinish/close)
        Timber.d("[id=$installerId] handleCancel: Cleaning up resources.")
        clearCache()

        // 3. Emit Finish instead of Ready. This effectively closes the session.
        Timber.d("[id=$installerId] handleCancel: Emitting ProgressEntity.Finish.")
        installer.progress.emit(ProgressEntity.Finish)
    }

    private fun startProcessingJob(action: InstallerRepoImpl.Action) {
        // Cancel previous job if exists
        processingJob?.cancel("New action received, cancelling old one")

        processingJob = scope.launch {
            runCatching {
                handleAction(action)
            }.onFailure { e ->
                if (e is CancellationException) {
                    Timber.d("[id=$installerId] Action ${action::class.simpleName} was cancelled.")
                    // Usually we don't need to emit error on cancellation, just stop.
                } else {
                    Timber.e(e, "[id=$installerId] Action ${action::class.simpleName} failed")
                    installer.error = e

                    val errorState = when (action) {
                        is InstallerRepoImpl.Action.Install -> ProgressEntity.InstallFailed
                        is InstallerRepoImpl.Action.Analyse -> ProgressEntity.InstallAnalysedFailed
                        is InstallerRepoImpl.Action.Uninstall -> ProgressEntity.UninstallFailed
                        else -> ProgressEntity.InstallResolvedFailed
                    }

                    val currentState = installer.progress.first()
                    // Avoid overwriting a Finish state or existing error loop
                    if (currentState != errorState && currentState !is ProgressEntity.InstallFailed) {
                        Timber.d("[id=$installerId] Emitting error state: $errorState")
                        installer.progress.emit(errorState)
                    }
                }
            }
        }
    }

    override suspend fun onFinish() {
        Timber.d("[id=$installerId] onFinish: Cleaning up resources and cancelling job.")
        clearCache()
        processingJob?.cancel()
        job?.cancel()
    }

    private suspend fun handleAction(action: InstallerRepoImpl.Action) {
        // Check for cancellation before starting
        if (!currentCoroutineContext().isActive) return

        when (action) {
            is InstallerRepoImpl.Action.ResolveInstall -> resolve(action.activity)
            is InstallerRepoImpl.Action.Analyse -> analyse()
            is InstallerRepoImpl.Action.Install -> handleSingleInstall(action.triggerAuth)
            is InstallerRepoImpl.Action.InstallMultiple -> handleMultiInstall()
            is InstallerRepoImpl.Action.ResolveUninstall -> resolveUninstall(action.activity, action.packageName)
            is InstallerRepoImpl.Action.Uninstall -> uninstall(action.packageName)
            is InstallerRepoImpl.Action.ResolveConfirmInstall -> resolveConfirm(action.activity, action.sessionId)
            is InstallerRepoImpl.Action.ApproveSession -> {
                Timber.d("[id=$installerId] ApproveSession: ${action.granted} for session ${action.sessionId}")
                sessionProcessor.approveSession(
                    action.sessionId,
                    action.granted,
                    installer.config
                )
                installer.progress.emit(ProgressEntity.Finish)
            }
            // Handle Reboot Action
            is InstallerRepoImpl.Action.Reboot -> handleReboot(action.reason)
            // Cancel and Finish are handled in the collector directly
            is InstallerRepoImpl.Action.Cancel,
            is InstallerRepoImpl.Action.Finish -> {
            }
        }
    }

    private suspend fun resolve(activity: Activity) {
        Timber.d("[id=$installerId] resolve: Starting new task.")
        resetState()
        Timber.d("[id=$installerId] resolve: State has been reset. Emitting ProgressEntity.InstallResolving.")
        installer.progress.emit(ProgressEntity.InstallResolving)

        // Resolve Config
        installer.config = ConfigResolver.resolve(activity)

        if (installer.config.installMode.isNotification) {
            Timber.d("[id=$installerId] Notification mode detected. Switching to background.")
            installer.background(true)
        }

        // Resolve Data (IO Heavy - Cancellable via SourceResolver)
        Timber.d("[id=$installerId] resolve: Resolving data URIs...")
        val data = sourceResolver.resolve(activity.intent)

        // Check active after IO
        if (!currentCoroutineContext().isActive) throw CancellationException()

        installer.data = data
        Timber.d("[id=$installerId] resolve: Data resolved successfully (${installer.data.size} items).")

        // Post-Resolution Logic
        val forceDialog = data.size > 1 || data.any { it.sourcePath()?.endsWith(".zip", true) == true }
        if (forceDialog) {
            Timber.d("[id=$installerId] resolve: Batch share or module file detected. Forcing install mode to Dialog.")
            installer.config.installMode = ConfigEntity.InstallMode.Dialog
        }

        Timber.d("[id=$installerId] resolve: Requesting AutoLockManager check.")
        AutoLockManager.onResolveInstall(installer.config.authorizer)

        if (installer.config.installMode.isNotification) {
            Timber.d("[id=$installerId] Notification mode detected early. Switching to background.")
            installer.background(true)
        }

        if (installer.config.installMode == ConfigEntity.InstallMode.Ignore) {
            Timber.d("[id=$installerId] resolve: InstallMode is Ignore. Finishing task.")
            installer.progress.emit(ProgressEntity.Finish)
            return
        }

        Timber.d("[id=$installerId] resolve: Emitting ProgressEntity.InstallResolveSuccess.")
        Timber.d("[id=$installerId] Final InstallMode before emitting success: ${installer.config.installMode}")
        installer.progress.emit(ProgressEntity.InstallResolveSuccess)
    }

    private suspend fun analyse() {
        Timber.d("[id=$installerId] analyse: Starting. Emitting ProgressEntity.InstallAnalysing.")
        installer.progress.emit(ProgressEntity.InstallAnalysing)

        val isModuleEnabled = appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false).first()
        Timber.d("[id=$installerId] Module flashing enabled: $isModuleEnabled")

        val extra = AnalyseExtraEntity(cacheDirectory, isModuleFlashEnabled = isModuleEnabled)

        // AnalyserRepoImpl.doWork needs to be responsive to cancellation internally or checked after
        val results = AnalyserRepoImpl.doWork(installer.config, installer.data, extra)

        if (!currentCoroutineContext().isActive) throw CancellationException()

        if (results.isEmpty()) {
            Timber.w("[id=$installerId] analyse: Analysis resulted in an empty list. No valid apps found.")
            throw AnalyseFailedAllFilesUnsupportedException("No valid files found")
        }

        // Handle Dynamic Colors
        val useDynamicColor = appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
        val useDynamicColorForLiveActivity =
            appDataStore.getBoolean(AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
        val enableDynamicColorAnalyse = useDynamicColor || useDynamicColorForLiveActivity

        val preferSystemIcon = appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false).first()

        installer.analysisResults = if (enableDynamicColorAnalyse) {
            Timber.d("[id=$installerId] analyse: Dynamic color is enabled. Extracting colors from icons.")
            coroutineScope {
                results.map { res ->
                    async {
                        // Check cancellation inside async block if needed
                        if (!isActive) throw CancellationException()
                        val base = res.appEntities.map { it.app }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                        val color = iconColorExtractor.extractColorFromApp(installer.id, res.packageName, base, preferSystemIcon)
                        res.copy(seedColor = color)
                    }
                }.awaitAll()
            }
        } else {
            Timber.d("[id=$installerId] analyse: Dynamic color is disabled. Skipping color extraction.")
            results
        }

        Timber.d("[id=$installerId] analyse: Emitting ProgressEntity.InstallAnalysedSuccess.")
        installer.progress.emit(ProgressEntity.InstallAnalysedSuccess)
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
            if (isInstall) appDataStore.getBoolean(AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH, false).first()
            else appDataStore.getBoolean(AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH, false).first()

        if (!requireBiometricAuth) return

        return context.doBiometricAuthOrThrow(
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
        installer.moduleLog = emptyList()
        performInstallLogic()
    }

    private suspend fun handleMultiInstall() {
        requestUserBiometricAuthentication(true)
        val queue = installer.multiInstallQueue
        if (queue.isEmpty()) return

        val groupedQueue: List<List<SelectInstallEntity>> = queue
            .groupBy { it.app.packageName }
            .values
            .toList()

        // Clear previous logs
        installer.moduleLog = emptyList()

        // Loop through the queue
        while (installer.currentMultiInstallIndex < groupedQueue.size) {
            if (!currentCoroutineContext().isActive) break

            val appEntities = groupedQueue[installer.currentMultiInstallIndex]
            val firstEntity = appEntities.first()

            val appLabel = (firstEntity.app as? AppEntity.BaseEntity)?.label
                ?: firstEntity.app.packageName

            val currentProgressIndex = installer.currentMultiInstallIndex + 1
            val totalCount = groupedQueue.size

            // Emit progress to UI
            installer.progress.emit(
                ProgressEntity.Installing(
                    current = currentProgressIndex,
                    total = totalCount,
                    appLabel = appLabel
                )
            )

            try {
                // Construct a temporary result list for the processor
                val originalResults = installer.analysisResults
                val targetResult = findResultForEntity(firstEntity, originalResults)

                if (targetResult != null) {
                    val entitiesToInstall = appEntities.map { it.copy(selected = true) }

                    val tempResults = listOf(targetResult.copy(appEntities = entitiesToInstall))

                    // Perform install.
                    installationProcessor.install(
                        config = installer.config,
                        analysisResults = tempResults,
                        cacheDirectory = cacheDirectory,
                        current = currentProgressIndex,
                        total = totalCount
                    )

                    appEntities.forEach { entity ->
                        installer.multiInstallResults.add(InstallResult(entity, true))
                    }
                } else {
                    throw IllegalStateException("Original package info not found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Batch install failed for ${firstEntity.app.packageName}")
                appEntities.forEach { entity ->
                    installer.multiInstallResults.add(InstallResult(entity, false, e))
                }
                // Continue to next app even if one fails
            }

            installer.currentMultiInstallIndex++
        }

        // Emit final completion state with results
        installer.progress.emit(ProgressEntity.InstallCompleted(installer.multiInstallResults.toList()))
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
        Timber.d("[id=$installerId] install: Starting installation process via InstallationProcessor.")
        installationProcessor.install(installer.config, installer.analysisResults, cacheDirectory)

        // Cache cleanup strategy
        val mode = installer.analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
        if (mode == SessionMode.Single) {
            Timber.d("[id=$installerId] Single-app install succeeded. Clearing cache now.")
            clearCache()
        } else {
            Timber.d("[id=$installerId] Multi-app install step succeeded. Deferring cache cleanup.")
        }
    }

    private suspend fun resolveConfirm(activity: Activity, sessionId: Int) {
        Timber.d("[id=$installerId] resolveConfirmInstall: Starting for session $sessionId.")
        installer.config = ConfigResolver.resolve(activity)

        val details = sessionProcessor.getSessionDetails(sessionId, installer.config)
        installer.confirmationDetails.value = details

        Timber.d("[id=$installerId] resolveConfirmInstall: Success. Emitting InstallConfirming.")
        installer.progress.emit(ProgressEntity.InstallConfirming)
    }

    private suspend fun resolveUninstall(activity: Activity, packageName: String) {
        Timber.d("[id=$installerId] resolveUninstall: Starting for $packageName.")
        installer.config = ConfigResolver.resolve(activity)
        installer.progress.emit(ProgressEntity.UninstallResolving)

        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val pInfo = pm.getPackageInfo(packageName, 0)
        val icon = pm.getApplicationIcon(appInfo)

        val color = if (appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()) {
            Timber.d("[id=$installerId] resolveUninstall: Dynamic color enabled, extracting color.")
            iconColorExtractor.extractColorFromDrawable(icon)
        } else null

        installer.uninstallInfo.update {
            UninstallInfo(
                packageName,
                pm.getApplicationLabel(appInfo).toString(),
                pInfo.versionName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong(),
                icon,
                color
            )
        }
        Timber.d("[id=$installerId] resolveUninstall: Success. Emitting UninstallReady.")
        installer.progress.emit(ProgressEntity.UninstallReady)
    }

    private suspend fun uninstall(packageName: String) {
        requestUserBiometricAuthentication(false)
        Timber.d("[id=$installerId] uninstall: Starting for $packageName. Emitting ProgressEntity.Uninstalling.")
        installer.progress.emit(ProgressEntity.Uninstalling)

        com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doUninstallWork(
            installer.config,
            packageName,
            InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory)
        )
        Timber.d("[id=$installerId] uninstall: Succeeded for $packageName. Emitting ProgressEntity.UninstallSuccess.")
        installer.progress.emit(ProgressEntity.UninstallSuccess)
    }

    private suspend fun handleReboot(reason: String) {
        Timber.d("[id=$installerId] handleReboot: Starting cleanup before reboot.")
        val systemUseRoot = OSUtils.isSystemApp && appDataStore.getBoolean(AppDataStore.LAB_MODULE_ALWAYS_ROOT, false).first()
        if (systemUseRoot) installer.config.authorizer = ConfigEntity.Authorizer.Root
        // Execute cleanup immediately
        // Call clearCache() explicitly to ensure temporary files are removed
        // before the system goes down
        clearCache()

        Timber.d("[id=$installerId] handleReboot: Cleanup finished. Executing reboot command.")

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

            PrivilegedManager.execArr(installer.config, commandArray)
        }

        installer.progress.emit(ProgressEntity.Finish)
    }

    private fun resetState() {
        installer.error = Throwable()
        installer.config = ConfigEntity.default
        installer.data = emptyList()
        installer.analysisResults = emptyList()
        installer.progress.tryEmit(ProgressEntity.Ready)
    }

    private fun clearCache() {
        Timber.d("[id=$installerId] clearCacheDirectory: Clearing cache...")
        sourceResolver.getTrackedCloseables().forEach { runCatching { it.close() } }
        File(cacheDirectory).runCatching {
            if (exists()) {
                val deleted = deleteRecursively()
                Timber.d("[id=$installerId] Cache directory deleted ($cacheDirectory): $deleted")
            } else {
                Timber.d("[id=$installerId] Cache directory not found, already cleared.")
            }
        }
    }

    private val ConfigEntity.InstallMode.isNotification get() = this == ConfigEntity.InstallMode.Notification || this == ConfigEntity.InstallMode.AutoNotification
}