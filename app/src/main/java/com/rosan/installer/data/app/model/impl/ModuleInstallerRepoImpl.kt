//
package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.data.app.model.impl.moduleInstaller.LocalModuleInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.moduleInstaller.ShizukuModuleInstallerRepoImpl
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.RootImplementation
import com.rosan.installer.util.OSUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object ModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigModel,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> {
        // 1. Select the appropriate repository implementation
        val repo = when (config.authorizer) {
            Authorizer.Root,
            Authorizer.Customize -> LocalModuleInstallerRepoImpl

            // Shizuku MUST use the Remote implementation
            Authorizer.Shizuku -> ShizukuModuleInstallerRepoImpl

            Authorizer.None -> {
                if (OSUtils.isSystemApp && useRoot)
                    LocalModuleInstallerRepoImpl
                else null // Signal that no repo is available
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
            if (repo is ShizukuModuleInstallerRepoImpl &&
                config.authorizer == Authorizer.Shizuku &&
                e.message?.contains("binder") == true
            ) {
                flow { throw ShizukuNotWorkException("Shizuku service connection lost.", e) }
            } else {
                throw e
            }
        }
    }
}