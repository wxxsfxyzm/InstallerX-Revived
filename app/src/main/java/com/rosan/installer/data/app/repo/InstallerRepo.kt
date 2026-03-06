package com.rosan.installer.data.app.repo

import com.rosan.installer.data.app.model.entity.InstallEntity
import com.rosan.installer.data.app.model.entity.InstallExtraInfoEntity
import com.rosan.installer.domain.settings.model.ConfigModel

interface InstallerRepo {
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