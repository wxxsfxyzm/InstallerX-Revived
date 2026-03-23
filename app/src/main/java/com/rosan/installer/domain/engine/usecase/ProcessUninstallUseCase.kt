// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.settings.model.ConfigModel

/**
 * UseCase for executing the uninstallation of a package.
 */
class ProcessUninstallUseCase(
    private val appInstaller: AppInstallerRepository
) {
    /**
     * Executes the uninstallation work.
     */
    suspend operator fun invoke(
        config: ConfigModel,
        packageName: String
    ) {
        appInstaller.doUninstallWork(config, packageName)
    }
}
