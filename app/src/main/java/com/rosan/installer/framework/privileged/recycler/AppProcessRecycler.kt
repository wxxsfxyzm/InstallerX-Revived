// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.recycler

import com.rosan.app_process.AppProcess
import com.rosan.installer.framework.privileged.lifecycle.Recycler
import com.rosan.installer.framework.privileged.util.SHELL_ROOT
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType

class AppProcessRecycler(private val shell: String) : Recycler<AppProcess>() {

    override val delayDuration: Long = 100L

    private class CustomizeAppProcess(private val shell: String) : AppProcess.Terminal() {
        override fun newTerminal(): MutableList<String> {
            // Split the shell command and its arguments properly to build the command list
            return shell.trim().split("\\s+".toRegex()).toMutableList()
        }
    }

    override fun onMake(): AppProcess {
        return CustomizeAppProcess(shell).apply {
            if (init()) return@apply

            val command = shell.trim().split("\\s+".toRegex()).firstOrNull()
            val fullCommand = shell.trim()

            // Strictly check if the user intended to use standard root.
            // Avoid throwing RootNotWorkException if arguments like "su 2000" were passed.
            if (command == SHELL_ROOT && fullCommand == SHELL_ROOT) {
                throw PrivilegedException(
                    errorType = PrivilegedErrorType.ROOT_NOT_WORK,
                    message = "Cannot access su command"
                )
            } else {
                // Throw the exact full command that failed initialization for accurate debugging
                throw PrivilegedException(
                    errorType = PrivilegedErrorType.APP_PROCESS_NOT_WORK,
                    message = "AppProcess init failed for shell: $fullCommand"
                )
            }
        }
    }
}