package com.rosan.installer.ui.page.main.settings.preferred

import androidx.compose.ui.graphics.Color
import com.rosan.installer.data.app.model.enums.HttpProfile
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.RawColor
import com.rosan.installer.ui.theme.m3color.ThemeMode

data class PreferredViewState(
    val progress: Progress = Progress.Loading,
    val authorizer: ConfigEntity.Authorizer = ConfigEntity.Authorizer.Shizuku,
    val customizeAuthorizer: String = "",
    val installMode: ConfigEntity.InstallMode = ConfigEntity.InstallMode.Dialog,
    val adbVerifyEnabled: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val showDialogInstallExtendedMenu: Boolean = false,
    val showSmartSuggestion: Boolean = false,
    val disableNotificationForDialogInstall: Boolean = false,
    val showDialogWhenPressingNotification: Boolean = true,
    val dhizukuAutoCloseCountDown: Int = 3,
    val notificationSuccessAutoClearSeconds: Int = 0,
    val versionCompareInSingleLine: Boolean = false,
    val sdkCompareInMultiLine: Boolean = false,
    val showOPPOSpecial: Boolean = false,
    val showExpressiveUI: Boolean = true,
    val showLiveActivity: Boolean = false,
    val installerRequireBiometricAuth: Boolean = false,
    val uninstallerRequireBiometricAuth: Boolean = false,
    val autoLockInstaller: Boolean = false,
    val autoSilentInstall: Boolean = false,
    val showMiuixUI: Boolean = false,
    val preferSystemIcon: Boolean = false,
    val showLauncherIcon: Boolean = true,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val managedBlacklistPackages: List<NamedPackage> = emptyList(),
    val managedSharedUserIdBlacklist: List<SharedUid> = emptyList(),
    val managedSharedUserIdExemptedPackages: List<NamedPackage> = emptyList(),
    val labRootEnableModuleFlash: Boolean = false,
    val labRootImplementation: RootImplementation = RootImplementation.Magisk,
    val labRootShowModuleArt: Boolean = true,
    val labRootModuleAlwaysUseRoot: Boolean = false,
    val labHttpSaveFile: Boolean = false,
    val labHttpProfile: HttpProfile = HttpProfile.ALLOW_SECURE,
    val labSetInstallRequester: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val seedColor: Color = PresetColors.first().color,
    val availableColors: List<RawColor> = PresetColors,
    val useDynColorFollowPkgIcon: Boolean = false,
    val useDynColorFollowPkgIconForLiveActivity: Boolean = false,
    val hasUpdate: Boolean = false,
    val remoteVersion: String = "",
    val uninstallFlags: Int = 0
) {
    val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

    sealed class Progress {
        data object Loading : Progress()
        data object Loaded : Progress()
    }
}