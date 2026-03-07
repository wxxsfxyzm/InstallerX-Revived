// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller

import com.rosan.installer.domain.settings.model.Authorizer

data class UninstallerSettingsState(
    val useBlur: Boolean = true,
    val authorizer: Authorizer = Authorizer.None,
    val uninstallFlags: Int = 0,
    val uninstallerRequireBiometricAuth: Boolean = false
)
