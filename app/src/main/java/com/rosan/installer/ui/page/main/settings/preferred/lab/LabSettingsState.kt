// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.preferences.RootMode

data class LabSettingsState(
    val labRootEnableModuleFlash: Boolean = false,
    val labRootShowModuleArt: Boolean = true,
    val labRootMode: RootMode = RootMode.Magisk,
    val labHttpProfile: HttpProfile = HttpProfile.ALLOW_SECURE,
    val labHttpSaveFile: Boolean = false,
    val labTapIconToShare: Boolean = false,
    val labAllowInstallWithoutUserAction: Boolean = false,
    val githubUpdateChannel: GithubUpdateChannel = GithubUpdateChannel.OFFICIAL,
    val customGithubProxyUrl: String = ""
)
