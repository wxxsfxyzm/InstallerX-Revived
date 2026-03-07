package com.rosan.installer.data.engine.executor.moduleInstaller

import com.rosan.installer.data.engine.executor.ModuleInstallerUtils
import com.rosan.installer.data.privileged.util.SHELL_ROOT
import com.rosan.installer.data.privileged.util.SU_ARGS
import com.rosan.installer.domain.engine.exception.ModuleInstallCmdInitException
import com.rosan.installer.domain.engine.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.RootImplementation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * A module installer that executes commands DIRECTLY in the local process via `su`.
 */
class LocalModuleInstallerRepositoryImpl : ModuleInstallerRepository {
    override fun doInstallWork(
        config: ConfigModel,
        module: AppEntity.ModuleEntity,
        useRoot: Boolean,
        rootImplementation: RootImplementation
    ): Flow<String> = callbackFlow {
        // 1. Resolve Path using Helper
        val modulePath = ModuleInstallerUtils.getModulePathOrThrow(module)

        // 2. Determine Shell Binary Parts
        // If customizing, we still need to split user input (e.g. "su -c") to ensure ProcessBuilder works correctly.
        // If default, we directly concatenate the constants (SHELL_ROOT + SU_ARGS).
        val shellParts = if (config.authorizer == Authorizer.Customize && config.customizeAuthorizer.isNotBlank()) {
            config.customizeAuthorizer.trim().split("\\s+".toRegex())
        } else {
            listOf(SHELL_ROOT, SU_ARGS)
        }

        // 3. Construct Command String using Helper (Safe for Shell)
        val installCmd = ModuleInstallerUtils.buildShellCommandString(rootImplementation, modulePath)

        val commandList = shellParts + listOf("-c", installCmd)
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