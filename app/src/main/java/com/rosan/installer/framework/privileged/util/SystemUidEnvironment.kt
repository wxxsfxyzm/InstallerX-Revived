// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.util

import android.system.Os
import timber.log.Timber

object SystemUidEnvironment {
    private const val ROOT_UID = 0
    private const val SYSTEM_UID = 1000
    private const val SHELL_UID = 2000

    @Suppress("DEPRECATION")
    fun switchRootToSystemIfNeeded(tag: String): Int {
        val euid = Os.geteuid()
        return when (euid) {
            ROOT_UID -> {
                check(Os.gettid() == Os.getpid()) {
                    "Cannot switch root service euid outside the main thread."
                }
                Timber.tag(tag).d("Root service detected. Switching euid to system.")
                Os.seteuid(SYSTEM_UID)
                SYSTEM_UID
            }

            SYSTEM_UID, SHELL_UID -> euid
            else -> error("Unexpected service euid: $euid")
        }
    }

    fun shizukuPackageNameFor(uidMode: UserServiceUidMode, tag: String): String {
        if (uidMode == UserServiceUidMode.Default) return "com.android.shell"

        return when (switchRootToSystemIfNeeded(tag)) {
            SYSTEM_UID -> "android"
            SHELL_UID -> "com.android.shell"
            else -> error("Unexpected Shizuku service euid after environment setup.")
        }
    }
}
