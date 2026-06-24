// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import com.rosan.installer.data.engine.executor.moduleinstaller.LocalModuleInstallerRepoImpl
import com.rosan.installer.data.engine.executor.moduleinstaller.ShizukuModuleInstallerRepoImpl
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.ModuleInstallException
import com.rosan.installer.domain.engine.model.error.ModuleInstallErrorType
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.RootMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ModuleInstallerRepositoryImpl(
    private val deviceCapabilityProvider: DeviceCapabilityProvider
) : ModuleInstallerRepository {
    override fun doInstallWork(
        config: ConfigModel,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootMode: RootMode
    ): Flow<String> {
        // 1. Select the appropriate repository implementation
        val repo = when (config.authorizer) {
            Authorizer.Root,
            Authorizer.Customize -> LocalModuleInstallerRepoImpl()

            // Shizuku MUST use the Remote implementation
            Authorizer.Shizuku -> ShizukuModuleInstallerRepoImpl(deviceCapabilityProvider)

            Authorizer.None -> {
                if (deviceCapabilityProvider.isSystemApp && useRoot)
                    LocalModuleInstallerRepoImpl()
                else null // Signal that no session is available
            }

            else -> null
        }

        // 2. Handle unsupported authorizers immediately
        if (repo == null) {
            return flow {
                throw ModuleInstallException(
                    errorType = ModuleInstallErrorType.INCOMPATIBLE_AUTHORIZER,
                    message = "Module installation is not supported with the '${config.authorizer.name}' authorizer."
                )
            }
        }

        // 3. Execute with error handling
        return try {
            repo.doInstallWork(config, module, useRoot, rootMode)
        } catch (e: IllegalStateException) {
            // Catch immediate configuration errors
            if (repo is ShizukuModuleInstallerRepoImpl && e.message?.contains("binder") == true
            ) {
                flow {
                    throw PrivilegedException(
                        errorType = PrivilegedErrorType.SHIZUKU_NOT_WORK,
                        message = "Shizuku service connection lost.",
                        cause = e
                    )
                }
            } else {
                throw e
            }
        }
    }
}