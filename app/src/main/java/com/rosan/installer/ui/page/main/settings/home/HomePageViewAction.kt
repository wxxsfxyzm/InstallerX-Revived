// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home

import com.rosan.installer.domain.settings.model.Authorizer

sealed interface HomePageViewAction {
    data object RefreshActivateStatus : HomePageViewAction
    data class ChangeAutoLockInstaller(val autoLockInstaller: Boolean) : HomePageViewAction
    data class SetDefaultInstaller(val lock: Boolean) : HomePageViewAction
    data class ChangeAuthorizer(val authorizer: Authorizer) : HomePageViewAction
    data class ChangeUserSetLSPosedActive(val active: Boolean) : HomePageViewAction
}
