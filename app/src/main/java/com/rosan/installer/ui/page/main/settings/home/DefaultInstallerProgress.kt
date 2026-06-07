// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_INSTALLER_PROGRESS_MIN_MILLIS = 500L

internal suspend fun delayDefaultInstallerProgressIfNeeded(startedAtMillis: Long) {
    val elapsedMillis = SystemClock.elapsedRealtime() - startedAtMillis
    val remainingMillis = DEFAULT_INSTALLER_PROGRESS_MIN_MILLIS - elapsedMillis
    if (remainingMillis > 0L) {
        delay(remainingMillis.milliseconds)
    }
}
