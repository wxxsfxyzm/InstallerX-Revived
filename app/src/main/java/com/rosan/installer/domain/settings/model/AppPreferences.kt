// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode

// Represents the aggregated state of all application preferences.
data class AppPreferences(
    val authorizer: Authorizer,
    val customizeAuthorizer: String,
    val installMode: InstallMode,
    val showDialogInstallExtendedMenu: Boolean,
    val showSmartSuggestion: Boolean,
    val disableNotificationForDialogInstall: Boolean,
    val showDialogWhenPressingNotification: Boolean,
    val dhizukuAutoCloseCountDown: Int,
    val notificationSuccessAutoClearSeconds: Int,
    val versionCompareInSingleLine: Boolean,
    val sdkCompareInMultiLine: Boolean,
    val showOPPOSpecial: Boolean,
    val showExpressiveUI: Boolean,
    val installerRequireBiometricAuth: Boolean,
    val uninstallerRequireBiometricAuth: Boolean,
    val showLiveActivity: Boolean,
    val autoLockInstaller: Boolean,
    val autoSilentInstall: Boolean,
    val showMiuixUI: Boolean,
    val preferSystemIcon: Boolean,
    val showLauncherIcon: Boolean,
    val managedInstallerPackages: List<NamedPackage>,
    val managedBlacklistPackages: List<NamedPackage>,
    val managedSharedUserIdBlacklist: List<SharedUid>,
    val managedSharedUserIdExemptedPackages: List<NamedPackage>,
    val uninstallFlags: Int,
    // Lab Settings
    val labRootEnableModuleFlash: Boolean,
    val labRootShowModuleArt: Boolean,
    val labRootModuleAlwaysUseRoot: Boolean,
    val labRootImplementation: RootImplementation,
    val labUseMiIsland: Boolean,
    val labHttpProfile: HttpProfile,
    val labHttpSaveFile: Boolean,
    val labSetInstallRequester: Boolean,
    val labTapIconToShare: Boolean,
    val enableFileLogging: Boolean,
    // Theme Settings
    val themeMode: ThemeMode,
    val paletteStyle: PaletteStyle,
    val colorSpec: ThemeColorSpec,
    val useDynamicColor: Boolean,
    val useMiuixMonet: Boolean,
    val seedColorInt: Int, // Stored as raw Int from DataStore
    val useDynColorFollowPkgIcon: Boolean,
    val useDynColorFollowPkgIconForLiveActivity: Boolean,
    val useBlur: Boolean
)
