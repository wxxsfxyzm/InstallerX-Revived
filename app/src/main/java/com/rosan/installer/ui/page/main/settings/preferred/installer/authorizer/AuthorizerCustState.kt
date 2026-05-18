// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer

import com.rosan.installer.domain.settings.model.Authorizer

data class AuthorizerCustState(
    val authorizer: Authorizer = Authorizer.Shizuku,
    val alwaysUseRootInSystem: Boolean = false,
    val closeSessionCountDown: Int = 5,
    val allowInstallWithoutUserAction: Boolean = false
)
