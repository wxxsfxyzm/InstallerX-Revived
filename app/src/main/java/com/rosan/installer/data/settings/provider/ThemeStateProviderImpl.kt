// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.provider

import android.os.Build
import androidx.compose.ui.graphics.Color
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.domain.settings.model.ThemeState
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

class ThemeStateProviderImpl(
    appSettingsRepo: AppSettingsRepository,
    appScope: CoroutineScope
) : ThemeStateProvider {
    override val themeStateFlow: StateFlow<ThemeState> = combine(
        appSettingsRepo.preferencesFlow,
        getWallpaperColorsFlow()
    ) { prefs, wallpaperColors ->
        val manualSeedColor = Color(prefs.seedColorInt)
        val effectiveSeedColor =
            if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) Color(wallpaperColors[0]) else manualSeedColor
            } else manualSeedColor

        ThemeState(
            isLoaded = true,
            useMiuix = prefs.showMiuixUI,
            themeMode = prefs.themeMode,
            paletteStyle = prefs.paletteStyle,
            colorSpec = prefs.colorSpec,
            useDynamicColor = prefs.useDynamicColor,
            useMiuixMonet = prefs.useMiuixMonet,
            useAppleFloatingBar = prefs.useAppleFloatingBar,
            seedColor = effectiveSeedColor,
            useBlur = prefs.useBlur,
            predictiveBackAnimation = prefs.predictiveBackAnimation,
            predictiveBackExitDirection = prefs.predictiveBackExitDirection
        )
    }.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeState()
    )

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
