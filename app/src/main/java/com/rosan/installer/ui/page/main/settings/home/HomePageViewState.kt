// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home

import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.RootMode

class HomePageViewState(
    val globalAuthorizer: Authorizer = Authorizer.Shizuku,
    val isDefaultInstaller: Boolean = false,

    // Shizuku states
    val shizukuAvailable: Boolean = false,
    val shizukuAuthorized: Boolean = false,
    val shizukuMode: ShizukuMode = ShizukuMode.NONE,

    // Dhizuku states
    val dhizukuAvailable: Boolean = false,
    val dhizukuAuthorized: Boolean = false,

    val rootMode: RootMode = RootMode.None,
    val isSystemApp: Boolean = false,
    val availableAuthorizerCount: Int = 0,
    val userSetLSPosedActive: Boolean = false,
    val autoLockInstaller: Boolean = false,
    val customizeAuthorizer: String = "",
    val defaultInstaller: String = "Unknown"
) {
    val activate: Boolean get() = isDefaultInstaller
}
