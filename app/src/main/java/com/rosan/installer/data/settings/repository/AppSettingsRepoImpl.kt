// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.Preferences
import com.rosan.installer.data.settings.local.datastore.AppDataStore
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.AppPreferences
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.RootImplementation
import com.rosan.installer.domain.settings.model.SharedUid
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.ui.theme.material.PaletteStyle
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.ThemeColorSpec
import com.rosan.installer.ui.theme.material.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn

class AppSettingsRepoImpl(
    private val appDataStore: AppDataStore,
    capabilityProvider: DeviceCapabilityProvider,
    appScope: CoroutineScope
) : AppSettingsRepo {
    override val preferencesFlow: Flow<AppPreferences> = combine(
        listOf(
            appDataStore.getString(
                AppDataStore.AUTHORIZER,
                if (capabilityProvider.isSystemApp) Authorizer.None.value else Authorizer.Shizuku.value
            ),
            appDataStore.getString(AppDataStore.CUSTOMIZE_AUTHORIZER, ""),
            appDataStore.getString(AppDataStore.INSTALL_MODE, InstallMode.Dialog.value),
            appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, false),
            appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, true),
            appDataStore.getBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, false),
            appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true),
            appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3),
            appDataStore.getInt(AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS, 0),
            appDataStore.getBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, false),
            appDataStore.getBoolean(AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE, false),
            appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_OPPO_SPECIAL, false),
            appDataStore.getBoolean(AppDataStore.UI_EXPRESSIVE_SWITCH, true),
            appDataStore.getBoolean(AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH, false),
            appDataStore.getBoolean(AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH, false),
            appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false),
            appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER, false),
            appDataStore.getBoolean(AppDataStore.DIALOG_AUTO_SILENT_INSTALL, false),
            appDataStore.getBoolean(AppDataStore.UI_USE_MIUIX, false),
            appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false),
            appDataStore.getBoolean(AppDataStore.SHOW_LAUNCHER_ICON, true),

            // Lists
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST),
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST),
            appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST),
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST),

            appDataStore.getInt(AppDataStore.UNINSTALL_FLAGS, 0),

            // Lab settings
            appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false),
            appDataStore.getBoolean(AppDataStore.LAB_MODULE_FLASH_SHOW_ART, true),
            appDataStore.getBoolean(AppDataStore.LAB_MODULE_ALWAYS_ROOT, false),
            appDataStore.getString(AppDataStore.LAB_ROOT_IMPLEMENTATION, "Default"),
            appDataStore.getBoolean(AppDataStore.SHOW_MI_ISLAND, false),
            appDataStore.getString(AppDataStore.LAB_HTTP_PROFILE, "Default"),
            appDataStore.getBoolean(AppDataStore.LAB_HTTP_SAVE_FILE, false),
            appDataStore.getBoolean(AppDataStore.LAB_SET_INSTALL_REQUESTER, false),
            appDataStore.getBoolean(AppDataStore.LAB_TAP_ICON_TO_SHARE, false),
            appDataStore.getBoolean(AppDataStore.ENABLE_FILE_LOGGING, true),

            // Theme settings
            appDataStore.getString(AppDataStore.THEME_MODE, ThemeMode.SYSTEM.name),
            appDataStore.getString(AppDataStore.THEME_PALETTE_STYLE, PaletteStyle.TonalSpot.name),
            appDataStore.getString(AppDataStore.THEME_COLOR_SPEC, ThemeColorSpec.SPEC_2025.name),
            appDataStore.getBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, true),
            appDataStore.getBoolean(AppDataStore.UI_USE_MIUIX_MONET, false),
            appDataStore.getBoolean(AppDataStore.UI_USE_APPLE_FLOATING_BAR, false),
            appDataStore.getInt(AppDataStore.THEME_SEED_COLOR, PresetColors.first().color.toArgb()),
            appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false),
            appDataStore.getBoolean(AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON, false),
            appDataStore.getBoolean(AppDataStore.UI_USE_BLUR, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        )
    ) { values: Array<Any?> ->
        var idx = 0

        // Map raw strings back to Domain Enums
        val authorizerStr = values[idx++] as String
        val authorizer = Authorizer.entries.find { it.value == authorizerStr } ?: Authorizer.Global
        val customizeAuthorizer = values[idx++] as String
        val installModeStr = values[idx++] as String
        val installMode = InstallMode.entries.find { it.value == installModeStr } ?: InstallMode.Global

        @Suppress("UNCHECKED_CAST")
        AppPreferences(
            authorizer = authorizer,
            customizeAuthorizer = customizeAuthorizer,
            installMode = installMode,
            showDialogInstallExtendedMenu = values[idx++] as Boolean,
            showSmartSuggestion = values[idx++] as Boolean,
            disableNotificationForDialogInstall = values[idx++] as Boolean,
            showDialogWhenPressingNotification = values[idx++] as Boolean,
            dhizukuAutoCloseCountDown = values[idx++] as Int,
            notificationSuccessAutoClearSeconds = values[idx++] as Int,
            versionCompareInSingleLine = values[idx++] as Boolean,
            sdkCompareInMultiLine = values[idx++] as Boolean,
            showOPPOSpecial = values[idx++] as Boolean,
            showExpressiveUI = values[idx++] as Boolean,
            installerRequireBiometricAuth = values[idx++] as Boolean,
            uninstallerRequireBiometricAuth = values[idx++] as Boolean,
            showLiveActivity = values[idx++] as Boolean,
            autoLockInstaller = values[idx++] as Boolean,
            autoSilentInstall = values[idx++] as Boolean,
            showMiuixUI = values[idx++] as Boolean,
            preferSystemIcon = values[idx++] as Boolean,
            showLauncherIcon = values[idx++] as Boolean,

            managedInstallerPackages = values[idx++] as List<NamedPackage>,
            managedBlacklistPackages = values[idx++] as List<NamedPackage>,
            managedSharedUserIdBlacklist = values[idx++] as List<SharedUid>,
            managedSharedUserIdExemptedPackages = values[idx++] as List<NamedPackage>,

            uninstallFlags = values[idx++] as Int,
            labRootEnableModuleFlash = values[idx++] as Boolean,
            labRootShowModuleArt = values[idx++] as Boolean,
            labRootModuleAlwaysUseRoot = values[idx++] as Boolean,
            labRootImplementation = RootImplementation.fromString(values[idx++] as String),
            labUseMiIsland = values[idx++] as Boolean,
            labHttpProfile = HttpProfile.fromString(values[idx++] as String),
            labHttpSaveFile = values[idx++] as Boolean,
            labSetInstallRequester = values[idx++] as Boolean,
            labTapIconToShare = values[idx++] as Boolean,
            enableFileLogging = values[idx++] as Boolean,

            themeMode = runCatching { ThemeMode.valueOf(values[idx++] as String) }.getOrDefault(ThemeMode.SYSTEM),
            paletteStyle = runCatching { PaletteStyle.valueOf(values[idx++] as String) }.getOrDefault(PaletteStyle.TonalSpot),
            colorSpec = runCatching { ThemeColorSpec.valueOf(values[idx++] as String) }.getOrDefault(ThemeColorSpec.SPEC_2025),
            useDynamicColor = values[idx++] as Boolean,
            useMiuixMonet = values[idx++] as Boolean,
            useAppleFloatingBar = values[idx++] as Boolean,
            seedColorInt = values[idx++] as Int,
            useDynColorFollowPkgIcon = values[idx++] as Boolean,
            useDynColorFollowPkgIconForLiveActivity = values[idx++] as Boolean,
            useBlur = values[idx++] as Boolean
        )
    }.shareIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    override suspend fun putString(setting: StringSetting, value: String) =
        appDataStore.putString(stringKey(setting), value)

    override fun getString(setting: StringSetting, default: String): Flow<String> =
        appDataStore.getString(stringKey(setting), default)

    override suspend fun putInt(setting: IntSetting, value: Int) =
        appDataStore.putInt(intKey(setting), value)

    override fun getInt(setting: IntSetting, default: Int): Flow<Int> =
        appDataStore.getInt(intKey(setting), default)

    override suspend fun putBoolean(setting: BooleanSetting, value: Boolean) =
        appDataStore.putBoolean(booleanKey(setting), value)

    override fun getBoolean(setting: BooleanSetting, default: Boolean): Flow<Boolean> =
        appDataStore.getBoolean(booleanKey(setting), default)

    override suspend fun putNamedPackageList(
        setting: NamedPackageListSetting,
        packages: List<NamedPackage>
    ) = appDataStore.putNamedPackageList(namedPackageListKey(setting), packages)

    override fun getNamedPackageList(
        setting: NamedPackageListSetting,
        default: List<NamedPackage>
    ): Flow<List<NamedPackage>> = appDataStore.getNamedPackageList(namedPackageListKey(setting), default)

    override suspend fun putSharedUidList(setting: SharedUidListSetting, uids: List<SharedUid>) =
        appDataStore.putSharedUidList(sharedUidListKey(setting), uids)

    override fun getSharedUidList(
        setting: SharedUidListSetting,
        default: List<SharedUid>
    ): Flow<List<SharedUid>> = appDataStore.getSharedUidList(sharedUidListKey(setting), default)

    override suspend fun updateUninstallFlags(transform: (Int) -> Int) =
        appDataStore.updateUninstallFlags(transform)

    private fun stringKey(setting: StringSetting): Preferences.Key<String> =
        when (setting) {
            StringSetting.ThemeMode -> AppDataStore.THEME_MODE
            StringSetting.ThemePaletteStyle -> AppDataStore.THEME_PALETTE_STYLE
            StringSetting.ThemeColorSpec -> AppDataStore.THEME_COLOR_SPEC
            StringSetting.Authorizer -> AppDataStore.AUTHORIZER
            StringSetting.CustomizeAuthorizer -> AppDataStore.CUSTOMIZE_AUTHORIZER
            StringSetting.InstallMode -> AppDataStore.INSTALL_MODE
            StringSetting.ApplyOrderType -> AppDataStore.APPLY_ORDER_TYPE
            StringSetting.LabRootImplementation -> AppDataStore.LAB_ROOT_IMPLEMENTATION
            StringSetting.LabHttpProfile -> AppDataStore.LAB_HTTP_PROFILE
        }

    private fun intKey(setting: IntSetting): Preferences.Key<Int> =
        when (setting) {
            IntSetting.ThemeSeedColor -> AppDataStore.THEME_SEED_COLOR
            IntSetting.NotificationSuccessAutoClearSeconds -> AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS
            IntSetting.DialogAutoCloseCountdown -> AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN
            IntSetting.UninstallFlags -> AppDataStore.UNINSTALL_FLAGS
        }

    private fun booleanKey(setting: BooleanSetting): Preferences.Key<Boolean> =
        when (setting) {
            BooleanSetting.UiUseBlur -> AppDataStore.UI_USE_BLUR
            BooleanSetting.UiExpressiveSwitch -> AppDataStore.UI_EXPRESSIVE_SWITCH
            BooleanSetting.ThemeUseDynamicColor -> AppDataStore.THEME_USE_DYNAMIC_COLOR
            BooleanSetting.UiUseMiuix -> AppDataStore.UI_USE_MIUIX
            BooleanSetting.UiUseMiuixMonet -> AppDataStore.UI_USE_MIUIX_MONET
            BooleanSetting.UiUseAppleFloatingBar -> AppDataStore.UI_USE_APPLE_FLOATING_BAR
            BooleanSetting.UiDynColorFollowPkgIcon -> AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON
            BooleanSetting.LiveActivityDynColorFollowPkgIcon -> AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON
            BooleanSetting.ShowLiveActivity -> AppDataStore.SHOW_LIVE_ACTIVITY
            BooleanSetting.ShowMiIsland -> AppDataStore.SHOW_MI_ISLAND
            BooleanSetting.InstallerRequireBiometricAuth -> AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH
            BooleanSetting.UninstallerRequireBiometricAuth -> AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH
            BooleanSetting.ShowLauncherIcon -> AppDataStore.SHOW_LAUNCHER_ICON
            BooleanSetting.PreferSystemIconForInstall -> AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL
            BooleanSetting.ShowDialogWhenPressingNotification -> AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION
            BooleanSetting.AutoLockInstaller -> AppDataStore.AUTO_LOCK_INSTALLER
            BooleanSetting.UserReadScopeTips -> AppDataStore.USER_READ_SCOPE_TIPS
            BooleanSetting.ApplyOrderInReverse -> AppDataStore.APPLY_ORDER_IN_REVERSE
            BooleanSetting.ApplySelectedFirst -> AppDataStore.APPLY_SELECTED_FIRST
            BooleanSetting.ApplyShowSystemApp -> AppDataStore.APPLY_SHOW_SYSTEM_APP
            BooleanSetting.ApplyShowPackageName -> AppDataStore.APPLY_SHOW_PACKAGE_NAME
            BooleanSetting.DialogVersionCompareSingleLine -> AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE
            BooleanSetting.DialogSdkCompareMultiLine -> AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE
            BooleanSetting.DialogShowExtendedMenu -> AppDataStore.DIALOG_SHOW_EXTENDED_MENU
            BooleanSetting.DialogShowIntelligentSuggestion -> AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION
            BooleanSetting.DialogDisableNotificationOnDismiss -> AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS
            BooleanSetting.DialogShowOppoSpecial -> AppDataStore.DIALOG_SHOW_OPPO_SPECIAL
            BooleanSetting.DialogAutoSilentInstall -> AppDataStore.DIALOG_AUTO_SILENT_INSTALL
            BooleanSetting.LabEnableModuleFlash -> AppDataStore.LAB_ENABLE_MODULE_FLASH
            BooleanSetting.LabModuleFlashShowArt -> AppDataStore.LAB_MODULE_FLASH_SHOW_ART
            BooleanSetting.LabModuleAlwaysRoot -> AppDataStore.LAB_MODULE_ALWAYS_ROOT
            BooleanSetting.LabHttpSaveFile -> AppDataStore.LAB_HTTP_SAVE_FILE
            BooleanSetting.LabSetInstallRequester -> AppDataStore.LAB_SET_INSTALL_REQUESTER
            BooleanSetting.LabTapIconToShare -> AppDataStore.LAB_TAP_ICON_TO_SHARE
            BooleanSetting.EnableFileLogging -> AppDataStore.ENABLE_FILE_LOGGING
        }

    private fun namedPackageListKey(setting: NamedPackageListSetting): Preferences.Key<String> =
        when (setting) {
            NamedPackageListSetting.ManagedInstallerPackages -> AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST
            NamedPackageListSetting.ManagedBlacklistPackages -> AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST
            NamedPackageListSetting.ManagedSharedUserIdExemptedPackages -> AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST
        }

    private fun sharedUidListKey(setting: SharedUidListSetting): Preferences.Key<String> =
        when (setting) {
            SharedUidListSetting.ManagedSharedUserIdBlacklist -> AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST
        }
}
