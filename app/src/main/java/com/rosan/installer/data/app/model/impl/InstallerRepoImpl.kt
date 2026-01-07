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
    ) = executeWithRepo(config) { repo ->
        repo.doInstallWork(
            config,
            entities,
            extra,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption
        )
    }

    override suspend fun doUninstallWork(
        config: ConfigEntity,
        packageName: String,
        extra: InstallExtraInfoEntity,
    ) = executeWithRepo(config) { repo ->
        repo.doUninstallWork(config, packageName, extra)
    }

    override suspend fun approveSession(
        config: ConfigEntity,
        sessionId: Int,
        granted: Boolean
    ) = executeWithRepo(config) { repo ->
        repo.approveSession(config, sessionId, granted)
    }

    /**
     * Execute an action with the InstallerRepo based on the provided ConfigEntity.
     */
    private suspend fun <T> executeWithRepo(
        config: ConfigEntity,
        action: suspend (InstallerRepo) -> T
    ): T {
        val repo = resolveRepo(config)

        try {
            return action(repo)
        } catch (e: IllegalStateException) {
            // Check if Shizuku service connection is lost
            if (repo is ShizukuInstallerRepoImpl && e.message?.contains("binder haven't been received") == true) {
                throw ShizukuNotWorkException("Shizuku service connection lost during operation.", e)
            }
            // Throw other exceptions as-is
            throw e
        }
    }

    /**
     * Resolve the InstallerRepo based on the provided ConfigEntity.
     */
    private fun resolveRepo(config: ConfigEntity): InstallerRepo {
        return when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku -> ShizukuInstallerRepoImpl
            ConfigEntity.Authorizer.Dhizuku -> DhizukuInstallerRepoImpl
            ConfigEntity.Authorizer.None -> {
                if (OSUtils.isSystemApp) SystemInstallerRepoImpl
                else NoneInstallerRepoImpl
            }

            else -> ProcessInstallerRepoImpl
        }
    }
}