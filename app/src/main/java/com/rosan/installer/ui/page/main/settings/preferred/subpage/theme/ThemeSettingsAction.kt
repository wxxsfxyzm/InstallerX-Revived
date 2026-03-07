// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.theme

import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode

sealed class ThemeSettingsAction {
    data class ChangeUseMiuix(val useMiuix: Boolean) : ThemeSettingsAction()
    data class ChangeShowExpressiveUI(val showRefreshedUI: Boolean) : ThemeSettingsAction()
    data class SetUseBlur(val enable: Boolean) : ThemeSettingsAction()
    data class SetThemeMode(val mode: ThemeMode) : ThemeSettingsAction()
    data class SetPaletteStyle(val style: PaletteStyle) : ThemeSettingsAction()
    data class SetColorSpec(val spec: ThemeColorSpec) : ThemeSettingsAction()
    data class SetUseDynamicColor(val use: Boolean) : ThemeSettingsAction()
    data class SetUseMiuixMonet(val use: Boolean) : ThemeSettingsAction()
    data class SetDynColorFollowPkgIcon(val follow: Boolean) : ThemeSettingsAction()
    data class SetDynColorFollowPkgIconForLiveActivity(val follow: Boolean) : ThemeSettingsAction()
    data class SetSeedColor(val color: Color) : ThemeSettingsAction()
    data class ChangePreferSystemIcon(val preferSystemIcon: Boolean) : ThemeSettingsAction()
    data class ChangeShowLauncherIcon(val showLauncherIcon: Boolean) : ThemeSettingsAction()
}
