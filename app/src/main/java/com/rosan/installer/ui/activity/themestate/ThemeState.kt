// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity.themestate

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * A shared data class to hold the theme-related UI state.
 */
data class ThemeUiState(
    val isLoaded: Boolean = false,
    val useMiuix: Boolean = false,
    val isExpressive: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val seedColor: Color = PresetColors.first().color
)

/**
 * A reusable factory function that encapsulates the logic for creating
 * a Flow of theme UI state from AppDataStore.
 */
fun createThemeUiStateFlow(dataStore: AppDataStore): Flow<ThemeUiState> {
    val useMiuixFlow = dataStore.getBoolean(AppDataStore.UI_USE_MIUIX, false)
    val showExpressiveUIFlow = dataStore.getBoolean(AppDataStore.UI_EXPRESSIVE_SWITCH, true) // Flow for Expressive UI
    val themeModeFlow = dataStore.getString(AppDataStore.THEME_MODE, ThemeMode.SYSTEM.name)
        .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
    val paletteStyleFlow = dataStore.getString(AppDataStore.THEME_PALETTE_STYLE, PaletteStyle.TonalSpot.name)
        .map { runCatching { PaletteStyle.valueOf(it) }.getOrDefault(PaletteStyle.TonalSpot) }
    val colorSpecFlow = dataStore.getString(AppDataStore.THEME_COLOR_SPEC, ThemeColorSpec.SPEC_2025.name)
        .map { runCatching { ThemeColorSpec.valueOf(it) }.getOrDefault(ThemeColorSpec.SPEC_2025) }
    val useDynamicColorFlow = dataStore.getBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, true)
    val useMiuixMonetFlow = dataStore.getBoolean(AppDataStore.UI_USE_MIUIX_MONET, false)
    val manualSeedColorFlow = dataStore.getInt(AppDataStore.THEME_SEED_COLOR, PresetColors.first().color.toArgb())
        .map { Color(it) }
    val wallpaperColorsFlow = getWallpaperColorsFlow()

    return combine(
        useMiuixFlow,
        showExpressiveUIFlow, // Include in combine
        themeModeFlow,
        paletteStyleFlow,
        colorSpecFlow,
        useDynamicColorFlow,
        useMiuixMonetFlow,
        manualSeedColorFlow,
        wallpaperColorsFlow
    ) { values: Array<Any?> ->
        var idx = 0
        val useMiuix = values[idx++] as Boolean
        val showExpressiveUI = values[idx++] as Boolean // Extract Expressive UI value
        val themeMode = values[idx++] as ThemeMode
        val paletteStyle = values[idx++] as PaletteStyle
        val colorSpec = values[idx++] as ThemeColorSpec
        val useDynamic = values[idx++] as Boolean
        val useMonet = values[idx++] as Boolean
        val manualSeedColor = values[idx++] as Color
        @Suppress("UNCHECKED_CAST") val wallpaperColors = values[idx] as? List<Int>

        val effectiveSeedColor =
            if (useDynamic && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) if (!wallpaperColors.isNullOrEmpty()) Color(
                wallpaperColors[0]
            ) else manualSeedColor
            else manualSeedColor

        ThemeUiState(
            isLoaded = true,
            useMiuix = useMiuix,
            isExpressive = showExpressiveUI, // Pass to state
            themeMode = themeMode,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            useDynamicColor = useDynamic,
            useMiuixMonet = useMonet,
            seedColor = effectiveSeedColor
        )
    }
}

private fun getWallpaperColorsFlow(): Flow<List<Int>?> = flow {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val colors = try {
            MonetCompat.getInstance().getAvailableWallpaperColors()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Monet colors in ThemeState")
            null
        }
        emit(colors)
    } else {
        emit(null)
    }
}.flowOn(Dispatchers.IO)