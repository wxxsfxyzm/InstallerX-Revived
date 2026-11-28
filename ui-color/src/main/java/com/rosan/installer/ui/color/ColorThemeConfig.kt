// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.color

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Resolved color input shared by every UI family.
 *
 * @property mode The appearance mode the user selected.
 * @property isDark Whether the dark appearance is active after resolving [mode].
 * @property seedColor The seed color used when [useDynamicColor] is off or unavailable.
 * @property useDynamicColor Whether to use the system accent color on Android 12 and later.
 * @property paletteStyle The palette style used to generate the color scheme.
 * @property preferredSpec The color specification the user selected.
 * @property effectiveSpec The color specification that [paletteStyle] supports for [preferredSpec].
 * @property useAmoled Whether dark surfaces use pure black.
 */
@Immutable
data class ColorThemeConfig(
    val mode: ColorThemeMode,
    val isDark: Boolean,
    val seedColor: Color,
    val useDynamicColor: Boolean,
    val paletteStyle: ColorPaletteStyle,
    val preferredSpec: ColorSpecVersion,
    val effectiveSpec: ColorSpecVersion,
    val useAmoled: Boolean = false,
)
