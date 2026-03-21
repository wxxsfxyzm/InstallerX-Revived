// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import com.rosan.installer.data.engine.executor.moduleInstaller.LocalModuleInstallerRepositoryImpl
import com.rosan.installer.data.engine.executor.moduleInstaller.ShizukuModuleInstallerRepositoryImpl
import com.rosan.installer.data.privileged.exception.ShizukuNotWorkException
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.RootImplementation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ModuleInstallerRepositoryImpl(
    private val deviceCapabilityProvider: DeviceCapabilityProvider
) : ModuleInstallerRepository {
    override fun doInstallWork(
        config: ConfigModel,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> {
        // 1. Select the appropriate repository implementation
        val repo = when (config.authorizer) {
            Authorizer.Root,
            Authorizer.Customize -> LocalModuleInstallerRepositoryImpl()

            // Shizuku MUST use the Remote implementation
            Authorizer.Shizuku -> ShizukuModuleInstallerRepositoryImpl(deviceCapabilityProvider)

            Authorizer.None -> {
                if (deviceCapabilityProvider.isSystemApp && useRoot)
                    LocalModuleInstallerRepositoryImpl()
                else null // Signal that no session is available
            }

            else -> null
        }

        // 2. Handle unsupported authorizers immediately
        if (repo == null) {
            return flow {
                throw ModuleInstallFailedIncompatibleAuthorizerException(
                    "Module installation is not supported with the '${config.authorizer.name}' authorizer."
                )
            }
        }

        // 3. Execute with error handling
        return try {
            repo.doInstallWork(config, module, useRoot, rootImplementation)
        } catch (e: IllegalStateException) {
            // Catch immediate configuration errors
            if (repo is ShizukuModuleInstallerRepositoryImpl && e.message?.contains("binder") == true
            ) {
                flow { throw ShizukuNotWorkException("Shizuku service connection lost.", e) }
            } else {
                throw e
            }
        }
    }
}