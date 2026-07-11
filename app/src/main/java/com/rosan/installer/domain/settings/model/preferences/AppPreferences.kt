// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

import com.rosan.installer.domain.settings.model.app.NamedPackage
import com.rosan.installer.domain.settings.model.app.SharedUid
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeMode

/**
 * The aggregated state of all application preferences.
 */
data class AppPreferences(
    val authorizer: Authorizer,
    val alwaysUseRootInSystem: Boolean,
    val customizeAuthorizer: String,
    val hideIdenticalInstallComparisons: Boolean,
    val showDialogInstallExtendedMenu: Boolean,
    val expandDialogTemporarySettingsByDefault: Boolean,
    val showSmartSuggestion: Boolean,
    val disableNotificationForDialogInstall: Boolean,
    val showDialogWhenPressingNotification: Boolean,
    val closeSessionCountDown: Int,
    val notificationSuccessAutoClearSeconds: Int,
    val versionCompareInSingleLine: Boolean,
    val sdkCompareInMultiLine: Boolean,
    val showOPPOSpecial: Boolean,
    val checkAppSignature: Boolean,
    val showSignatureInfoOnMatch: Boolean,
    val showSignatureDetails: Boolean,
    val installerRequireBiometricAuth: BiometricAuthMode,
    val uninstallerRequireBiometricAuth: Boolean,
    val showLiveActivity: Boolean,
    val useMiIsland: Boolean,
    val useMiIslandBypassRestriction: Boolean,
    val useMiIslandOuterGlow: Boolean,
    val useMiIslandBlockingIntervalMs: Int,
    val autoLockInstaller: Boolean,
    val autoSilentInstall: Boolean,
    val longClickBackgroundInstall: Boolean,
    val tryMultipleAuthorizersOnInstall: Boolean,
    val smartAuthorizerCandidates: List<SmartAuthorizerCandidate>,
    val showMiuixUI: Boolean,
    val preferSystemIcon: Boolean,
    val showLauncherIcon: Boolean,
    val userSetLSPosedActive: Boolean,
    val detectXposedModule: Boolean,
    val quickOpenLSPosed: Boolean,
    val managedInstallerPackages: List<NamedPackage>,
    val managedBlacklistPackages: List<NamedPackage>,
    val managedSharedUserIdBlacklist: List<SharedUid>,
    val managedSharedUserIdExemptedPackages: List<NamedPackage>,
    val uninstallFlags: Int,
    // Lab Settings
    val githubUpdateChannel: GithubUpdateChannel,
    val customGithubProxyUrl: String,
    val labRootEnableModuleFlash: Boolean,
    val labRootShowModuleArt: Boolean,
    val labRootMode: RootMode,
    val labHttpProfile: HttpProfile,
    val labHttpSaveFile: Boolean,
    val labSetInstallRequester: Boolean,
    val labTapIconToShare: Boolean,
    val labShowFilePath: Boolean,
    val labShowInstallInitiator: Boolean,
    val labInstallWithoutUserAction: Boolean,
    val labRespectPlatformInstallPolicy: Boolean,
    val enableFileLogging: Boolean,
    // Theme Settings
    val themeMode: ThemeMode,
    val paletteStyle: PaletteStyle,
    val colorSpec: ThemeColorSpec,
    val useDynamicColor: Boolean,
    val useMiuixMonet: Boolean,
    val useAppleFloatingBar: Boolean,
    val seedColorInt: Int, // Stored as raw Int from DataStore
    val useDynColorFollowPkgIcon: Boolean,
    val useDynColorFollowPkgIconForLiveActivity: Boolean,
    val useBlur: Boolean,
    // Predictive Back Settings
    val predictiveBackAnimation: PredictiveBackAnimation,
    val predictiveBackExitDirection: PredictiveBackExitDirection
)
