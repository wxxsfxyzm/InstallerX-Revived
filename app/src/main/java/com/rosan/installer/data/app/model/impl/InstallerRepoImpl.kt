package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.impl.installer.DhizukuInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.ProcessInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.ShizukuInstallerRepoImpl
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

object InstallerRepoImpl : InstallerRepo {
    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
    ) {
        val repo = when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            else -> ProcessInstallerRepoImpl
        }
        repo.doInstallWork(config, entities, extra, blacklist)
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity,
    ) {
        val repo = when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            else -> ProcessInstallerRepoImpl
        }
        repo.doUninstallWork(config, packageName, extra)
    }
}