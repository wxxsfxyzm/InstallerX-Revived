// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.domain.engine.repository

import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.settings.model.ConfigModel

/**
 * Interface for the installer repository.
 */
interface AppInstallerRepository {
    /**
     * Performs the installation of packages.
     *
     * @param config The configuration model.
     * @param entities The list of installation entities.
     * @param blacklist The list of blacklisted package names.
     * @param sharedUserIdBlacklist The list of blacklisted shared user IDs.
     * @param sharedUserIdExemption The list of package names for which shared user ID exemption is enabled.
     */
    suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>
    )

    /**
     * Performs the uninstallation of a package.
     *
     * @param config The configuration model.
     * @param packageName The name of the package to uninstall.
     */
    suspend fun doUninstallWork(
        config: ConfigModel,
        packageName: String,
    )

    /**
     * Approve or deny a session.
     *
     * @param config The configuration model.
     * @param sessionId The ID of the session to approve or deny.
     * @param granted Whether the session should be granted or denied.
     */
    suspend fun approveSession(
        config: ConfigModel,
        sessionId: Int,
        granted: Boolean
    )
}
