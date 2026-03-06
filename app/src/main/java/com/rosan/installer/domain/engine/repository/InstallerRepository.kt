package com.rosan.installer.domain.engine.repository

import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallExtraInfoEntity
import com.rosan.installer.domain.settings.model.ConfigModel

interface InstallerRepository {
    /**
     * Performs the installation of packages.
     * Renamed from doWork for clarity.
     */
    suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    )

    /**
     * Performs the uninstallation of a package.
     */
    suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String,
        extra: InstallExtraInfoEntity,
    )

    /**
     * Approve or deny a session.
     */
    suspend fun approveSession(
        config: ConfigModel,
        sessionId: Int,
        granted: Boolean
    )
}