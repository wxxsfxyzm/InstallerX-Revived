// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid

sealed interface InstallerSettingsAction {
    data class ChangeGlobalAuthorizer(val authorizer: Authorizer) : InstallerSettingsAction
    data class ChangeDhizukuAutoCloseCountDown(val countDown: Int) : InstallerSettingsAction
    data class ChangeAlwaysUseRootInSystem(val alwaysUseRootInSystem: Boolean) : InstallerSettingsAction
    data class ChangeBiometricAuth(val mode: BiometricAuthMode) : InstallerSettingsAction
    data class ChangeShowOPPOSpecial(val show: Boolean) : InstallerSettingsAction

    // --- Collection Management ---
    data class AddManagedInstallerPackage(val pkg: NamedPackage) : InstallerSettingsAction
    data class RemoveManagedInstallerPackage(val pkg: NamedPackage) : InstallerSettingsAction
    data class AddManagedBlacklistPackage(val pkg: NamedPackage) : InstallerSettingsAction
    data class RemoveManagedBlacklistPackage(val pkg: NamedPackage) : InstallerSettingsAction
    data class AddManagedSharedUserIdBlacklist(val uid: SharedUid) : InstallerSettingsAction
    data class RemoveManagedSharedUserIdBlacklist(val uid: SharedUid) : InstallerSettingsAction
    data class AddManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : InstallerSettingsAction
    data class RemoveManagedSharedUserIdExemptedPackages(val pkg: NamedPackage) : InstallerSettingsAction

    data class MoveManagedInstallerPackage(val fromIndex: Int, val toIndex: Int) : InstallerSettingsAction
    data class MoveManagedBlacklistPackage(val fromIndex: Int, val toIndex: Int) : InstallerSettingsAction
    data class MoveManagedSharedUserIdBlacklist(val fromIndex: Int, val toIndex: Int) : InstallerSettingsAction
    data class MoveManagedSharedUserIdExemptedPackages(val fromIndex: Int, val toIndex: Int) : InstallerSettingsAction
}
