// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.network

import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile

data class NetworkSettingsState(
    val allowInternetAccess: Boolean = true,
    val networkSourceMode: NetworkSourceMode = NetworkSourceMode.Cache,
    val networkSourceModeWarningAcknowledged: Boolean = false,
    val httpProfile: HttpProfile = HttpProfile.ALLOW_SECURE,
    val githubUpdateChannel: GithubUpdateChannel = GithubUpdateChannel.OFFICIAL,
    val customGithubProxyUrl: String = "",
)
