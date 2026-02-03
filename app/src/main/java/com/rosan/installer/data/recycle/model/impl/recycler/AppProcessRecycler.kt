package com.rosan.installer.data.recycle.model.impl.recycler

import com.rosan.app_process.AppProcess
import com.rosan.installer.data.recycle.model.exception.AppProcessNotWorkException
import com.rosan.installer.data.recycle.model.exception.RootNotWorkException
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.util.SHELL_ROOT

class AppProcessRecycler(private val shell: String) : Recycler<AppProcess>() {

    override val delayDuration: Long = 100L

    private class CustomizeAppProcess(private val shell: String) : AppProcess.Terminal() {
        override fun newTerminal(): MutableList<String> {
            return shell.trim().split("\\s+".toRegex()).toMutableList()
        }
    }

    override fun onMake(): AppProcess {
        return CustomizeAppProcess(shell).apply {
            if (init()) return@apply
            val command = shell.trim().split("\\s+".toRegex()).firstOrNull()
            if (command == SHELL_ROOT) throw RootNotWorkException("Cannot access su command")
            else throw AppProcessNotWorkException("Cannot access command $command")
        }
    }
}