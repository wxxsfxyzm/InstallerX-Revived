package com.rosan.installer.data.app.model.impl.moduleInstaller

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallException
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.app.util.sourcePath
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * A module installer that executes commands DIRECTLY in the local process via `su`.
 * Best for Root/Customize modes to avoid Binder overhead.
 */
object LocalModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        rootImplementation: RootImplementation
    ): Flow<String> = callbackFlow {
        val modulePath = module.data.sourcePath()
            ?: throw ModuleInstallException("Could not resolve module file path for ${module.name}")

        // Determine the shell binary
        val shellBinary = if (config.authorizer == ConfigEntity.Authorizer.Customize) {
            config.customizeAuthorizer.ifBlank { "su" }
        } else {
            "su"
        }

        // Construct the command string
        val installCmd = when (rootImplementation) {
            RootImplementation.Magisk -> "magisk --install-module \"$modulePath\""
            RootImplementation.KernelSU -> "ksud module install \"$modulePath\""
            RootImplementation.APatch -> "apd module install \"$modulePath\""
        }

        val commandList = listOf(shellBinary, "-c", installCmd)
        Timber.d("Locally executing module install: $commandList")

        var process: Process? = null
        try {
            process = ProcessBuilder(commandList)
                .redirectErrorStream(true) // Merge stdout and stderr
                .start()

            // Read output stream
            val reader = process.inputStream.bufferedReader()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                trySend(line!!)
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Timber.i("Local installation completed successfully.")
                close()
            } else {
                Timber.e("Local installation failed with exit code: $exitCode")
                close(ModuleInstallExitCodeNonZeroException("Command failed with exit code $exitCode"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute local install command.")
            close(ModuleInstallCmdInitException("Failed to initiate command: ${e.message}", e))
        } finally {
            process?.destroy()
        }

        awaitClose {
            Timber.d("Local installation flow cancelled. Killing process.")
            process?.destroy()
        }
    }
}