// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.dialog

data class DialogSettingsState(
    val hideIdenticalComparisons: Boolean = true,
    val versionCompareInSingleLine: Boolean = false,
    val sdkCompareInMultiLine: Boolean = false,
    val showDialogInstallExtendedMenu: Boolean = false,
    val expandTemporarySettingsByDefault: Boolean = false,
    val showSmartSuggestion: Boolean = true,
    val autoSilentInstall: Boolean = false,
    val longClickBackgroundInstall: Boolean = true,
    val disableNotificationForDialogInstall: Boolean = false,
    val tapIconToShare: Boolean = false,
    val showFilePath: Boolean = false,
    val showInstallInitiator: Boolean = false
)
