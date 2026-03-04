package com.rosan.installer.data.settings.model.datastore.repo

import androidx.datastore.preferences.core.Preferences
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.repo.AppSettingsRepo
import com.rosan.installer.data.settings.repo.BooleanSetting
import com.rosan.installer.data.settings.repo.IntSetting
import com.rosan.installer.data.settings.repo.NamedPackageListSetting
import com.rosan.installer.data.settings.repo.SharedUidListSetting
import com.rosan.installer.data.settings.repo.StringSetting
import kotlinx.coroutines.flow.Flow

class AppSettingsRepoImpl(
    private val appDataStore: AppDataStore
) : AppSettingsRepo {
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
