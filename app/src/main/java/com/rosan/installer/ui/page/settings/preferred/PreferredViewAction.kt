package com.rosan.installer.ui.page.settings.preferred

import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

sealed class PreferredViewAction {
    data object Init : PreferredViewAction()

    data class ChangeGlobalAuthorizer(val authorizer: ConfigEntity.Authorizer) : PreferredViewAction()
    data class ChangeGlobalCustomizeAuthorizer(val customizeAuthorizer: String) : PreferredViewAction()
    data class ChangeGlobalInstallMode(val installMode: ConfigEntity.InstallMode) : PreferredViewAction()
    data class ChangeShowDialogInstallExtendedMenu(val showMenu: Boolean) : PreferredViewAction()
    data class ChangeShowSuggestion(val showIntelligentSuggestion: Boolean) : PreferredViewAction()
    data class ChangeShowDisableNotification(val showDisableNotification: Boolean) : PreferredViewAction()
    data class ChangeShowDialogWhenPressingNotification(val showDialog: Boolean) : PreferredViewAction()
    data class ChangeDhizukuAutoCloseCountDown(val countDown: Int) : PreferredViewAction()
    data class ChangeShowRefreshedUI(val showRefreshedUI: Boolean) : PreferredViewAction()
    data class ChangeVersionCompareInSingleLine(val versionCompareInSingleLine: Boolean) : PreferredViewAction()

    data class AddManagedInstallerPackage(val pkg: NamedPackage) : PreferredViewAction()
    data class RemoveManagedInstallerPackage(val pkg: NamedPackage) : PreferredViewAction()
    data class AddManagedBlacklistPackage(val pkg: NamedPackage) : PreferredViewAction()
    data class RemoveManagedBlacklistPackage(val pkg: NamedPackage) : PreferredViewAction()

    data class AddManagedSharedUserIdBlacklist(val uid: SharedUid) : PreferredViewAction()
    data class RemoveManagedSharedUserIdBlacklist(val uid: SharedUid) : PreferredViewAction()
    data class AddManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : PreferredViewAction()
    data class RemoveManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : PreferredViewAction()

    data class SetAdbVerifyEnabledState(val enabled: Boolean) : PreferredViewAction()
    data object RequestIgnoreBatteryOptimization : PreferredViewAction()
    data object RefreshIgnoreBatteryOptimizationStatus : PreferredViewAction()
    data class SetDefaultInstaller(val lock: Boolean) : PreferredViewAction()
}