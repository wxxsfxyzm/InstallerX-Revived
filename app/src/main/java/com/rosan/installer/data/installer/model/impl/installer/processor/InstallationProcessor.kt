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
        if (selected.isEmpty()) throw IllegalStateException("No items selected")

        val firstApp = selected.first().app
        if (firstApp is AppEntity.ModuleEntity) {
            installModule(config, firstApp)
        } else {
            installApp(config, analysisResults, selected, cacheDirectory)
        }
    }

    private suspend fun installModule(config: ConfigEntity, module: AppEntity.ModuleEntity) {
        val output = mutableListOf("Starting installation...")
        progressFlow.emit(ProgressEntity.InstallingModule(output.toList()))

        val rootImpl = RootImplementation.fromString(appDataStore.getString(AppDataStore.LAB_ROOT_IMPLEMENTATION).first())

        ModuleInstallerRepoImpl.doInstallWork(config, module, rootImpl).collect {
            output.add(it)
            progressFlow.emit(ProgressEntity.InstallingModule(output.toList()))
        }
        progressFlow.emit(ProgressEntity.InstallSuccess)
    }

    private suspend fun installApp(
        config: ConfigEntity,
        allResults: List<PackageAnalysisResult>,
        selectedEntities: List<SelectInstallEntity>,
        cacheDirectory: String
    ) {
        progressFlow.emit(ProgressEntity.Installing)

        val blacklist =
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST).first().map { it.packageName }
        val sharedUidBlacklist =
            appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST).first().map { it.uidName }
        val sharedUidWhitelist =
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST).first()
                .map { it.packageName }

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

        val targetUserId = if (config.enableCustomizeUser) config.targetUserId else Os.getuid() / 100000

        CoreInstaller.doInstallWork(
            config,
            installEntities,
            InstallExtraInfoEntity(targetUserId, cacheDirectory),
            blacklist,
            sharedUidBlacklist,
            sharedUidWhitelist
        )
        progressFlow.emit(ProgressEntity.InstallSuccess)
    }
}