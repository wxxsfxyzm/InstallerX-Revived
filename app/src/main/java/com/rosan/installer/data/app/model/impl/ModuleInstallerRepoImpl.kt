//
package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.data.app.model.impl.moduleInstaller.LocalModuleInstallerRepoImpl
import com.rosan.installer.data.app.model.impl.moduleInstaller.ShizukuModuleInstallerRepoImpl
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.recycle.model.exception.ShizukuNotWorkException
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object ModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        rootImplementation: RootImplementation
    ): Flow<String> {
        val repo = when (config.authorizer) {
            // Use Local implementation for Root to avoid Binder overhead
            ConfigEntity.Authorizer.Root,
            ConfigEntity.Authorizer.Customize -> LocalModuleInstallerRepoImpl

            // Shizuku MUST use the Remote implementation because we don't have direct permission
            ConfigEntity.Authorizer.Shizuku -> ShizukuModuleInstallerRepoImpl

            else -> {
                return flow {
                    throw ModuleInstallFailedIncompatibleAuthorizerException(
                        "Module installation is not supported with the '${config.authorizer.name}' authorizer."
                    )
                }
            }
        }

        // Apply Shizuku-specific error handling wrapper if needed (similar to InstallerRepoImpl)
        return try {
            repo.doInstallWork(config, module, rootImplementation)
        } catch (e: IllegalStateException) {
            // Catch immediate configuration errors
            if (repo is ShizukuModuleInstallerRepoImpl && config.authorizer == ConfigEntity.Authorizer.Shizuku && e.message?.contains(
                    "binder"
                ) == true
            ) {
                flow { throw ShizukuNotWorkException("Shizuku service connection lost.", e) }
            } else {
                throw e
            }
        }
    }
}