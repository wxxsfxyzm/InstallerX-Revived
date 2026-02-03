package com.rosan.installer.ui.activity.themestate

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.ThemeMode
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
    val manualSeedColorFlow = dataStore.getInt(AppDataStore.THEME_SEED_COLOR, PresetColors.first().color.toArgb())
        .map { Color(it) }
    // Dynamic wallpaper color (only for < Android 12)
    val wallpaperColorsFlow = getWallpaperColorsFlow()

    return combine(
        useMiuixFlow,
        themeModeFlow,
        paletteStyleFlow,
        useDynamicColorFlow,
        useMiuixMonetFlow,
        manualSeedColorFlow,
        wallpaperColorsFlow
    ) { values: Array<Any?> ->
        var idx = 0
        val useMiuix = values[idx++] as Boolean
        val themeMode = values[idx++] as ThemeMode
        val paletteStyle = values[idx++] as PaletteStyle
        val useDynamic = values[idx++] as Boolean
        val useMonet = values[idx++] as Boolean
        val manualSeedColor = values[idx++] as Color
        @Suppress("UNCHECKED_CAST") val wallpaperColors = values[idx] as? List<Int>
        // Calculate the effective seed color logic here.
        // 1. If dynamic color is ON and Android < 12, try to use the fetched wallpaper color.
        // 2. Otherwise (Dynamic OFF or Android 12+), use the manual seed color.
        // Note: For Android 12+, the UI layer will override this with system resources anyway if dynamic is ON.
        val effectiveSeedColor =
            if (useDynamic && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) if (!wallpaperColors.isNullOrEmpty()) Color(
                wallpaperColors[0]
            ) else manualSeedColor
            else manualSeedColor

        ThemeUiState(
            isLoaded = true,
            useMiuix = useMiuix,
            themeMode = themeMode,
            paletteStyle = paletteStyle,
            useDynamicColor = useDynamic,
            useMiuixMonet = useMonet,
            seedColor = effectiveSeedColor // Pass the resolved color
        )
    }
}

/**
 * Helper flow to fetch wallpaper colors asynchronously for Android 11 and below.
 */
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