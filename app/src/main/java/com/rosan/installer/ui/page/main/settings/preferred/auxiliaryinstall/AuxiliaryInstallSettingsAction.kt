// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall

sealed class AuxiliaryInstallSettingsAction {
    data object OpenAccessibilitySettings : AuxiliaryInstallSettingsAction()
    data object RefreshAccessibilityServiceStatus : AuxiliaryInstallSettingsAction()
    data class ChangeAutoConfirmUsbInstall(val enabled: Boolean) : AuxiliaryInstallSettingsAction()
    data class ChangeShowToast(val enabled: Boolean) : AuxiliaryInstallSettingsAction()
    data class ChangeDelayedRetry(val enabled: Boolean) : AuxiliaryInstallSettingsAction()
    data class ChangeRequireScreenOn(val enabled: Boolean) : AuxiliaryInstallSettingsAction()
}
