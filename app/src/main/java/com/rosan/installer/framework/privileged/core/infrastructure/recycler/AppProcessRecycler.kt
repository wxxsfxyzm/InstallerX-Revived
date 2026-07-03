// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.infrastructure.recycler

import com.rosan.app_process.AppProcess
import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType
import com.rosan.installer.framework.privileged.core.infrastructure.lifecycle.Recycler
import com.rosan.installer.framework.privileged.core.infrastructure.process.AppProcessTerminal
import com.rosan.installer.framework.privileged.core.infrastructure.process.SHELL_COMMAND_PLACEHOLDER
import com.rosan.installer.framework.privileged.core.infrastructure.process.SHELL_ROOT
import com.rosan.installer.framework.privileged.core.infrastructure.process.SHELL_SYSTEM
import com.rosan.installer.framework.privileged.core.infrastructure.process.ShellCommand
import java.io.PrintWriter

class AppProcessRecycler(private val terminal: AppProcessTerminal) : Recycler<AppProcess>() {

    override val delayDuration: Long = 100L

    private class CustomizeAppProcess(private val shell: ShellCommand) : AppProcess.Default() {
        override fun newProcess(params: ProcessParams): Process {
            val command = params.cmdList.joinToString(" ") { it.shellQuote() }
            return if (shell.hasCommandPlaceholder()) {
                // Command-runner mode: `sh /path/to/rish -c {command}`.
                // Stdin terminal mode has no placeholder, for example: `su`, `su 1000`,
                // Or just `sh /path/to/rish`.
                val cmdList = shell.parts.map { part -> part.replace(SHELL_COMMAND_PLACEHOLDER, command) }
                super.newProcess(ProcessParams(params).setCmdList(cmdList))
            } else {
                val process = super.newProcess(ProcessParams(params).setCmdList(shell.parts))
                val printWriter = PrintWriter(process.outputStream, true)
                printWriter.println(command)
                printWriter.println("exit $?")
                process
            }
        }

        private fun ShellCommand.hasCommandPlaceholder(): Boolean {
            return parts.any { SHELL_COMMAND_PLACEHOLDER in it }
        }

        private fun String.shellQuote(): String {
            if (isEmpty()) return "''"
            if (all { it.isLetterOrDigit() || it in "-_./:=,@" }) return this
            return "'${replace("'", "'\\''")}'"
        }
    }

    override fun onMake(): AppProcess = newAppProcess().apply {
        if (init()) return@apply

        // Strictly check if the user intended to use standard root.
        // Avoid throwing RootNotWorkException if arguments like "su 2000" were passed.
        if (terminal == AppProcessTerminal.Root) {
            throw PrivilegedException(
                errorType = PrivilegedErrorType.ROOT_NOT_WORK,
                message = "Cannot access su command"
            )
        } else {
            // Throw the exact full command that failed initialization for accurate debugging
            throw PrivilegedException(
                errorType = PrivilegedErrorType.APP_PROCESS_NOT_WORK,
                message = "AppProcess init failed for shell: ${terminal.commandName()}"
            )
        }
    }

    private fun newAppProcess(): AppProcess = when (val current = terminal) {
        AppProcessTerminal.Root -> AppProcess.Root()
        AppProcessTerminal.RootSystem -> AppProcess.RootSystem()
        is AppProcessTerminal.Customize -> CustomizeAppProcess(current.command)
    }

    private fun AppProcessTerminal.commandName(): String = when (this) {
        AppProcessTerminal.Root -> SHELL_ROOT
        AppProcessTerminal.RootSystem -> SHELL_SYSTEM
        is AppProcessTerminal.Customize -> command.toString()
    }
}
