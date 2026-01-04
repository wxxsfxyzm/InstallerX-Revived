package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.data.app.model.impl.appInstaller.DhizukuInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.appInstaller.NoneInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.appInstaller.ProcessInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.appInstaller.ShizukuInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.appInstaller.SystemInstallerRepoImpl
import com.rosan.installer.data.app.repo.InstallerRepo
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.OSUtils
import org.koin.core.component.KoinComponent

object InstallerRepoImpl : InstallerRepo, KoinComponent {

    override suspend fun doInstallWork(
        config: ConfigEntity,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    ) {
        val repo = when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            ConfigEntity.Authorizer.None -> {
                if (OSUtils.isSystemApp) SystemInstallerRepoImpl
                else NoneInstallerRepoImpl
            }

            else -> ProcessInstallerRepoImpl
        }
        try {
            repo.doInstallWork(
                config,
                entities,
                extra,
                blacklist,
                sharedUserIdBlacklist,
                sharedUserIdExemption
            )
        } catch (e: IllegalStateException) {
            // Check if the exception is the specific one from Shizuku a runtime connection failure.
            if (repo is ShizukuInstallerRepoImpl && e.message?.contains("binder haven't been received") == true) {
                // If it is, wrap it in our custom ShizukuNotWorkException to provide better context.
                throw ShizukuNotWorkException("Shizuku service connection lost during operation.", e)
            }
            // Re-throw any other IllegalStateException that we are not specifically handling.
            throw e
        }
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity,
    ) {
        val repo = when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            ConfigEntity.Authorizer.None -> {
                if (OSUtils.isSystemApp) SystemInstallerRepoImpl
                else NoneInstallerRepoImpl
            }

            else -> ProcessInstallerRepoImpl
        }
        try {
            repo.doUninstallWork(config, packageName, extra)
        } catch (e: IllegalStateException) {
            if (repo is ShizukuInstallerRepoImpl && e.message?.contains("binder haven't been received") == true) {
                throw ShizukuNotWorkException("Shizuku service connection lost during operation.", e)
            }
            throw e
        }
    }
}