// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.util

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.ui.unit.dp

// Define standard layout thresholds based on Material Design 3 guidelines
object UIConstants {
    val WIDE_SCREEN_THRESHOLD = 840.dp
    val MEDIUM_WIDTH_THRESHOLD = 600.dp

    // Threshold to distinguish phone landscape from tablet landscape
    val COMPACT_HEIGHT_THRESHOLD = 480.dp
    const val WIDE_ASPECT_RATIO_THRESHOLD = 1.2f
    const val PORTRAIT_ASPECT_RATIO_THRESHOLD = 1.4f
}

// Check if it's a typical tablet or desktop wide layout (excludes compact phone landscape)
val BoxWithConstraintsScope.isTabletLandscapeLayout: Boolean
    get() {
        // A phone in landscape usually has very limited height.
        // We exclude compact height from standard wide layouts to allow custom UI.
        if (maxHeight < UIConstants.COMPACT_HEIGHT_THRESHOLD) {
            return false
        }

        val isDefinitelyWide = maxWidth > UIConstants.WIDE_SCREEN_THRESHOLD
        val isWideByShape = maxWidth > UIConstants.MEDIUM_WIDTH_THRESHOLD &&
                (maxHeight / maxWidth < UIConstants.WIDE_ASPECT_RATIO_THRESHOLD)
        return isDefinitelyWide || isWideByShape
    }

// Check if the current constraints represent a portrait layout (typically phone or tablet in portrait)
val BoxWithConstraintsScope.isPortraitLayout: Boolean
    get() = maxWidth < maxHeight || (maxHeight / maxWidth > UIConstants.PORTRAIT_ASPECT_RATIO_THRESHOLD)

// Check specifically for phone landscape (wide width, but very limited height)
val BoxWithConstraintsScope.isPhoneLandscape: Boolean
    get() = maxWidth > maxHeight && maxHeight < UIConstants.COMPACT_HEIGHT_THRESHOLD