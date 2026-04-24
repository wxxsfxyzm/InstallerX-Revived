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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class AppSettingsRepositoryImpl(
    private val appDataStore: AppDataStore,
    capabilityProvider: DeviceCapabilityProvider,
    appScope: CoroutineScope
) : AppSettingsRepository {

    // Replaced brittle combine(listOf(...)) with a safe mapping from the raw Preferences flow.
    override val preferencesFlow: Flow<AppPreferences> = appDataStore.data.map { prefs ->
        // Pre-compute values for Github update channel validation
        val rawGithubUpdateChannel = GithubUpdateChannel.fromValueOrDefault(
            prefs[AppDataStore.GITHUB_UPDATE_CHANNEL] ?: GithubUpdateChannel.OFFICIAL.name
        )
        val customGithubProxyUrl = prefs[AppDataStore.CUSTOM_GITHUB_PROXY_URL] ?: ""
        val githubUpdateChannel = if (rawGithubUpdateChannel == GithubUpdateChannel.CUSTOM && customGithubProxyUrl.isBlank()) {
            GithubUpdateChannel.OFFICIAL
        } else {
            rawGithubUpdateChannel
        }

        // Map all preferences explicitly by key. Order no longer matters.
        AppPreferences(
            authorizer = Authorizer.fromValueOrDefault(
                prefs[AppDataStore.AUTHORIZER] ?: if (capabilityProvider.isSystemApp) Authorizer.None.value else Authorizer.Shizuku.value
            ),
            alwaysUseRootInSystem = prefs[AppDataStore.ALWAYS_USE_ROOT_IN_SYSTEM] ?: false,
            customizeAuthorizer = prefs[AppDataStore.CUSTOMIZE_AUTHORIZER] ?: "",
            showDialogInstallExtendedMenu = prefs[AppDataStore.DIALOG_SHOW_EXTENDED_MENU] ?: false,
            showSmartSuggestion = prefs[AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION] ?: true,
            disableNotificationForDialogInstall = prefs[AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS] ?: false,
            showDialogWhenPressingNotification = prefs[AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION] ?: true,
            dhizukuAutoCloseCountDown = prefs[AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN] ?: 3,
            notificationSuccessAutoClearSeconds = prefs[AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS] ?: 0,
            versionCompareInSingleLine = prefs[AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE] ?: false,
            sdkCompareInMultiLine = prefs[AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE] ?: false,
            showOPPOSpecial = prefs[AppDataStore.DIALOG_SHOW_OPPO_SPECIAL] ?: false,
            installerRequireBiometricAuth = BiometricAuthMode.fromValueOrDefault(
                prefs[AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH] ?: BiometricAuthMode.Disable.value
            ),
            uninstallerRequireBiometricAuth = prefs[AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH] ?: false,
            showLiveActivity = prefs[AppDataStore.SHOW_LIVE_ACTIVITY] ?: false,
            useMiIsland = prefs[AppDataStore.SHOW_MI_ISLAND] ?: false,
            useMiIslandBypassRestriction = prefs[AppDataStore.SHOW_MI_ISLAND_BYPASS_RESTRICTION] ?: false,
            useMiIslandOuterGlow = prefs[AppDataStore.SHOW_MI_ISLAND_OUTER_GLOW] ?: true,
            useMiIslandBlockingIntervalMs = prefs[AppDataStore.SHOW_MI_ISLAND_BLOCKING_INTERVAL_MS] ?: 100,
            autoLockInstaller = prefs[AppDataStore.AUTO_LOCK_INSTALLER] ?: false,
            autoSilentInstall = prefs[AppDataStore.DIALOG_AUTO_SILENT_INSTALL] ?: false,
            showMiuixUI = prefs[AppDataStore.UI_USE_MIUIX] ?: false,
            preferSystemIcon = prefs[AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL] ?: false,
            showLauncherIcon = prefs[AppDataStore.SHOW_LAUNCHER_ICON] ?: true,
            userSetLSPosedActive = prefs[AppDataStore.USER_SET_LSPOSED_ACTIVE] ?: false,

            // Lists require synchronous parsing functions from AppDataStore
            managedInstallerPackages = appDataStore.parseNamedPackageList(
                prefs,
                AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST,
                AppDataStore.DEFAULT_MANAGED_INSTALLER_PACKAGES
            ),
            managedBlacklistPackages = appDataStore.parseNamedPackageList(
                prefs,
                AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST,
                emptyList()
            ),
            managedSharedUserIdBlacklist = appDataStore.parseSharedUidList(
                prefs,
                AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST,
                emptyList()
            ),
            managedSharedUserIdExemptedPackages = appDataStore.parseNamedPackageList(
                prefs,
                AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST,
                emptyList()
            ),
            // Uninstaller
            uninstallFlags = prefs[AppDataStore.UNINSTALL_FLAGS] ?: 0,
            // Updater
            githubUpdateChannel = githubUpdateChannel,
            customGithubProxyUrl = customGithubProxyUrl,
            // Lab
            labRootEnableModuleFlash = prefs[AppDataStore.LAB_ENABLE_MODULE_FLASH] ?: false,
            labRootShowModuleArt = prefs[AppDataStore.LAB_MODULE_FLASH_SHOW_ART] ?: true,
            labRootMode = RootMode.fromString(prefs[AppDataStore.LAB_ROOT_IMPLEMENTATION] ?: "Default"),
            labHttpProfile = HttpProfile.fromString(prefs[AppDataStore.LAB_HTTP_PROFILE] ?: "Default"),
            labHttpSaveFile = prefs[AppDataStore.LAB_HTTP_SAVE_FILE] ?: false,
            labSetInstallRequester = prefs[AppDataStore.LAB_SET_INSTALL_REQUESTER] ?: false,
            labTapIconToShare = prefs[AppDataStore.LAB_TAP_ICON_TO_SHARE] ?: false,
            labShowFilePath = prefs[AppDataStore.LAB_SHOW_FILE_PATH] ?: false,
            labShowInstallInitiator = prefs[AppDataStore.LAB_SHOW_INSTALL_INITIATOR] ?: false,
            labInstallWithoutUserAction = prefs[AppDataStore.LAB_INSTALL_WITHOUT_USER_ACTION] ?: false,
            enableFileLogging = prefs[AppDataStore.ENABLE_FILE_LOGGING] ?: true,
            // UI State
            themeMode = ThemeMode.fromValueOrDefault(prefs[AppDataStore.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            paletteStyle = PaletteStyle.fromValueOrDefault(prefs[AppDataStore.THEME_PALETTE_STYLE] ?: PaletteStyle.TonalSpot.name),
            colorSpec = ThemeColorSpec.fromValueOrDefault(prefs[AppDataStore.THEME_COLOR_SPEC] ?: ThemeColorSpec.SPEC_2025.name),
            useDynamicColor = prefs[AppDataStore.THEME_USE_DYNAMIC_COLOR] ?: true,
            useMiuixMonet = prefs[AppDataStore.UI_USE_MIUIX_MONET] ?: false,
            useAppleFloatingBar = prefs[AppDataStore.UI_USE_APPLE_FLOATING_BAR] ?: false,
            seedColorInt = prefs[AppDataStore.THEME_SEED_COLOR] ?: PresetColors.first().color.toArgb(),
            useDynColorFollowPkgIcon = prefs[AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON] ?: false,
            useDynColorFollowPkgIconForLiveActivity = prefs[AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON] ?: false,
            useBlur = prefs[AppDataStore.UI_USE_BLUR] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
            predictiveBackAnimation = PredictiveBackAnimation.fromValueOrDefault(
                prefs[AppDataStore.PREDICTIVE_BACK_ANIMATION] ?: PredictiveBackAnimation.MIUIX.value
            ),
            predictiveBackExitDirection = PredictiveBackExitDirection.fromValueOrDefault(
                prefs[AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION] ?: PredictiveBackExitDirection.ALWAYS_RIGHT.value
            )
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
            BooleanSetting.UserSetLSPosedActive -> AppDataStore.USER_SET_LSPOSED_ACTIVE
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
