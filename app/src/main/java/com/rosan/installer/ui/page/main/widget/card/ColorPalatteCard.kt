// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.widget.card

import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.ui.theme.material.RawColor
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.util.getDisplayName
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Global cache to prevent UI thread blocking and ensuring instant load on revisit
private val colorSchemeCache = ConcurrentHashMap<String, ColorScheme>()

@Composable
fun ColorSwatchPreview(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    isSelected: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    onClick: () -> Unit,
) {
    val isDarkForPreview = false

    // Generate a unique cache key
    val cacheKey = remember(rawColor.color, currentStyle, colorSpec, isDarkForPreview) {
        "${rawColor.color.toArgb()}_${currentStyle.name}_${colorSpec.name}_$isDarkForPreview"
    }

    // Asynchronous calculation with instant update logic
    val scheme by produceState<ColorScheme?>(
        initialValue = colorSchemeCache[cacheKey], // Attempt cache hit for initial value
        key1 = cacheKey,
    ) {
        val cachedScheme = colorSchemeCache[cacheKey]
        if (cachedScheme != null) {
            // Instant apply if cached
            value = cachedScheme
        } else {
            // IMPORTANT: Do NOT set value to null here. Keep previous value (if any)
            // to avoid flicker. Calculate new scheme in background.
            withContext(Dispatchers.Default) {
                val newScheme = dynamicColorScheme(
                    keyColor = rawColor.color,
                    isDark = isDarkForPreview,
                    style = currentStyle,
                    colorSpec = colorSpec,
                )
                colorSchemeCache[cacheKey] = newScheme
                // Update value instantly once calculated. Compose will recompose.
                value = newScheme
            }
        }
    }

    val label = rawColor.getDisplayName()
    ColorSwatch(
        baseColor = rawColor.color,
        scheme = scheme,
        isSelected = isSelected,
        label = label.takeUnless { it === rawColor.key },
        textStyle = textStyle,
        textColor = textColor,
        onClick = onClick,
    )
}
