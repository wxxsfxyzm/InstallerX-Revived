// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.recycler

import android.content.Context
import android.os.IBinder
import com.rosan.app_process.AppProcess
import com.rosan.installer.framework.privileged.lifecycle.Recyclable
import com.rosan.installer.framework.privileged.lifecycle.Recycler
import com.rosan.installer.framework.privileged.lifecycle.RecyclerManager
import org.koin.core.component.KoinComponent
import java.io.Closeable

/**
 * Manages an AppProcess used only for binder wrapping in root/custom hook mode.
 * It deliberately does not expose IPrivilegedService; hook callers use the wrapper directly.
 */
class ProcessHookRecycler(
    private val shell: String,
    private val context: Context,
    private val appProcessRecyclerManager: RecyclerManager<String, AppProcessRecycler>
) : Recycler<ProcessHookRecycler.HookedUserService>(), KoinComponent {

    class HookedUserService(
        private val appProcessHandle: Recyclable<AppProcess>
    ) : Closeable {
        fun binderWrapper(binder: IBinder): IBinder {
            return appProcessHandle.entity.binderWrapper(binder)
        }

        override fun close() {
            appProcessHandle.recycle()
        }
    }

    override fun onMake(): HookedUserService {
        // Obtain a raw AppProcess shell from the injected manager
        val appProcessHandle = appProcessRecyclerManager.get(shell).make()

        // Critical Fix: Ensure the reused AppProcess is initialized.
        // If the process was previously closed/recycled, its context/manager might be null.
        // init() checks state internally and is safe to call multiple times.
        if (!appProcessHandle.entity.init(context)) {
            // If init fails, we might want to recycle it and try fresh or throw,
            // but usually init() will re-create the manager.
            // If it fails returning false, it usually means binder connection failed.
            throw IllegalStateException("Failed to initialize AppProcess for Hook Mode.")
        }

        return HookedUserService(appProcessHandle)
    }
}
