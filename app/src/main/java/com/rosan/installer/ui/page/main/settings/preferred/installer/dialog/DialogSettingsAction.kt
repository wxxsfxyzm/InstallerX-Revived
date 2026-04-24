// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.dialog

sealed interface DialogSettingsAction {
    data class ChangeVersionCompareInSingleLine(val compareInSingleLine: Boolean) : DialogSettingsAction
    data class ChangeSdkCompareInMultiLine(val compareInMultiLine: Boolean) : DialogSettingsAction
    data class ChangeShowDialogInstallExtendedMenu(val showMenu: Boolean) : DialogSettingsAction
    data class ChangeShowSuggestion(val showSuggestion: Boolean) : DialogSettingsAction
    data class ChangeAutoSilentInstall(val autoSilentInstall: Boolean) : DialogSettingsAction
    data class ChangeShowDisableNotification(val disable: Boolean) : DialogSettingsAction
}
