// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid

data class InstallerSettingsState(
    val useBlur: Boolean = true,
    val authorizer: Authorizer = Authorizer.Shizuku,
    val alwaysUseRootInSystem: Boolean = false,
    val dhizukuAutoCloseCountDown: Int = 5,
    val installMode: InstallMode = InstallMode.Dialog,
    val showLiveActivity: Boolean = false,
    val installerRequireBiometricAuth: Boolean = false,
    val notificationSuccessAutoClearSeconds: Int = 10,
    val versionCompareInSingleLine: Boolean = false,
    val sdkCompareInMultiLine: Boolean = false,
    val showDialogInstallExtendedMenu: Boolean = false,
    val showSmartSuggestion: Boolean = true,
    val showDialogWhenPressingNotification: Boolean = false,
    val autoSilentInstall: Boolean = false,
    val disableNotificationForDialogInstall: Boolean = false,
    val showOPPOSpecial: Boolean = false,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val managedBlacklistPackages: List<NamedPackage> = emptyList(),
    val managedSharedUserIdBlacklist: List<SharedUid> = emptyList(),
    val managedSharedUserIdExemptedPackages: List<NamedPackage> = emptyList()
)
