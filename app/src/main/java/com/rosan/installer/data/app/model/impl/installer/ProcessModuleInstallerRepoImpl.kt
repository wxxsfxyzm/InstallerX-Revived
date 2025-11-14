package com.rosan.installer.data.app.model.impl.installer

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
 * A module installer that works by executing shell commands via a privileged process (e.g., Root).
 */
object ProcessModuleInstallerRepoImpl : ModuleInstallerRepo {
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

        Timber.d("Executing module install command: ${command.joinToString(" ")}")

        val listener = object : ICommandOutputListener.Stub() {
            override fun onOutput(line: String) {
                trySend(line)
            }

            override fun onError(line: String) {
                trySend(line)
            }

            override fun onComplete(exitCode: Int) {
                if (exitCode == 0) {
                    Timber.i("Module installation command completed successfully.")
                    close() // Close the flow successfully
                } else {
                    Timber.e("Module installation command failed with exit code: $exitCode")
                    close(ModuleInstallExitCodeNonZeroException("Command failed with exit code $exitCode"))
                }
            }
        }

        try {
            useUserService(config) { userService ->
                userService.privileged.execArrWithCallback(command, listener)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate module installation.")
            close(ModuleInstallCmdInitException("Failed to initiate command: ${e.message}", e))
        }

        // This is called when the Flow is cancelled (e.g., user navigates away)
        awaitClose {
            Timber.d("Module installation flow was cancelled by the collector.")
            // We can't easily kill the remote root process here.
            // The process will continue to run in the background.
        }
    }
}