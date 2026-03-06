// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.model.InstallExtraInfoEntity
import com.rosan.installer.domain.engine.repository.InstallerRepository
import com.rosan.installer.domain.settings.model.ConfigModel

/**
 * UseCase for executing the actual installation or uninstallation work.
 * It shields the session layer from the implementation details of different installation backends.
 */
class ExecuteInstallUseCase(
    private val installerRepository: InstallerRepository
) {
    /**
     * Executes the installation of the provided entities.
     * * @param config The configuration containing authorizer and install flags.
     * @param entities The list of APK/Module entities to be installed.
     * @param extra Metadata including userId and cache directory paths.
     * @param blacklist List of package names that are restricted.
     * @param sharedUserIdBlacklist List of SharedUserIDs that are restricted.
     * @param sharedUserIdExemption List of packages exempt from UID restrictions.
     */
    suspend fun install(
        config: ConfigModel,
        entities: List<InstallEntity>,
        extra: InstallExtraInfoEntity,
        blacklist: List<String> = emptyList(),
        sharedUserIdBlacklist: List<String> = emptyList(),
        sharedUserIdExemption: List<String> = emptyList()
    ) {
        installerRepository.doInstallWork(
            config = config,
            entities = entities,
            extra = extra,
            blacklist = blacklist,
            sharedUserIdBlacklist = sharedUserIdBlacklist,
            sharedUserIdExemption = sharedUserIdExemption
        )
    }

    /**
     * Executes the uninstallation of a package.
     */
    suspend fun uninstall(
        config: ConfigModel,
        packageName: String,
        extra: InstallExtraInfoEntity
    ) {
        installerRepository.doUninstallWork(config, packageName, extra)
    }

    /**
     * Approves an existing PackageInstaller session (primarily for Binder-based confirmers).
     */
    suspend fun approveSession(
        config: ConfigModel,
        sessionId: Int,
        granted: Boolean
    ) {
        installerRepository.approveSession(config, sessionId, granted)
    }
}

