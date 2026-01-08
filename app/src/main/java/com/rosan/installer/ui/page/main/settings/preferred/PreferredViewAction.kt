package com.rosan.installer.ui.page.main.settings.preferred

import androidx.compose.ui.graphics.Color
import com.rosan.installer.data.app.model.enums.HttpProfile
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.ThemeMode

sealed class PreferredViewAction {
    data object Init : PreferredViewAction()
    data object Update : PreferredViewAction()

    data class ChangeGlobalAuthorizer(val authorizer: ConfigEntity.Authorizer) : PreferredViewAction()
    data class ChangeGlobalCustomizeAuthorizer(val customizeAuthorizer: String) : PreferredViewAction()
    data class ChangeGlobalInstallMode(val installMode: ConfigEntity.InstallMode) : PreferredViewAction()
    data class ChangeShowDialogInstallExtendedMenu(val showMenu: Boolean) : PreferredViewAction()
    data class ChangeShowSuggestion(val showSuggestion: Boolean) : PreferredViewAction()
    data class ChangeShowDisableNotification(val showDisableNotification: Boolean) : PreferredViewAction()
    data class ChangeShowDialogWhenPressingNotification(val showDialog: Boolean) : PreferredViewAction()
    data class ChangeDhizukuAutoCloseCountDown(val countDown: Int) : PreferredViewAction()
    data class ChangeNotificationSuccessAutoClearSeconds(val seconds: Int) : PreferredViewAction()
    data class ChangeShowExpressiveUI(val showRefreshedUI: Boolean) : PreferredViewAction()
    data class ChangeShowLiveActivity(val showLiveActivity: Boolean) : PreferredViewAction()
    data class ChangeBiometricAuth(val biometricAuth: Boolean, val isInstaller: Boolean) : PreferredViewAction()
    data class ChangeUseMiuix(val useMiuix: Boolean) : PreferredViewAction()
    data class ChangePreferSystemIcon(val preferSystemIcon: Boolean) : PreferredViewAction()
    data class ChangeShowLauncherIcon(val showLauncherIcon: Boolean) : PreferredViewAction()
    data class ChangeVersionCompareInSingleLine(val versionCompareInSingleLine: Boolean) : PreferredViewAction()
    data class ChangeSdkCompareInMultiLine(val sdkCompareInMultiLine: Boolean) : PreferredViewAction()
    data class ChangeShowOPPOSpecial(val showOPPOSpecial: Boolean) : PreferredViewAction()
    data class ChangeAutoLockInstaller(val autoLockInstaller: Boolean) : PreferredViewAction()
    data class ChangeAutoSilentInstall(val autoSilentInstall: Boolean) : PreferredViewAction()

    data class AddManagedInstallerPackage(val pkg: NamedPackage) : PreferredViewAction()
    data class RemoveManagedInstallerPackage(val pkg: NamedPackage) : PreferredViewAction()
    data class AddManagedBlacklistPackage(val pkg: NamedPackage) : PreferredViewAction()
    data class RemoveManagedBlacklistPackage(val pkg: NamedPackage) : PreferredViewAction()

    data class AddManagedSharedUserIdBlacklist(val uid: SharedUid) : PreferredViewAction()
    data class RemoveManagedSharedUserIdBlacklist(val uid: SharedUid) : PreferredViewAction()
    data class AddManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : PreferredViewAction()
    data class RemoveManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : PreferredViewAction()

    data class ToggleGlobalUninstallFlag(val flag: Int, val enable: Boolean) : PreferredViewAction()

    data class SetAdbVerifyEnabledState(val enabled: Boolean) : PreferredViewAction()
    data object RequestIgnoreBatteryOptimization : PreferredViewAction()
    data object RefreshIgnoreBatteryOptimizationStatus : PreferredViewAction()
    data class SetDefaultInstaller(val lock: Boolean) : PreferredViewAction()

    data class LabChangeRootModuleFlash(val enable: Boolean) : PreferredViewAction()
    data class LabChangeRootShowModuleArt(val enable: Boolean) : PreferredViewAction()
    data class LabChangeRootModuleAlwaysUseRoot(val enable: Boolean) : PreferredViewAction()
    data class LabChangeRootImplementation(val implementation: RootImplementation) : PreferredViewAction()
    data class LabChangeHttpProfile(val profile: HttpProfile) : PreferredViewAction()
    data class LabChangeHttpSaveFile(val enable: Boolean) : PreferredViewAction()
    data class LabChangeSetInstallRequester(val enable: Boolean) : PreferredViewAction()
    data class SetThemeMode(val mode: ThemeMode) : PreferredViewAction()
    data class SetPaletteStyle(val style: PaletteStyle) : PreferredViewAction()
    data class SetUseDynamicColor(val use: Boolean) : PreferredViewAction()
    data class SetUseMiuixMonet(val use: Boolean) : PreferredViewAction()
    data class SetSeedColor(val color: Color) : PreferredViewAction()
    data class SetDynColorFollowPkgIcon(val follow: Boolean) : PreferredViewAction()
    data class SetDynColorFollowPkgIconForLiveActivity(val follow: Boolean) : PreferredViewAction()
}