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

    // Components
    private val cacheDirectory = "${context.externalCacheDir?.absolutePath}/${installer.id}".apply { File(this).mkdirs() }
    private val sourceResolver = SourceResolver(cacheDirectory, mutableProgressFlow)
    private val sessionProcessor = SessionProcessor()
    private val installationProcessor = InstallationProcessor(mutableProgressFlow)

    override suspend fun onStart() {
        job = scope.launch {
            installer.action.collect { action ->
                runCatching { handleAction(action) }.onFailure { e ->
                    Timber.e(e, "Action ${action::class.simpleName} failed")
                    installer.error = e

                    val errorState = when (action) {
                        is InstallerRepoImpl.Action.Install -> ProgressEntity.InstallFailed
                        is InstallerRepoImpl.Action.Analyse -> ProgressEntity.InstallAnalysedFailed
                        is InstallerRepoImpl.Action.Uninstall -> ProgressEntity.UninstallFailed
                        else -> ProgressEntity.InstallResolvedFailed
                    }

                    val currentState = installer.progress.first()
                    if (currentState != errorState && currentState !is ProgressEntity.InstallFailed) {
                        installer.progress.emit(errorState)
                    }
                }
            }
        }
    }

    override suspend fun onFinish() {
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
            is InstallerRepoImpl.Action.ApproveSession -> sessionProcessor.approveSession(
                action.sessionId,
                action.granted,
                installer.config
            )

            is InstallerRepoImpl.Action.Finish -> installer.progress.emit(ProgressEntity.Finish)
        }
    }

    private suspend fun resolve(activity: Activity) {
        resetState()
        installer.progress.emit(ProgressEntity.InstallResolving)

        // 1. Resolve Config
        installer.config = ConfigResolver.resolve(activity)

        // 2. Resolve Data (IO Heavy)
        val data = sourceResolver.resolve(activity.intent)
        installer.data = data

        // 3. Post-Resolution Logic
        val forceDialog = data.size > 1 || data.any { it.sourcePath()?.endsWith(".zip", true) == true }
        if (forceDialog) installer.config.installMode = ConfigEntity.InstallMode.Dialog

        autoLockInstallerIfNeeded()

        if (installer.config.installMode.isNotification) installer.background(true)
        if (installer.config.installMode == ConfigEntity.InstallMode.Ignore) {
            installer.progress.emit(ProgressEntity.Finish)
            return
        }

        installer.progress.emit(ProgressEntity.InstallResolveSuccess)
    }

    private suspend fun analyse() {
        installer.progress.emit(ProgressEntity.InstallAnalysing)

        val isModuleEnabled = appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false).first()
        val extra = AnalyseExtraEntity(cacheDirectory, isModuleFlashEnabled = isModuleEnabled)

        val results = AnalyserRepoImpl.doWork(installer.config, installer.data, extra)

        if (results.isEmpty()) throw AnalyseFailedAllFilesUnsupportedException("No valid files found")

        // Handle Dynamic Colors (moved inline logic to be cleaner or extract to AnalysisProcessor)
        val useDynamicColor = appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
        installer.analysisResults = if (useDynamicColor) {
            coroutineScope {
                results.map { res ->
                    async {
                        val base = res.appEntities.map { it.app }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                        val color = iconColorExtractor.extractColorFromApp(installer.id, res.packageName, base, false)
                        res.copy(seedColor = color)
                    }
                }.awaitAll()
            }
        } else results

        installer.progress.emit(ProgressEntity.InstallAnalysedSuccess)
    }

    private suspend fun install() {
        installationProcessor.install(installer.config, installer.analysisResults, cacheDirectory)

        // Cache cleanup strategy
        val mode = installer.analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single
        if (mode == SessionMode.Single) clearCache()
    }

    private suspend fun resolveConfirm(activity: Activity, sessionId: Int) {
        installer.config.authorizer = ConfigUtil.getGlobalAuthorizer()
        val details = sessionProcessor.getSessionDetails(sessionId, installer.config)
        installer.confirmationDetails.value = details
        installer.progress.emit(ProgressEntity.InstallAnalysedSuccess)
    }

    private suspend fun resolveUninstall(activity: Activity, packageName: String) {
        installer.config = ConfigResolver.resolve(activity)
        installer.progress.emit(ProgressEntity.UninstallResolving)

        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val pInfo = pm.getPackageInfo(packageName, 0)
        val icon = pm.getApplicationIcon(appInfo)

        val color = if (appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()) {
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
        installer.progress.emit(ProgressEntity.UninstallReady)
    }

    private suspend fun uninstall(packageName: String) {
        installer.progress.emit(ProgressEntity.Uninstalling)
        com.rosan.installer.data.app.model.impl.InstallerRepoImpl.doUninstallWork(
            installer.config,
            packageName,
            InstallExtraInfoEntity(Os.getuid() / 100000, cacheDirectory)
        )
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
        sourceResolver.getTrackedCloseables().forEach { runCatching { it.close() } }
        File(cacheDirectory).deleteRecursively()
    }

    private suspend fun autoLockInstallerIfNeeded() {
        if (appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER).first()) {
            runCatching { setInstallerDefaultPrivileged(context, installer.config, true) }
        }
    }

    private val ConfigEntity.InstallMode.isNotification get() = this == ConfigEntity.InstallMode.Notification || this == ConfigEntity.InstallMode.AutoNotification
}