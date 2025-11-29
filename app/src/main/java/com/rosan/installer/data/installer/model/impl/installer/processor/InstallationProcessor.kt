package com.rosan.installer.data.installer.model.impl.installer.processor

import android.system.Os
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.data.app.model.impl.ModuleInstallerRepoImpl
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import com.rosan.installer.data.app.model.impl.InstallerRepoImpl as CoreInstaller

class InstallationProcessor(
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) : KoinComponent {
    private val appDataStore by inject<AppDataStore>()

    suspend fun install(
        config: ConfigEntity,
        analysisResults: List<PackageAnalysisResult>,
        cacheDirectory: String
    ) {
        val selected = analysisResults.flatMap { it.appEntities }.filter { it.selected }
        if (selected.isEmpty()) {
            Timber.w("install: No entities selected for installation.")
            throw IllegalStateException("No items selected")
        }

        val firstApp = selected.first().app
        if (firstApp is AppEntity.ModuleEntity) {
            installModule(config, firstApp)
        } else {
            installApp(config, selected, cacheDirectory)
        }
    }

    private suspend fun installModule(config: ConfigEntity, module: AppEntity.ModuleEntity) {
        Timber.d("installModule: Starting module installation for ${module.name}")
        val output = mutableListOf("Starting installation...")
        progressFlow.emit(ProgressEntity.InstallingModule(output.toList()))

        val rootImpl = RootImplementation.fromString(appDataStore.getString(AppDataStore.LAB_ROOT_IMPLEMENTATION).first())

        ModuleInstallerRepoImpl.doInstallWork(config, module, rootImpl).collect {
            output.add(it)
            progressFlow.emit(ProgressEntity.InstallingModule(output.toList()))
        }
        Timber.d("installModule: Succeeded. Emitting ProgressEntity.InstallSuccess.")
        progressFlow.emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun installApp(
        config: ConfigEntity,
        selectedEntities: List<SelectInstallEntity>,
        cacheDirectory: String
    ) {
        Timber.d("install: Starting. Emitting ProgressEntity.Installing.")
        progressFlow.emit(ProgressEntity.Installing)

        Timber.d("install: Loading package name blacklist from AppDataStore.")
        val blacklist = appDataStore.getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST)
            .first().map { it.packageName }

        Timber.d("install: Loading SharedUID blacklist from AppDataStore.")
        val sharedUidBlacklist = appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST)
            .first().map { it.uidName }

        Timber.d("install: Loading SharedUID whitelist from AppDataStore.")
        val sharedUidWhitelist = appDataStore.getNamedPackageList(AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST)
            .first().map { it.packageName }

        val installEntities = selectedEntities.map {
            InstallEntity(
                name = it.app.name,
                packageName = it.app.packageName,
                sharedUserId = (it.app as? AppEntity.BaseEntity)?.sharedUserId,
                arch = it.app.arch,
                data = it.app.data,
                sourceType = it.app.sourceType!!
            )
        }

        val targetUserId = if (config.enableCustomizeUser) {
            Timber.d("Custom user is enabled. Installing for user: ${config.targetUserId}")
            config.targetUserId
        } else {
            val uid = Os.getuid() / 100000
            Timber.d("Custom user is disabled. Installing for current user: $uid")
            uid
        }

        CoreInstaller.doInstallWork(
            config,
            installEntities,
            InstallExtraInfoEntity(targetUserId, cacheDirectory),
            blacklist,
            sharedUidBlacklist,
            sharedUidWhitelist
        )
        Timber.d("install: Succeeded. Emitting ProgressEntity.InstallSuccess.")
        progressFlow.emit(ProgressEntity.InstallSuccess)
    }
}