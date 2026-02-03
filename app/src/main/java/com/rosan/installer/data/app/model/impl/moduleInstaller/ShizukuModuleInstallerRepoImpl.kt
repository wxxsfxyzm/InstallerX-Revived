package com.rosan.installer.data.app.model.impl.moduleInstaller

import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.app.util.ModuleInstallerUtils
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * A module installer that works by executing shell commands via a REMOTE privileged process (Binder).
 */
object ShizukuModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> = callbackFlow {
        // 1. Resolve Path using Helper
        val modulePath = ModuleInstallerUtils.getModulePathOrThrow(module)

        // 2. Get Command Array using Helper (Best for IPC/exec)
        val command = ModuleInstallerUtils.getInstallCommandArgs(rootImplementation, modulePath)

        Timber.d("Executing remote module install command via IPC: ${command.joinToString(" ")}")

        val listener = object : ICommandOutputListener.Stub() {
            override fun onOutput(line: String) {
                trySend(line)
            }

            override fun onError(line: String) {
                trySend(line)
            }

            override fun onComplete(exitCode: Int) {
                if (exitCode == 0) {
                    close()
                } else {
                    close(ModuleInstallExitCodeNonZeroException("Remote command failed with exit code $exitCode"))
                }
            }
        }

        try {
            useUserService(
                authorizer = config.authorizer,
                useHookMode = false
            ) { userService ->
                userService.privileged.execArrWithCallback(command, listener)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate remote module installation.")
            close(ModuleInstallCmdInitException("Failed to initiate remote command: ${e.message}", e))
        }

        awaitClose {
            Timber.d("Remote module installation flow cancelled.")
        }
    }
}