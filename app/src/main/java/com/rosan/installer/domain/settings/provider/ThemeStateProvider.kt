// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.provider

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.StringSetting
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

class ThemeStateProvider(private val appSettingsRepo: AppSettingsRepo) {

    val themeStateFlow: Flow<ThemeState> = combine(
        appSettingsRepo.getBoolean(BooleanSetting.UiUseMiuix, false),
        appSettingsRepo.getBoolean(BooleanSetting.UiExpressiveSwitch, true),
        appSettingsRepo.getString(StringSetting.ThemeMode, ThemeMode.SYSTEM.name)
            .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) },
        appSettingsRepo.getString(StringSetting.ThemePaletteStyle, PaletteStyle.TonalSpot.name)
            .map { runCatching { PaletteStyle.valueOf(it) }.getOrDefault(PaletteStyle.TonalSpot) },
        appSettingsRepo.getString(StringSetting.ThemeColorSpec, ThemeColorSpec.SPEC_2025.name)
            .map { runCatching { ThemeColorSpec.valueOf(it) }.getOrDefault(ThemeColorSpec.SPEC_2025) },
        appSettingsRepo.getBoolean(BooleanSetting.ThemeUseDynamicColor, true),
        appSettingsRepo.getBoolean(BooleanSetting.UiUseMiuixMonet, false),
        appSettingsRepo.getInt(IntSetting.ThemeSeedColor, PresetColors.first().color.toArgb())
            .map { Color(it) },
        appSettingsRepo.getBoolean(BooleanSetting.UiUseBlur, true),
        getWallpaperColorsFlow()
    ) { values: Array<Any?> ->
        var idx = 0
        val useMiuix = values[idx++] as Boolean
        val showExpressiveUI = values[idx++] as Boolean
        val themeMode = values[idx++] as ThemeMode
        val paletteStyle = values[idx++] as PaletteStyle
        val colorSpec = values[idx++] as ThemeColorSpec
        val useDynamic = values[idx++] as Boolean
        val useMonet = values[idx++] as Boolean
        val manualSeedColor = values[idx++] as Color
        val useBlur = values[idx++] as Boolean
        @Suppress("UNCHECKED_CAST") val wallpaperColors = values[idx] as? List<Int>

        val effectiveSeedColor =
            if (useDynamic && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) Color(wallpaperColors[0]) else manualSeedColor
            } else manualSeedColor

        ThemeState(
            isLoaded = true,
            useMiuix = useMiuix,
            isExpressive = showExpressiveUI,
            themeMode = themeMode,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            useDynamicColor = useDynamic,
            useMiuixMonet = useMonet,
            seedColor = effectiveSeedColor,
            useBlur = useBlur
        )
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
}
