// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.pm.PackageInstaller

/** Approval is intermediate: only a later terminal result completes installation. */
internal suspend fun <T> awaitInstallResult(
    receive: suspend () -> T,
    status: (T) -> Int,
    onPendingUserAction: suspend (T) -> Unit,
): T {
    while (true) {
        val result = receive()
        if (status(result) != PackageInstaller.STATUS_PENDING_USER_ACTION) return result
        onPendingUserAction(result)
    }
}
