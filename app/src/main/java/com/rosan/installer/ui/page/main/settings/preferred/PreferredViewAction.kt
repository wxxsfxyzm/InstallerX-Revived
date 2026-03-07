// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

sealed class PreferredViewAction {
    data class ChangeAutoLockInstaller(val autoLockInstaller: Boolean) : PreferredViewAction()
    data class SetAdbVerifyEnabledState(val enabled: Boolean) : PreferredViewAction()
    data object RequestIgnoreBatteryOptimization : PreferredViewAction()
    data object RefreshIgnoreBatteryOptimizationStatus : PreferredViewAction()
    data class SetDefaultInstaller(val lock: Boolean) : PreferredViewAction()
}
