// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.repository

import com.rosan.installer.domain.settings.model.AppPreferences
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid
import kotlinx.coroutines.flow.Flow

enum class StringSetting {
    ThemeMode,
    ThemePaletteStyle,
    ThemeColorSpec,
    Authorizer,
    CustomizeAuthorizer,
    InstallMode,
    ApplyOrderType,
    LabRootImplementation,
    LabHttpProfile
}

enum class IntSetting {
    ThemeSeedColor,
    NotificationSuccessAutoClearSeconds,
    DialogAutoCloseCountdown,
    UninstallFlags
}

enum class BooleanSetting {
    UiUseBlur,
    UiExpressiveSwitch,
    ThemeUseDynamicColor,
    UiUseMiuix,
    UiUseMiuixMonet,
    UiUseAppleFloatingBar,
    UiDynColorFollowPkgIcon,
    LiveActivityDynColorFollowPkgIcon,
    ShowLiveActivity,
    ShowMiIsland,
    InstallerRequireBiometricAuth,
    UninstallerRequireBiometricAuth,
    ShowLauncherIcon,
    PreferSystemIconForInstall,
    ShowDialogWhenPressingNotification,
    AutoLockInstaller,
    UserReadScopeTips,
    ApplyOrderInReverse,
    ApplySelectedFirst,
    ApplyShowSystemApp,
    ApplyShowPackageName,
    DialogVersionCompareSingleLine,
    DialogSdkCompareMultiLine,
    DialogShowExtendedMenu,
    DialogShowIntelligentSuggestion,
    DialogDisableNotificationOnDismiss,
    DialogShowOppoSpecial,
    DialogAutoSilentInstall,
    LabEnableModuleFlash,
    LabModuleFlashShowArt,
    LabModuleAlwaysRoot,
    LabHttpSaveFile,
    LabSetInstallRequester,
    LabTapIconToShare,
    EnableFileLogging
}

enum class NamedPackageListSetting {
    ManagedInstallerPackages,
    ManagedBlacklistPackages,
    ManagedSharedUserIdExemptedPackages
}

enum class SharedUidListSetting {
    ManagedSharedUserIdBlacklist
}

interface AppSettingsRepo {
    val preferencesFlow: Flow<AppPreferences>

    suspend fun putString(setting: StringSetting, value: String)
    fun getString(setting: StringSetting, default: String = ""): Flow<String>

    suspend fun putInt(setting: IntSetting, value: Int)
    fun getInt(setting: IntSetting, default: Int = 0): Flow<Int>

    suspend fun putBoolean(setting: BooleanSetting, value: Boolean)
    fun getBoolean(setting: BooleanSetting, default: Boolean = false): Flow<Boolean>

    suspend fun putNamedPackageList(setting: NamedPackageListSetting, packages: List<NamedPackage>)
    fun getNamedPackageList(
        setting: NamedPackageListSetting,
        default: List<NamedPackage> = emptyList()
    ): Flow<List<NamedPackage>>

    suspend fun putSharedUidList(setting: SharedUidListSetting, uids: List<SharedUid>)
    fun getSharedUidList(
        setting: SharedUidListSetting,
        default: List<SharedUid> = emptyList()
    ): Flow<List<SharedUid>>

    suspend fun updateUninstallFlags(transform: (Int) -> Int)
}
