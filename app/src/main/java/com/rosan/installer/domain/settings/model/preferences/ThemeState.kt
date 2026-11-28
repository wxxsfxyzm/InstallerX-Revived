package com.rosan.installer.domain.settings.model.preferences

import android.os.Build
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode
import com.rosan.installer.domain.settings.model.preferences.theme.UiStyle

/**
 * A shared data class to hold the theme-related UI state.
 */
data class ThemeState(
    val isLoaded: Boolean = false,
    val uiStyle: UiStyle = UiStyle.MATERIAL3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useAmoled: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useMiuixOs4: Boolean = false,
    val useMiuixMonet: Boolean = false,
    val useCouiMonet: Boolean = false,
    val useCouiContourGlow: Boolean = true,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Int = 0xFF6750A4.toInt(),
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.MIUIX,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.ALWAYS_RIGHT,
) {
    val useMiuix: Boolean get() = uiStyle == UiStyle.MIUIX
    val useCoui: Boolean get() = uiStyle == UiStyle.COUI
}
