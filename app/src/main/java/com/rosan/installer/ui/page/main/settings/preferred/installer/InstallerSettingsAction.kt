// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid

sealed class InstallerSettingsAction {
    data class ChangeGlobalAuthorizer(val authorizer: Authorizer) : InstallerSettingsAction()
    data class ChangeDhizukuAutoCloseCountDown(val countDown: Int) : InstallerSettingsAction()
    data class ChangeAlwaysUseRootInSystem(val alwaysUseRootInSystem: Boolean) : InstallerSettingsAction()
    data class ChangeGlobalInstallMode(val installMode: InstallMode) : InstallerSettingsAction()
    data class ChangeShowLiveActivity(val showLiveActivity: Boolean) : InstallerSettingsAction()
    data class ChangeBiometricAuth(val require: Boolean) : InstallerSettingsAction()
    data class ChangeNotificationSuccessAutoClearSeconds(val seconds: Int) : InstallerSettingsAction()
    data class ChangeVersionCompareInSingleLine(val compareInSingleLine: Boolean) : InstallerSettingsAction()
    data class ChangeSdkCompareInMultiLine(val compareInMultiLine: Boolean) : InstallerSettingsAction()
    data class ChangeShowDialogInstallExtendedMenu(val showMenu: Boolean) : InstallerSettingsAction()
    data class ChangeShowSuggestion(val showSuggestion: Boolean) : InstallerSettingsAction()
    data class ChangeShowDialogWhenPressingNotification(val showDialog: Boolean) : InstallerSettingsAction()
    data class ChangeAutoSilentInstall(val autoSilentInstall: Boolean) : InstallerSettingsAction()
    data class ChangeShowDisableNotification(val disable: Boolean) : InstallerSettingsAction()
    data class ChangeShowOPPOSpecial(val show: Boolean) : InstallerSettingsAction()

    // --- Collection Management ---
    data class AddManagedInstallerPackage(val pkg: NamedPackage) : InstallerSettingsAction()
    data class RemoveManagedInstallerPackage(val pkg: NamedPackage) : InstallerSettingsAction()
    data class AddManagedBlacklistPackage(val pkg: NamedPackage) : InstallerSettingsAction()
    data class RemoveManagedBlacklistPackage(val pkg: NamedPackage) : InstallerSettingsAction()
    data class AddManagedSharedUserIdBlacklist(val uid: SharedUid) : InstallerSettingsAction()
    data class RemoveManagedSharedUserIdBlacklist(val uid: SharedUid) : InstallerSettingsAction()
    data class AddManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : InstallerSettingsAction()
    data class RemoveManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : InstallerSettingsAction()
}
