package com.rosan.installer.data.installer.model.impl.installer

import android.app.Activity
import android.content.Context
import android.os.Build
import android.system.Os
import com.rosan.installer.data.app.model.entity.AnalyseExtraEntity
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.entity.SessionMode
import com.rosan.installer.data.app.model.exception.AnalyseFailedAllFilesUnsupportedException
import com.rosan.installer.data.app.model.impl.AnalyserRepoImpl
import com.rosan.installer.data.app.util.IconColorExtractor
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.model.impl.installer.helper.ConfigResolver
import com.rosan.installer.data.installer.model.impl.installer.helper.SourceResolver
import com.rosan.installer.data.installer.model.impl.installer.processor.InstallationProcessor
import com.rosan.installer.data.installer.model.impl.installer.processor.SessionProcessor
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.util.setInstallerDefaultPrivileged
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // Helper property to get ID for logging
    private val installerId get() = installer.id

    // Components
    private val cacheDirectory = "${context.externalCacheDir?.absolutePath}/$installerId".apply { File(this).mkdirs() }

    // Initializing helpers without passing ID
    private val sourceResolver = SourceResolver(cacheDirectory, mutableProgressFlow)
    private val sessionProcessor = SessionProcessor()
    private val installationProcessor = InstallationProcessor(mutableProgressFlow)

    override suspend fun onStart() {
        Timber.d("[id=$installerId] onStart: Starting to collect actions.")
        job = scope.launch {
            installer.action.collect { action ->
                Timber.d("[id=$installerId] Received action: ${action::class.simpleName}")
                runCatching { handleAction(action) }.onFailure { e ->
                    Timber.e(e, "[id=$installerId] Action ${action::class.simpleName} failed")
                    installer.error = e

                    val errorState = when (action) {
                        is InstallerRepoImpl.Action.Install -> ProgressEntity.InstallFailed
                        is InstallerRepoImpl.Action.Analyse -> ProgressEntity.InstallAnalysedFailed
                        is InstallerRepoImpl.Action.Uninstall -> ProgressEntity.UninstallFailed
                        else -> ProgressEntity.InstallResolvedFailed
                    }

                    val currentState = installer.progress.first()
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
        job?.cancel()
    }

    private suspend fun handleAction(action: InstallerRepoImpl.Action) {
        when (action) {
            is InstallerRepoImpl.Action.ResolveInstall -> resolve(action.activity)
            is InstallerRepoImpl.Action.Analyse -> analyse()
            is InstallerRepoImpl.Action.Install -> install()
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

            is InstallerRepoImpl.Action.Finish -> {
                Timber.d("[id=$installerId] finish: Emitting ProgressEntity.Finish.")
                installer.progress.emit(ProgressEntity.Finish)
            }
        }
    }

    private suspend fun resolve(activity: Activity) {
        Timber.d("[id=$installerId] resolve: Starting new task.")
        resetState()
        Timber.d("[id=$installerId] resolve: State has been reset. Emitting ProgressEntity.InstallResolving.")
        installer.progress.emit(ProgressEntity.InstallResolving)

        // 1. Resolve Config
        installer.config = ConfigResolver.resolve(activity)

        // 2. Resolve Data (IO Heavy)
        Timber.d("[id=$installerId] resolve: Resolving data URIs...")
        val data = sourceResolver.resolve(activity.intent)
        installer.data = data
        Timber.d("[id=$installerId] resolve: Data resolved successfully (${installer.data.size} items).")

        // 3. Post-Resolution Logic
        val forceDialog = data.size > 1 || data.any { it.sourcePath()?.endsWith(".zip", true) == true }
        if (forceDialog) {
            Timber.d("[id=$installerId] resolve: Batch share or module file detected. Forcing install mode to Dialog.")
            installer.config.installMode = ConfigEntity.InstallMode.Dialog
        }

        autoLockInstallerIfNeeded()

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
        installer.progress.emit(ProgressEntity.InstallResolveSuccess)
    }

    private suspend fun analyse() {
        Timber.d("[id=$installerId] analyse: Starting. Emitting ProgressEntity.InstallAnalysing.")
        installer.progress.emit(ProgressEntity.InstallAnalysing)

        val isModuleEnabled = appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false).first()
        Timber.d("[id=$installerId] Module flashing enabled: $isModuleEnabled")

        val extra = AnalyseExtraEntity(cacheDirectory, isModuleFlashEnabled = isModuleEnabled)

        val results = AnalyserRepoImpl.doWork(installer.config, installer.data, extra)

        if (results.isEmpty()) {
            Timber.w("[id=$installerId] analyse: Analysis resulted in an empty list. No valid apps found.")
            throw AnalyseFailedAllFilesUnsupportedException("No valid files found")
        }

        // Handle Dynamic Colors
        val useDynamicColor = appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()

        installer.analysisResults = if (useDynamicColor) {
            Timber.d("[id=$installerId] analyse: Dynamic color is enabled. Extracting colors from icons.")
            coroutineScope {
                results.map { res ->
                    async {
                        val base = res.appEntities.map { it.app }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                        val color = iconColorExtractor.extractColorFromApp(installer.id, res.packageName, base, false)
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

    private suspend fun install() {
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
        installer.config.authorizer = ConfigUtil.getGlobalAuthorizer()

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
                if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else pInfo.versionCode.toLong(),
                icon,
                color
            )
        }
        Timber.d("[id=$installerId] resolveUninstall: Success. Emitting UninstallReady.")
        installer.progress.emit(ProgressEntity.UninstallReady)
    }

    private suspend fun uninstall(packageName: String) {
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

    private suspend fun autoLockInstallerIfNeeded() {
        if (appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER).first()) {
            Timber.d("[id=$installerId] resolve: Attempting to auto-lock default installer.")
            runCatching {
                setInstallerDefaultPrivileged(context, installer.config, true)
                Timber.d("[id=$installerId] resolve: Auto-lock attempt finished successfully.")
            }.onFailure {
                Timber.w(it, "[id=$installerId] resolve: Failed to auto-lock default installer. This is non-fatal.")
            }
        }
    }

    private val ConfigEntity.InstallMode.isNotification get() = this == ConfigEntity.InstallMode.Notification || this == ConfigEntity.InstallMode.AutoNotification
}