package com.rosan.installer.data.recycle.model.impl.recycler

import com.rosan.app_process.AppProcess
import com.rosan.installer.data.recycle.model.exception.AppProcessNotWorkException
import com.rosan.installer.data.recycle.model.exception.RootNotWorkException
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.util.SHELL_ROOT

class AppProcessRecycler(private val shell: String) : Recycler<AppProcess>() {

    // OPTIMIZATION: Fixes the "Double Delay" issue.
    // Since the upper layer (ProcessUserServiceRecycler) already buffers for 15s,
    // this underlying shell process doesn't need to wait another 15s.
    // We set it to a very short time (100ms) to clean up almost immediately
    // after the Service layer releases it.
    override val delayDuration = 100L

    private class CustomizeAppProcess(private val shell: String) : AppProcess.Terminal() {
        override fun newTerminal(): MutableList<String> {
            // Use regex split instead of legacy StringTokenizer.
            // This handles multiple spaces gracefully (e.g., "sh  -c").
            return shell.trim().split("\\s+".toRegex()).toMutableList()
        }
    }

    override fun onMake(): AppProcess {
        return CustomizeAppProcess(shell).apply {
            if (init()) return@apply
            val command = shell.trim().split(' ').firstOrNull()
            if (command == SHELL_ROOT) throw RootNotWorkException("Cannot access su command")
            else throw AppProcessNotWorkException("Cannot access command $command")
        }
    }
}