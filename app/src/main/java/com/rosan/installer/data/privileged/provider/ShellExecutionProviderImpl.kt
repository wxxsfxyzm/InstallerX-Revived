// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.privileged.provider

import com.rosan.installer.data.privileged.util.SHELL_ROOT
import com.rosan.installer.data.privileged.util.SU_ARGS
import com.rosan.installer.data.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ShellExecutionProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ShellExecutionProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : ShellExecutionProvider {
    override suspend fun executeCommandArray(config: ConfigModel, command: Array<String>): String {
        return withContext(Dispatchers.IO) {
            if (config.authorizer == Authorizer.Root || config.authorizer == Authorizer.Customize) {
                return@withContext try {
                    val shellParts = if (config.authorizer == Authorizer.Customize && config.customizeAuthorizer.isNotBlank()) {
                        config.customizeAuthorizer.trim().split("\\s+".toRegex())
                    } else {
                        listOf(SHELL_ROOT, SU_ARGS)
                    }

                    val escapedCommand = command.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
                    val processCommand = shellParts + listOf("-c", escapedCommand)

                    val process = ProcessBuilder(processCommand).redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().use { it.readText() }
                    val exitCode = process.waitFor()

                    if (exitCode != 0) "Exit Code $exitCode: $output" else output
                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute local array command")
                    e.message ?: "Local execution failed"
                }
            }

            var result = ""
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                useHookMode = false
            ) {
                try {
                    result = it.privileged.execArr(command)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute array command via IPC")
                    result = e.message ?: "Execution failed"
                }
            }
            result
        }
    }
}
