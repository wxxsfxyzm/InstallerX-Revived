package com.rosan.installer.data.app.model.impl

import android.content.Context
import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.impl.installer.DhizukuInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.NoneInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.ProcessInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.ShizukuInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.installer.SystemInstallerRepoImpl
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object InstallerRepoImpl : InstallerRepo, KoinComponent {
    private val context by inject<Context>()

    private val isSystemInstaller: Boolean by lazy {
        context.packageName == "com.android.packageinstaller"
    }

    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val repo = when {
            isSystemInstaller -> SystemInstallerRepoImpl

            config.authorizer == ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            config.authorizer == ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            config.authorizer == ConfigEntity.Authorizer.None -> NoneInstallerRepoImpl
            else -> ProcessInstallerRepoImpl
        }
        repo.doInstallWork(config, entities, extra, blacklist, sharedUserIdBlacklist, sharedUserIdExemption)
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity,
    ) {
        val repo = when {
            isSystemInstaller -> SystemInstallerRepoImpl

            config.authorizer == ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            config.authorizer == ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            config.authorizer == ConfigEntity.Authorizer.None -> NoneInstallerRepoImpl
            else -> ProcessInstallerRepoImpl
        }
        repo.doUninstallWork(config, packageName, extra)
    }
}