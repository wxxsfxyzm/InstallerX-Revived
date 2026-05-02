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
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.domain.settings.model.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.PredictiveBackAnimation
import com.rosan.installer.domain.settings.model.PredictiveBackExitDirection
import com.rosan.installer.domain.settings.model.RootMode
import com.rosan.installer.domain.settings.model.SharedUid
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
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

class AppSettingsRepositoryImpl(
    private val appDataStore: AppDataStore,
    capabilityProvider: DeviceCapabilityProvider,
    appScope: CoroutineScope
) : AppSettingsRepository {
    override val preferencesFlow: Flow<AppPreferences> = combine(
        listOf(
            appDataStore.getString(
                AppDataStore.AUTHORIZER,
                if (capabilityProvider.isSystemApp) Authorizer.None.value else Authorizer.Shizuku.value
            ),
            appDataStore.getBoolean(AppDataStore.ALWAYS_USE_ROOT_IN_SYSTEM, false),
            appDataStore.getString(AppDataStore.CUSTOMIZE_AUTHORIZER, ""),
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
            appDataStore.getString(AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH, BiometricAuthMode.Disable.value),
            appDataStore.getBoolean(AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH, false),
            appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false),
            appDataStore.getBoolean(AppDataStore.SHOW_MI_ISLAND, false),
            appDataStore.getBoolean(AppDataStore.SHOW_MI_ISLAND_BYPASS_RESTRICTION, false),
            appDataStore.getBoolean(AppDataStore.SHOW_MI_ISLAND_OUTER_GLOW, true),
            appDataStore.getInt(AppDataStore.SHOW_MI_ISLAND_BLOCKING_INTERVAL_MS, 100),
            appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER, false),
            appDataStore.getBoolean(AppDataStore.DIALOG_AUTO_SILENT_INSTALL, false),
            appDataStore.getBoolean(AppDataStore.UI_USE_MIUIX, false),
            appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false),
            appDataStore.getBoolean(AppDataStore.SHOW_LAUNCHER_ICON, true),

            // Lists
            appDataStore.getNamedPackageList(
                AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST,
                AppDataStore.DEFAULT_MANAGED_INSTALLER_PACKAGES
            ),
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST),
            appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST),
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST),

            appDataStore.getInt(AppDataStore.UNINSTALL_FLAGS, 0),

            // Lab settings
            appDataStore.getString(AppDataStore.GITHUB_UPDATE_CHANNEL, GithubUpdateChannel.OFFICIAL.name),
            appDataStore.getString(AppDataStore.CUSTOM_GITHUB_PROXY_URL, ""),
            appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false),
            appDataStore.getBoolean(AppDataStore.LAB_MODULE_FLASH_SHOW_ART, true),
            appDataStore.getString(AppDataStore.LAB_ROOT_IMPLEMENTATION, "Default"),
            appDataStore.getString(AppDataStore.LAB_HTTP_PROFILE, "Default"),
            appDataStore.getBoolean(AppDataStore.LAB_HTTP_SAVE_FILE, false),
            appDataStore.getBoolean(AppDataStore.LAB_SET_INSTALL_REQUESTER, false),
            appDataStore.getBoolean(AppDataStore.LAB_TAP_ICON_TO_SHARE, false),
            appDataStore.getBoolean(AppDataStore.LAB_SHOW_FILE_PATH, false),
            appDataStore.getBoolean(AppDataStore.LAB_SHOW_INSTALL_INITIATOR, false),
            appDataStore.getBoolean(AppDataStore.LAB_INSTALL_WITHOUT_USER_ACTION, false),
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
            appDataStore.getBoolean(AppDataStore.UI_USE_BLUR, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
            appDataStore.getString(AppDataStore.PREDICTIVE_BACK_ANIMATION, PredictiveBackAnimation.MIUIX.value),
            appDataStore.getString(AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION, PredictiveBackExitDirection.ALWAYS_RIGHT.value)
        )
    ) { values: Array<Any?> ->
        var idx = 0

        @Suppress("UNCHECKED_CAST")
        // Pre-compute values for github update channel validation
        val authorizer = Authorizer.fromValueOrDefault(values[idx++] as String)
        val alwaysUseRootInSystem = values[idx++] as Boolean
        val customizeAuthorizer = values[idx++] as String
        val showDialogInstallExtendedMenu = values[idx++] as Boolean
        val showSmartSuggestion = values[idx++] as Boolean
        val disableNotificationForDialogInstall = values[idx++] as Boolean
        val showDialogWhenPressingNotification = values[idx++] as Boolean
        val dhizukuAutoCloseCountDown = values[idx++] as Int
        val notificationSuccessAutoClearSeconds = values[idx++] as Int
        val versionCompareInSingleLine = values[idx++] as Boolean
        val sdkCompareInMultiLine = values[idx++] as Boolean
        val showOPPOSpecial = values[idx++] as Boolean
        val showExpressiveUI = values[idx++] as Boolean
        val installerRequireBiometricAuth = BiometricAuthMode.fromValueOrDefault(values[idx++] as String)
        val uninstallerRequireBiometricAuth = values[idx++] as Boolean
        val showLiveActivity = values[idx++] as Boolean
        val useMiIsland = values[idx++] as Boolean
        val useMiIslandBypassRestriction = values[idx++] as Boolean
        val useMiIslandOuterGlow = values[idx++] as Boolean
        val useMiIslandBlockingIntervalMs = values[idx++] as Int
        val autoLockInstaller = values[idx++] as Boolean
        val autoSilentInstall = values[idx++] as Boolean
        val showMiuixUI = values[idx++] as Boolean
        val preferSystemIcon = values[idx++] as Boolean
        val showLauncherIcon = values[idx++] as Boolean

        val managedInstallerPackages = values[idx++] as List<NamedPackage>
        val managedBlacklistPackages = values[idx++] as List<NamedPackage>
        val managedSharedUserIdBlacklist = values[idx++] as List<SharedUid>
        val managedSharedUserIdExemptedPackages = values[idx++] as List<NamedPackage>
        // Uninstaller
        val uninstallFlags = values[idx++] as Int
        // Updater
        val rawGithubUpdateChannel = GithubUpdateChannel.fromValueOrDefault(values[idx++] as String)
        val customGithubProxyUrl = values[idx++] as String
        val githubUpdateChannel = if (rawGithubUpdateChannel == GithubUpdateChannel.CUSTOM && customGithubProxyUrl.isBlank()) GithubUpdateChannel.OFFICIAL else rawGithubUpdateChannel
        // Lab
        val labRootEnableModuleFlash = values[idx++] as Boolean
        val labRootShowModuleArt = values[idx++] as Boolean
        val labRootMode = RootMode.fromString(values[idx++] as String)
        val labHttpProfile = HttpProfile.fromString(values[idx++] as String)
        val labHttpSaveFile = values[idx++] as Boolean
        val labSetInstallRequester = values[idx++] as Boolean
        val labTapIconToShare = values[idx++] as Boolean
        val labShowFilePath = values[idx++] as Boolean
        val labShowInstallInitiator = values[idx++] as Boolean
        val labInstallWithoutUserAction = values[idx++] as Boolean
        val enableFileLogging = values[idx++] as Boolean
        // UI State
        val themeMode = ThemeMode.fromValueOrDefault(values[idx++] as String)
        val paletteStyle = PaletteStyle.fromValueOrDefault(values[idx++] as String)
        val colorSpec = ThemeColorSpec.fromValueOrDefault(values[idx++] as String)
        val useDynamicColor = values[idx++] as Boolean
        val useMiuixMonet = values[idx++] as Boolean
        val useAppleFloatingBar = values[idx++] as Boolean
        val seedColorInt = values[idx++] as Int
        val useDynColorFollowPkgIcon = values[idx++] as Boolean
        val useDynColorFollowPkgIconForLiveActivity = values[idx++] as Boolean
        val useBlur = values[idx++] as Boolean
        val predictiveBackAnimation = PredictiveBackAnimation.fromValueOrDefault(values[idx++] as String)
        val predictiveBackExitDirection = PredictiveBackExitDirection.fromValueOrDefault(values[idx++] as String)

        AppPreferences(
            authorizer = authorizer,
            alwaysUseRootInSystem = alwaysUseRootInSystem,
            customizeAuthorizer = customizeAuthorizer,
            showDialogInstallExtendedMenu = showDialogInstallExtendedMenu,
            showSmartSuggestion = showSmartSuggestion,
            disableNotificationForDialogInstall = disableNotificationForDialogInstall,
            showDialogWhenPressingNotification = showDialogWhenPressingNotification,
            dhizukuAutoCloseCountDown = dhizukuAutoCloseCountDown,
            notificationSuccessAutoClearSeconds = notificationSuccessAutoClearSeconds,
            versionCompareInSingleLine = versionCompareInSingleLine,
            sdkCompareInMultiLine = sdkCompareInMultiLine,
            showOPPOSpecial = showOPPOSpecial,
            showExpressiveUI = showExpressiveUI,
            installerRequireBiometricAuth = installerRequireBiometricAuth,
            uninstallerRequireBiometricAuth = uninstallerRequireBiometricAuth,
            showLiveActivity = showLiveActivity,
            useMiIsland = useMiIsland,
            useMiIslandBypassRestriction = useMiIslandBypassRestriction,
            useMiIslandOuterGlow = useMiIslandOuterGlow,
            useMiIslandBlockingIntervalMs = useMiIslandBlockingIntervalMs,
            autoLockInstaller = autoLockInstaller,
            autoSilentInstall = autoSilentInstall,
            showMiuixUI = showMiuixUI,
            preferSystemIcon = preferSystemIcon,
            showLauncherIcon = showLauncherIcon,

            managedInstallerPackages = managedInstallerPackages,
            managedBlacklistPackages = managedBlacklistPackages,
            managedSharedUserIdBlacklist = managedSharedUserIdBlacklist,
            managedSharedUserIdExemptedPackages = managedSharedUserIdExemptedPackages,
            // Uninstaller
            uninstallFlags = uninstallFlags,
            // Updater
            githubUpdateChannel = githubUpdateChannel,
            customGithubProxyUrl = customGithubProxyUrl,
            // Lab
            labRootEnableModuleFlash = labRootEnableModuleFlash,
            labRootShowModuleArt = labRootShowModuleArt,
            labRootMode = labRootMode,
            labHttpProfile = labHttpProfile,
            labHttpSaveFile = labHttpSaveFile,
            labSetInstallRequester = labSetInstallRequester,
            labTapIconToShare = labTapIconToShare,
            labShowFilePath = labShowFilePath,
            labShowInstallInitiator = labShowInstallInitiator,
            labInstallWithoutUserAction = labInstallWithoutUserAction,
            enableFileLogging = enableFileLogging,
            // UI State
            themeMode = themeMode,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            useDynamicColor = useDynamicColor,
            useMiuixMonet = useMiuixMonet,
            useAppleFloatingBar = useAppleFloatingBar,
            seedColorInt = seedColorInt,
            useDynColorFollowPkgIcon = useDynColorFollowPkgIcon,
            useDynColorFollowPkgIconForLiveActivity = useDynColorFollowPkgIconForLiveActivity,
            useBlur = useBlur,
            predictiveBackAnimation = predictiveBackAnimation,
            predictiveBackExitDirection = predictiveBackExitDirection,
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
    ): Flow<List<NamedPackage>> {
        val finalDefault = if (setting == NamedPackageListSetting.ManagedInstallerPackages && default.isEmpty()) {
            AppDataStore.DEFAULT_MANAGED_INSTALLER_PACKAGES
        } else default
        return appDataStore.getNamedPackageList(namedPackageListKey(setting), finalDefault)
    }

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
            StringSetting.ApplyOrderType -> AppDataStore.APPLY_ORDER_TYPE
            StringSetting.LabRootImplementation -> AppDataStore.LAB_ROOT_IMPLEMENTATION
            StringSetting.LabHttpProfile -> AppDataStore.LAB_HTTP_PROFILE
            StringSetting.PredictiveBackAnimation -> AppDataStore.PREDICTIVE_BACK_ANIMATION
            StringSetting.PredictiveBackExitDirection -> AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION
            StringSetting.GithubUpdateChannel -> AppDataStore.GITHUB_UPDATE_CHANNEL
            StringSetting.CustomGithubProxyUrl -> AppDataStore.CUSTOM_GITHUB_PROXY_URL
            StringSetting.InstallerBiometricAuthMode -> AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH
        }

    private fun intKey(setting: IntSetting): Preferences.Key<Int> =
        when (setting) {
            IntSetting.ThemeSeedColor -> AppDataStore.THEME_SEED_COLOR
            IntSetting.ShowMiIslandBlockingInterval -> AppDataStore.SHOW_MI_ISLAND_BLOCKING_INTERVAL_MS
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
            BooleanSetting.ShowMiIslandBypassRestriction -> AppDataStore.SHOW_MI_ISLAND_BYPASS_RESTRICTION
            BooleanSetting.ShowMiIslandOuterGlow -> AppDataStore.SHOW_MI_ISLAND_OUTER_GLOW
            BooleanSetting.AlwaysUseRootInSystem -> AppDataStore.ALWAYS_USE_ROOT_IN_SYSTEM
            BooleanSetting.UninstallerRequireBiometricAuth -> AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH
            BooleanSetting.ShowLauncherIcon -> AppDataStore.SHOW_LAUNCHER_ICON
            BooleanSetting.PreferSystemIconForInstall -> AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL
            BooleanSetting.ShowDialogWhenPressingNotification -> AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION
            BooleanSetting.AutoLockInstaller -> AppDataStore.AUTO_LOCK_INSTALLER
            BooleanSetting.AuxiliaryInstallAutoConfirmUsb -> AppDataStore.AUXILIARY_INSTALL_AUTO_CONFIRM_USB
            BooleanSetting.AuxiliaryInstallShowToast -> AppDataStore.AUXILIARY_INSTALL_SHOW_TOAST
            BooleanSetting.AuxiliaryInstallDelayedRetry -> AppDataStore.AUXILIARY_INSTALL_DELAYED_RETRY
            BooleanSetting.AuxiliaryInstallRequireScreenOn -> AppDataStore.AUXILIARY_INSTALL_REQUIRE_SCREEN_ON
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
            BooleanSetting.LabHttpSaveFile -> AppDataStore.LAB_HTTP_SAVE_FILE
            BooleanSetting.LabSetInstallRequester -> AppDataStore.LAB_SET_INSTALL_REQUESTER
            BooleanSetting.LabTapIconToShare -> AppDataStore.LAB_TAP_ICON_TO_SHARE
            BooleanSetting.LabShowFilePath -> AppDataStore.LAB_SHOW_FILE_PATH
            BooleanSetting.LabShowInstallInitiator -> AppDataStore.LAB_SHOW_INSTALL_INITIATOR
            BooleanSetting.LabInstallWithoutUserAction -> AppDataStore.LAB_INSTALL_WITHOUT_USER_ACTION
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
