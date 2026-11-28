// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.ui.color.ColorPaletteStyle
import com.rosan.installer.ui.color.ColorSpecVersion
import com.rosan.installer.ui.color.ColorThemeConfig
import com.rosan.installer.ui.color.ColorThemeMode
import com.rosan.installer.ui.color.dynamicColorScheme as generateColorScheme

/** Generate a color scheme from saved theme preferences, using the spec that [style] supports. */
@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
): ColorScheme = generateColorScheme(
    keyColor = keyColor,
    isDark = isDark,
    style = style.toColorPaletteStyle(),
    specVersion = style.effectiveSpec(colorSpec).toColorSpecVersion(),
    contrastLevel = contrastLevel,
)

/** Map saved theme preferences to the color input shared by every UI family. */
fun colorThemeConfig(
    themeMode: ThemeMode,
    isDark: Boolean,
    seedColor: Color,
    useDynamicColor: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useAmoled: Boolean,
): ColorThemeConfig = ColorThemeConfig(
    mode = themeMode.toColorThemeMode(),
    isDark = isDark,
    seedColor = seedColor,
    useDynamicColor = useDynamicColor,
    paletteStyle = paletteStyle.toColorPaletteStyle(),
    preferredSpec = colorSpec.toColorSpecVersion(),
    effectiveSpec = paletteStyle.effectiveSpec(colorSpec).toColorSpecVersion(),
    useAmoled = useAmoled,
)

fun ThemeMode.toColorThemeMode(): ColorThemeMode = when (this) {
    ThemeMode.SYSTEM -> ColorThemeMode.System
    ThemeMode.LIGHT -> ColorThemeMode.Light
    ThemeMode.DARK -> ColorThemeMode.Dark
}

fun PaletteStyle.toColorPaletteStyle(): ColorPaletteStyle = when (this) {
    PaletteStyle.TonalSpot -> ColorPaletteStyle.TonalSpot
    PaletteStyle.Neutral -> ColorPaletteStyle.Neutral
    PaletteStyle.Vibrant -> ColorPaletteStyle.Vibrant
    PaletteStyle.Expressive -> ColorPaletteStyle.Expressive
    PaletteStyle.Rainbow -> ColorPaletteStyle.Rainbow
    PaletteStyle.FruitSalad -> ColorPaletteStyle.FruitSalad
    PaletteStyle.Monochrome -> ColorPaletteStyle.Monochrome
    PaletteStyle.Fidelity -> ColorPaletteStyle.Fidelity
    PaletteStyle.Content -> ColorPaletteStyle.Content
    PaletteStyle.Cmf -> ColorPaletteStyle.Cmf
}

fun ThemeColorSpec.toColorSpecVersion(): ColorSpecVersion = when (this) {
    ThemeColorSpec.SPEC_2021 -> ColorSpecVersion.Spec2021
    ThemeColorSpec.SPEC_2025 -> ColorSpecVersion.Spec2025
    ThemeColorSpec.SPEC_2026 -> ColorSpecVersion.Spec2026
}
