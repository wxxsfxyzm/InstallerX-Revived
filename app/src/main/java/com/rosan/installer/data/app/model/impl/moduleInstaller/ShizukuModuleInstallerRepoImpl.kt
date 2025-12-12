package com.rosan.installer.data.app.model.impl.moduleInstaller

import com.rosan.installer.ICommandOutputListener
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallException
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * A module installer that works by executing shell commands via a REMOTE privileged process (Binder).
 * Required for Shizuku mode where the main process has no direct shell access.
 */
object ShizukuModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        rootImplementation: RootImplementation
    ): Flow<String> = callbackFlow {
        val modulePath = module.data.sourcePath()
            ?: throw ModuleInstallException("Could not resolve module file path for ${module.name}")

        val command = when (rootImplementation) {
            RootImplementation.Magisk -> arrayOf("magisk", "--install-module", modulePath)
            RootImplementation.KernelSU -> arrayOf("ksud", "module", "install", modulePath)
            RootImplementation.APatch -> arrayOf("apd", "module", "install", modulePath)
        }

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
            // This goes through the AIDL/Binder service
            useUserService(
                authorizer = config.authorizer,
                useShizukuHookMode = false
            ) { userService ->
                userService.privileged.execArrWithCallback(command, listener)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate remote module installation.")
            close(ModuleInstallCmdInitException("Failed to initiate remote command: ${e.message}", e))
        }

        awaitClose {
            Timber.d("Remote module installation flow cancelled.")
            // Ideally, we would notify the remote service to cancel, but the binder death recipient handles cleanup mostly.
        }
    }
}