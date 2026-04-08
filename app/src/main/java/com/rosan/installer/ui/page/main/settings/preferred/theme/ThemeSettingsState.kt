// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.RawColor
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode

data class ThemeSettingsState(
    val showMiuixUI: Boolean = false,
    val showExpressiveUI: Boolean = true,
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Color = PresetColors[0].color,
    val availableColors: List<RawColor> = PresetColors,
    val useDynColorFollowPkgIcon: Boolean = false,
    val useDynColorFollowPkgIconForLiveActivity: Boolean = false,
    val preferSystemIcon: Boolean = false,
    val showLauncherIcon: Boolean = true,
    val showLiveActivity: Boolean = false,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.None,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.FOLLOW_GESTURE
)
