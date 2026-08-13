// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.network

import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode

sealed interface NetworkSettingsAction {
    data class ChangeInternetAccess(val enabled: Boolean) : NetworkSettingsAction
    data class ChangeNetworkSourceMode(val mode: NetworkSourceMode) : NetworkSettingsAction
    data class ConfirmNetworkSourceMode(val mode: NetworkSourceMode) : NetworkSettingsAction
    data class ChangeHttpProfile(val profile: HttpProfile) : NetworkSettingsAction
    data class ChangeGithubUpdateChannel(val channel: GithubUpdateChannel) : NetworkSettingsAction
    data class ChangeCustomGithubProxyUrl(val url: String) : NetworkSettingsAction
}
