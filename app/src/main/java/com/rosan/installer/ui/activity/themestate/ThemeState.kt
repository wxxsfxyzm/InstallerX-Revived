package com.rosan.installer.ui.activity.themestate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * A shared data class to hold the theme-related UI state.
 */
data class ThemeUiState(
    val isLoaded: Boolean = false,
    val useMiuix: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val seedColor: Color = PresetColors.first().color
)

/**
 * A reusable factory function that encapsulates the logic for creating
 * a Flow of theme UI state from AppDataStore.
 * @param dataStore The instance of AppDataStore to read settings from.
 * @return A Flow that emits ThemeUiState updates.
 */
fun createThemeUiStateFlow(dataStore: AppDataStore): Flow<ThemeUiState> {
    val useMiuixFlow = dataStore.getBoolean(AppDataStore.UI_USE_MIUIX, false)
    val themeModeFlow = dataStore.getString(AppDataStore.THEME_MODE, ThemeMode.SYSTEM.name)
        .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
    val paletteStyleFlow = dataStore.getString(AppDataStore.THEME_PALETTE_STYLE, PaletteStyle.TonalSpot.name)
        .map { runCatching { PaletteStyle.valueOf(it) }.getOrDefault(PaletteStyle.TonalSpot) }
    val useDynamicColorFlow = dataStore.getBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, true)
    val useMiuixMonetFlow = dataStore.getBoolean(AppDataStore.UI_USE_MIUIX_MONET, false)
    val seedColorFlow = dataStore.getInt(AppDataStore.THEME_SEED_COLOR, PresetColors.first().color.toArgb())
        .map { Color(it) }

    return combine(
        useMiuixFlow,
        themeModeFlow,
        paletteStyleFlow,
        useDynamicColorFlow,
        useMiuixMonetFlow,
        seedColorFlow
    ) { values: Array<Any?> ->
        var idx = 0
        val useMiuix = values[idx++] as Boolean
        val themeMode = values[idx++] as ThemeMode
        val paletteStyle = values[idx++] as PaletteStyle
        val useDynamic = values[idx++] as Boolean
        val useMonet = values[idx++] as Boolean
        val seedColor = values[idx] as Color
        ThemeUiState(
            isLoaded = true,
            useMiuix = useMiuix,
            themeMode = themeMode,
            paletteStyle = paletteStyle,
            useDynamicColor = useDynamic,
            useMiuixMonet = useMonet,
            seedColor = seedColor
        )
    }
}