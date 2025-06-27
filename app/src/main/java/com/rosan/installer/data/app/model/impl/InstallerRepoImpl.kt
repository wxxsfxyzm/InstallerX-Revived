package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.impl.installer.DhizukuInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.ProcessInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.ShizukuInstallerRepoImpl
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

object InstallerRepoImpl : InstallerRepo {
    override suspend fun doWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity
    ) {
        val repo = when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            else -> ProcessInstallerRepoImpl
        }
        repo.doWork(config, entities, extra)
    }
}