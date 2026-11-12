// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.infrastructure.recycler

import android.content.Context
import android.os.IBinder
import com.rosan.app_process.AppProcess
import com.rosan.installer.framework.privileged.core.infrastructure.lifecycle.Recyclable
import com.rosan.installer.framework.privileged.core.infrastructure.lifecycle.Recycler
import com.rosan.installer.framework.privileged.core.infrastructure.lifecycle.RecyclerManager
import com.rosan.installer.framework.privileged.core.infrastructure.process.AppProcessTerminal
import java.io.Closeable
import org.koin.core.component.KoinComponent

/**
 * Manages an AppProcess used only for binder wrapping in root/custom hook mode.
 * It deliberately does not expose IPrivilegedService; hook callers use the wrapper directly.
 */
class ProcessHookRecycler(
    private val terminal: AppProcessTerminal,
    private val context: Context,
    private val appProcessRecyclerManager: RecyclerManager<AppProcessTerminal, AppProcessRecycler>,
) : Recycler<ProcessHookRecycler.HookedUserService>(),
    KoinComponent {

    override val delayDuration: Long = 5_000L

    class HookedUserService(private val appProcessHandle: Recyclable<AppProcess>) : Closeable {
        fun binderWrapper(binder: IBinder): IBinder = appProcessHandle.entity.binderWrapper(binder)

        override fun close() {
            appProcessHandle.recycle()
        }
    }

    override fun onMake(): HookedUserService {
        // Obtain a raw AppProcess shell from the injected manager
        val appProcessHandle = appProcessRecyclerManager.get(terminal).make()

        // Critical Fix: Ensure the reused AppProcess is initialized.
        // If the process was previously closed/recycled, its context/manager might be null.
        // init() checks state internally and is safe to call multiple times.
        if (!appProcessHandle.entity.init(context)) {
            // Release the handle so AppProcessRecycler's refCount can drop to zero and recycle the entity.
            appProcessHandle.recycle()
            throw IllegalStateException("Failed to initialize AppProcess for Hook Mode.")
        }

        return HookedUserService(appProcessHandle)
    }
}
