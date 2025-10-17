package com.rosan.installer.data.recycle.model.impl

import com.rosan.app_process.AppProcess
import com.rosan.installer.data.recycle.model.exception.AppProcessNotWorkException
import com.rosan.installer.data.recycle.model.exception.RootNotWorkException
import com.rosan.installer.data.recycle.repo.Recycler
import java.util.StringTokenizer

class AppProcessRecycler(private val shell: String) : Recycler<AppProcess>() {
    private class CustomizeAppProcess(private val shell: String) : AppProcess.Terminal() {
        override fun newTerminal(): MutableList<String> {
            val st = StringTokenizer(shell)
            val cmdList = mutableListOf<String>()
            while (st.hasMoreTokens()) {
                cmdList.add(st.nextToken())
            }
            return cmdList
        }
    }

    override fun onMake(): AppProcess {
        return CustomizeAppProcess(shell).apply {
            if (init()) return@apply
            val command = shell.trim().split(' ').firstOrNull()
            if (command == "su") throw RootNotWorkException("Cannot access su command")
            else throw AppProcessNotWorkException("Cannot access command $command")
        }
    }
}