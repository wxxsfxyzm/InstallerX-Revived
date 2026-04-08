// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.SetLauncherIconUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.RawColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val setLauncherIconUseCase: SetLauncherIconUseCase
) : ViewModel() {

    val state: StateFlow<ThemeSettingsState> = combine(
        appSettingsRepo.preferencesFlow,
        systemEnvProvider.getWallpaperColorsFlow().onStart { emit(emptyList()) }
    ) { prefs, wallpaperColors ->
        val manualSeedColor = Color(prefs.seedColorInt)
        val effectiveSeedColor: Color = if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (!wallpaperColors.isNullOrEmpty()) {
                if (wallpaperColors.contains(manualSeedColor.toArgb())) {
                    manualSeedColor
                } else Color(wallpaperColors[0])
            } else manualSeedColor
        } else {
            if (PresetColors.any { it.color == manualSeedColor }) manualSeedColor else PresetColors[0].color
        }

        val availableColors: List<RawColor> = if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (!wallpaperColors.isNullOrEmpty()) {
                wallpaperColors.map { colorInt ->
                    RawColor(key = colorInt.toHexString(), color = Color(colorInt))
                }
            } else PresetColors
        } else PresetColors

        ThemeSettingsState(
            showMiuixUI = prefs.showMiuixUI,
            showExpressiveUI = prefs.showExpressiveUI,
            useBlur = prefs.useBlur,
            themeMode = prefs.themeMode,
            paletteStyle = prefs.paletteStyle,
            colorSpec = prefs.colorSpec,
            useDynamicColor = prefs.useDynamicColor,
            useMiuixMonet = prefs.useMiuixMonet,
            useAppleFloatingBar = prefs.useAppleFloatingBar,
            seedColor = effectiveSeedColor,
            availableColors = availableColors,
            useDynColorFollowPkgIcon = prefs.useDynColorFollowPkgIcon,
            useDynColorFollowPkgIconForLiveActivity = prefs.useDynColorFollowPkgIconForLiveActivity,
            preferSystemIcon = prefs.preferSystemIcon,
            showLauncherIcon = prefs.showLauncherIcon,
            showLiveActivity = prefs.showLiveActivity,
            predictiveBackAnimation = prefs.predictiveBackAnimation,
            predictiveBackExitDirection = prefs.predictiveBackExitDirection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeSettingsState()
    )

    fun dispatch(action: ThemeSettingsAction) {
        when (action) {
            is ThemeSettingsAction.ChangeUseMiuix -> viewModelScope.launch { updateSetting(BooleanSetting.UiUseMiuix, action.useMiuix) }
            is ThemeSettingsAction.ChangeShowExpressiveUI -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiExpressiveSwitch,
                    action.showRefreshedUI
                )
            }

            is ThemeSettingsAction.SetUseBlur -> viewModelScope.launch { updateSetting(BooleanSetting.UiUseBlur, action.enable) }
            is ThemeSettingsAction.SetThemeMode -> viewModelScope.launch { updateSetting(StringSetting.ThemeMode, action.mode.name) }
            is ThemeSettingsAction.SetPaletteStyle -> viewModelScope.launch { updateSetting(StringSetting.ThemePaletteStyle, action.style.name) }
            is ThemeSettingsAction.SetColorSpec -> viewModelScope.launch { updateSetting(StringSetting.ThemeColorSpec, action.spec.name) }
            is ThemeSettingsAction.SetUseDynamicColor -> viewModelScope.launch { updateSetting(BooleanSetting.ThemeUseDynamicColor, action.use) }
            is ThemeSettingsAction.SetUseMiuixMonet -> viewModelScope.launch { updateSetting(BooleanSetting.UiUseMiuixMonet, action.use) }
            is ThemeSettingsAction.SetUseAppleFloatingBar -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseAppleFloatingBar,
                    action.use
                )
            }

            is ThemeSettingsAction.SetDynColorFollowPkgIcon -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiDynColorFollowPkgIcon,
                    action.follow
                )
            }

            is ThemeSettingsAction.SetDynColorFollowPkgIconForLiveActivity -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LiveActivityDynColorFollowPkgIcon,
                    action.follow
                )
            }

            is ThemeSettingsAction.SetSeedColor -> viewModelScope.launch { updateSetting(IntSetting.ThemeSeedColor, action.color.toArgb()) }
            is ThemeSettingsAction.ChangePreferSystemIcon -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.PreferSystemIconForInstall,
                    action.preferSystemIcon
                )
            }

            is ThemeSettingsAction.ChangeShowLauncherIcon -> viewModelScope.launch { setLauncherIconUseCase(action.showLauncherIcon) }
            is ThemeSettingsAction.SetPredictiveBackAnimation -> viewModelScope.launch {
                updateSetting(
                    StringSetting.PredictiveBackAnimation,
                    action.animation.value
                )
            }

            is ThemeSettingsAction.SetPredictiveBackExitDirection -> viewModelScope.launch {
                updateSetting(
                    StringSetting.PredictiveBackExitDirection,
                    action.direction.value
                )
            }
        }
    }
}
