// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

data class InstallerViewSettings(
    val useBlur: Boolean = true,
    val preferSystemIconForUpdates: Boolean = false,
    val closeSessionCountDown: Int = 3,
    val hideIdenticalComparisons: Boolean = true,
    val showExtendedMenu: Boolean = false,
    val expandTemporarySettingsByDefault: Boolean = false,
    val showSmartSuggestion: Boolean = true,
    val disableNotificationOnDismiss: Boolean = false,
    val versionCompareInSingleLine: Boolean = false,
    val sdkCompareInMultiLine: Boolean = false,
    val showOPPOSpecial: Boolean = false,
    val autoSilentInstall: Boolean = false,
    val longClickBackgroundInstall: Boolean = true,
    val enableModuleInstall: Boolean = false,
    val useDynColorFollowPkgIcon: Boolean = false,
    val checkAppSignature: Boolean = true,
    val showSignatureInfoOnMatch: Boolean = false,
    val showSignatureDetails: Boolean = false,
    val detectXposedModule: Boolean = true,
    val quickOpenLSPosed: Boolean = true,
    // Lab
    val labTapIconToShare: Boolean = false,
    val labShowFilePath: Boolean = false,
    val labShowInstallInitiator: Boolean = false
)
