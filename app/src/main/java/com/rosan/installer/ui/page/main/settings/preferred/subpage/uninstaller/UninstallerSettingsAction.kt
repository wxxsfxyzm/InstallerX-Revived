// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller

sealed class UninstallerSettingsAction {
    data class ToggleGlobalUninstallFlag(val flag: Int, val enable: Boolean) : UninstallerSettingsAction()
    data class ChangeBiometricAuth(val require: Boolean) : UninstallerSettingsAction()
}
