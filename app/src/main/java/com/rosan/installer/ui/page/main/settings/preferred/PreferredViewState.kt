package com.rosan.installer.ui.page.main.settings.preferred

import androidx.compose.ui.graphics.Color
import com.rosan.installer.data.app.model.entity.RootImplementation
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
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
    val versionCompareInSingleLine: Boolean = false,
    val sdkCompareInMultiLine: Boolean = false,
    val showOPPOSpecial: Boolean = false,
    val showExpressiveUI: Boolean = true,
    val showLiveActivity: Boolean = false,
    val autoLockInstaller: Boolean = false,
    val autoSilentInstall: Boolean = false,
    val showMiuixUI: Boolean = false,
    val preferSystemIcon: Boolean = false,
    val showLauncherIcon: Boolean = true,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val managedBlacklistPackages: List<NamedPackage> = emptyList(),
    val managedSharedUserIdBlacklist: List<SharedUid> = emptyList(),
    val managedSharedUserIdExemptedPackages: List<NamedPackage> = emptyList(),
    val labShizukuHookMode: Boolean = false,
    val labRootEnableModuleFlash: Boolean = false,
    val labRootImplementation: RootImplementation = RootImplementation.Magisk,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val useDynamicColor: Boolean = true,
    val seedColor: Color = PresetColors.first().color,
    val useDynColorFollowPkgIcon: Boolean = false
) {
    val authorizerCustomize = authorizer == ConfigEntity.Authorizer.Customize

    sealed class Progress {
        data object Loading : Progress()
        data object Loaded : Progress()
    }
}