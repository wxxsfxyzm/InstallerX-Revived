// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.function.Consumer

/**
 * Applies a window-level blur-behind effect (Android 12+).
 * Correctly handles Activity and Dialog windows, and respects system settings.
 */
@Composable
fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int = 30) {
    // Window-level blur is only supported on Android 12 (API 31) and above.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val window = findCurrentWindow() ?: return
    val blurEnabledBySystem = isCrossWindowBlurEnabled()

    // Trigger effect when any parameter or system state changes.
    DisposableEffect(window, useBlur, blurRadius, blurEnabledBySystem) {
        if (useBlur && blurEnabledBySystem) {
            window.applyBlur(blurRadius)
        } else {
            window.clearBlur()
        }

        onDispose {
            // Ensure blur is removed when the composable leaves the tree.
            window.clearBlur()
        }
    }
}

/**
 * Finds the [Window] instance for the current composition.
 */
@Composable
private fun findCurrentWindow(): Window? {
    val view = LocalView.current
    // 1. Check if we are in a Dialog (Compose Dialogs use DialogWindowProvider)
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    if (dialogWindow != null) return dialogWindow

    // 2. Otherwise, find the Activity window
    return view.context.findActivity()?.window
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Reactive check for system-wide blur support.
 * Blur might be disabled by the user in Accessibility or automatically in Battery Saver mode.
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun isCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val wm = remember(context) { context.getSystemService(WindowManager::class.java) }

    // Initialize with the current real-time state instead of 'false'.
    var isEnabled by remember { mutableStateOf(wm.isCrossWindowBlurEnabled) }

    DisposableEffect(wm) {
        val listener = Consumer<Boolean> { enabled ->
            isEnabled = enabled
        }

        wm.addCrossWindowBlurEnabledListener(listener)

        onDispose {
            wm.removeCrossWindowBlurEnabledListener(listener)
        }
    }

    return isEnabled
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.applyBlur(radius: Int) {
    // FLAG_BLUR_BEHIND is required for window blur to work.
    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes = attributes.apply {
        blurBehindRadius = radius.coerceIn(0, 150) // System usually clamps to 150.
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Window.clearBlur() {
    clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes = attributes.apply {
        blurBehindRadius = 0
    }
}
