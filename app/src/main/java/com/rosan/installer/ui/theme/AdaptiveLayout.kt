// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

// Standard Material 3 window size classes
enum class WindowLayoutType {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

// Unified layout state object
data class WindowLayoutInfo(
    val type: WindowLayoutType,
    val isLandscape: Boolean,
    val showNavigationRail: Boolean,
    val isMediumPortrait: Boolean,
)

// Global provider for layout state
val LocalWindowLayoutInfo = compositionLocalOf<WindowLayoutInfo> {
    error("WindowLayoutInfo not provided")
}

/**
 * Detects if the device is a phone (smallest width < 600dp)
 */
val Context.isPhoneDevice: Boolean
    get() = resources.configuration.smallestScreenWidthDp < 600

/**
 * Gets the WindowLayoutType for the current configuration
 */
val Configuration.windowLayoutType: WindowLayoutType
    get() = when {
        screenWidthDp >= 840 -> WindowLayoutType.EXPANDED
        screenWidthDp >= 600 -> WindowLayoutType.MEDIUM
        else -> WindowLayoutType.COMPACT
    }

@Composable
fun rememberWindowLayoutInfo(): WindowLayoutInfo {
    // Use Compose native API to get the exact container bounds in pixels
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current

    // Convert pixels to DP safely
    val screenWidthDp = with(density) { containerSize.width.toDp().value }
    val screenHeightDp = with(density) { containerSize.height.toDp().value }

    val isLandscape = screenWidthDp > screenHeightDp

    val type = when {
        screenWidthDp >= 840f -> WindowLayoutType.EXPANDED
        screenWidthDp >= 600f -> WindowLayoutType.MEDIUM
        else -> WindowLayoutType.COMPACT
    }

    // Expanded layout: >= 840dp OR medium devices in landscape (like foldables)
    val showNavigationRail = type == WindowLayoutType.EXPANDED || (type == WindowLayoutType.MEDIUM && isLandscape)

    // Medium layout: >= 600dp but in portrait mode (like foldables in portrait or small tablets)
    val isMediumPortrait = type == WindowLayoutType.MEDIUM && !isLandscape

    return WindowLayoutInfo(
        type = type,
        isLandscape = isLandscape,
        showNavigationRail = showNavigationRail,
        isMediumPortrait = isMediumPortrait,
    )
}
