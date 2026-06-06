// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import timber.log.Timber

@SuppressLint("SourceLockedOrientationActivity")
private fun Activity.requestPortraitOrientationSafely() {
    // Android 8.0 throws when a translucent / non-fullscreen activity requests orientation.
    // See: IllegalStateException("Only fullscreen activities can request orientation")
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
        Timber.w("Skip forcing portrait on Android 8.0 because translucent activities cannot request orientation.")
        return
    }

    try {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } catch (e: IllegalStateException) {
        Timber.w(e, "Failed to request portrait orientation.")
    }
}

fun Activity.requestPortraitOrientationOnPhoneSafely(isPhoneDevice: Boolean) {
    if (!isPhoneDevice) return
    requestPortraitOrientationSafely()
}