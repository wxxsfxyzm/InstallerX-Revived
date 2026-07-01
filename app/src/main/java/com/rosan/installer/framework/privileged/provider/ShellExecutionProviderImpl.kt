// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import com.rosan.installer.framework.privileged.util.SHELL_ROOT
import com.rosan.installer.framework.privileged.util.SU_ARGS
import com.rosan.installer.framework.privileged.util.requireCustomizeAuthorizer
import com.rosan.installer.framework.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ShellExecutionProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ShellExecutionProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : ShellExecutionProvider {
    override suspend fun executeCommandArray(config: ConfigModel, command: Array<String>): String {
        return withContext(Dispatchers.IO) {
            if (config.authorizer == Authorizer.Root || config.authorizer == Authorizer.Customize) {
                val shellParts = if (config.authorizer == Authorizer.Customize) {
                    requireCustomizeAuthorizer(config.customizeAuthorizer).trim().split("\\s+".toRegex())
                } else {
                    listOf(SHELL_ROOT, SU_ARGS)
                }

                return@withContext try {
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
                customizeAuthorizer = config.customizeAuthorizer
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
