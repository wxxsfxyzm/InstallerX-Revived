package com.rosan.installer.data.app.model.impl.moduleInstaller

import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.app.model.exception.ModuleInstallCmdInitException
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.repo.ModuleInstallerRepo
import com.rosan.installer.data.app.util.ModuleInstallerUtils
import com.rosan.installer.data.recycle.util.SHELL_ROOT
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * A module installer that executes commands DIRECTLY in the local process via `su`.
 */
object LocalModuleInstallerRepoImpl : ModuleInstallerRepo {
    override fun doInstallWork(
        config: ConfigEntity,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> = callbackFlow {
        // 1. Resolve Path using Helper
        val modulePath = ModuleInstallerUtils.getModulePathOrThrow(module)

        // 2. Determine Shell Binary
        val shellBinary = if (config.authorizer == ConfigEntity.Authorizer.Customize) {
            config.customizeAuthorizer.ifBlank { SHELL_ROOT }
        } else {
            SHELL_ROOT
        }

        // 3. Construct Command String using Helper (Safe for Shell)
        val installCmd = ModuleInstallerUtils.buildShellCommandString(rootImplementation, modulePath)

        val commandList = listOf(shellBinary, "-c", installCmd)
        Timber.d("Locally executing module install: $commandList")

        var process: Process? = null
        try {
            process = ProcessBuilder(commandList)
                .redirectErrorStream(true)
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