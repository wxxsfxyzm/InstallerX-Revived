package com.rosan.installer.domain.settings.model

import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode

/**
 * A shared data class to hold the theme-related UI state.
 */
data class ThemeState(
    val isLoaded: Boolean = false,
    val useMiuix: Boolean = false,
    val isExpressive: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Color = PresetColors.first().color,
    val useBlur: Boolean = true
)