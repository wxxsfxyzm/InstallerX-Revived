// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer

sealed interface AuthorizerCustAction {
    data class ChangeAlwaysUseRootInSystem(val alwaysUseRootInSystem: Boolean) : AuthorizerCustAction
    data class ChangeCloseSessionCountDown(val countDown: Int) : AuthorizerCustAction
    data class ChangeAllowInstallWithoutUserAction(val enable: Boolean) : AuthorizerCustAction
}
