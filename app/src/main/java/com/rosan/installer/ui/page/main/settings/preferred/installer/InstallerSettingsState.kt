// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.BiometricAuthMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid

data class InstallerSettingsState(
    val authorizer: Authorizer = Authorizer.Shizuku,
    val alwaysUseRootInSystem: Boolean = false,
    val closeSessionCountDown: Int = 5,
    val installerRequireBiometricAuth: BiometricAuthMode = BiometricAuthMode.FollowConfig,
    val showOPPOSpecial: Boolean = false,
    val detectXposedModule: Boolean = true,
    val quickOpenLSPosed: Boolean = true,
    val managedInstallerPackages: List<NamedPackage> = emptyList(),
    val managedBlacklistPackages: List<NamedPackage> = emptyList(),
    val managedSharedUserIdBlacklist: List<SharedUid> = emptyList(),
    val managedSharedUserIdExemptedPackages: List<NamedPackage> = emptyList(),
    val setInstallRequester: Boolean = false
)
