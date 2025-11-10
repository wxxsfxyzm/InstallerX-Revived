package com.rosan.installer.data.app.model.impl

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallFailedIncompatibleAuthorizerException
import com.rosan.installer.data.app.model.impl.installer.ProcessModuleInstallerRepoImpl
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object ModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        rootImplementation: RootImplementation
    ): Flow<String> { // Update return type
        val repo = when (config.authorizer) {
            ConfigEntity.Authorizer.Shizuku,
            ConfigEntity.Authorizer.Root,
            ConfigEntity.Authorizer.Customize -> ProcessModuleInstallerRepoImpl

            else -> {
                // Return a flow that immediately emits an error
                return flow {
                    throw ModuleInstallFailedIncompatibleAuthorizerException(
                        "Module installation is not supported with the '${config.authorizer.name}' authorizer."
                    )
                }
            }
        }

        return repo.doInstallWork(config, module, rootImplementation)
    }
}