// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import com.rosan.installer.domain.settings.model.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.RootMode

sealed interface LabSettingsAction {
    data class LabChangeRootModuleFlash(val enable: Boolean) : LabSettingsAction
    data class LabChangeRootShowModuleArt(val enable: Boolean) : LabSettingsAction
    data class LabChangeRootImplementation(val implementation: RootMode) : LabSettingsAction
    data class LabChangeSetInstallRequester(val enable: Boolean) : LabSettingsAction
    data class LabChangeHttpProfile(val profile: HttpProfile) : LabSettingsAction
    data class LabChangeHttpSaveFile(val enable: Boolean) : LabSettingsAction
    data class LabChangeTapIconToShare(val enable: Boolean) : LabSettingsAction
    data class LabChangeShowFilePath(val enable: Boolean) : LabSettingsAction
    data class LabChangeShowInstallInitiator(val enable: Boolean) : LabSettingsAction
    data class LabChangeAllowInstallWithoutUserAction(val enable: Boolean) : LabSettingsAction
    data class LabChangeGithubUpdateChannel(val channel: GithubUpdateChannel) : LabSettingsAction
    data class LabChangeCustomGithubProxyUrl(val url: String) : LabSettingsAction
}
