package com.rosan.installer.domain.settings.model.preferences

import android.os.Build
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode

/**
 * A shared data class to hold the theme-related UI state.
 */
data class ThemeState(
    val isLoaded: Boolean = false,
    val useMiuix: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Int = 0xFF6750A4.toInt(),
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.MIUIX,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT
)
