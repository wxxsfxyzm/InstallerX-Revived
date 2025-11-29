package com.rosan.installer.ui.util

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

@Composable
fun isGestureNavigation(): Boolean {
    // API 29 (Android 10) introduced gesture nav, but tappableElement API came in API 30.
    // For API < 30, it is harder to detect reliably without using internal resources.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return false // Fallback strategy: assume 3-button on older devices usually
    }

    val density = LocalDensity.current
    val navBarInsets = WindowInsets.navigationBars
    val tappableInsets = WindowInsets.tappableElement

    // Calculate the bottom height in pixels
    val navBarBottom = navBarInsets.getBottom(density)
    val tappableBottom = tappableInsets.getBottom(density)

    return remember(navBarBottom, tappableBottom) {
        // Logic:
        // 1. If tappable bottom is 0, it means there are no buttons at the bottom -> Gesture.
        // 2. If tappable bottom is significantly smaller than nav bar bottom -> Gesture.
        // 3. In 3-button mode, tappable bottom usually equals nav bar bottom.
        tappableBottom == 0 || tappableBottom < navBarBottom
    }
}