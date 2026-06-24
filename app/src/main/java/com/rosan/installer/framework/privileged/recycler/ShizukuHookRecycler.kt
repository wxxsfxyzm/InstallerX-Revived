// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.recycler

import com.rosan.installer.framework.privileged.lifecycle.Recycler
import com.rosan.installer.framework.privileged.util.requireShizukuPermissionGranted
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.Closeable
import kotlin.time.Duration.Companion.milliseconds

/**
 * Validates Shizuku hook readiness without exposing a fake UserService.
 */
class ShizukuHookRecycler : Recycler<ShizukuHookRecycler.HookedUserService>(), KoinComponent {

    class HookedUserService : Closeable {
        override fun close() {
            Timber.tag("ShizukuHookRecycler").d("close() called, no action needed in hook mode.")
        }
    }

    override fun onMake(): HookedUserService = runBlocking {
        requireShizukuPermissionGranted {
            ensureBinderReady()
            HookedUserService()
        }
    }

    private suspend fun ensureBinderReady() {
        repeat(5) {
            if (Shizuku.pingBinder()) return
            delay(100.milliseconds)
        }
    }
}
