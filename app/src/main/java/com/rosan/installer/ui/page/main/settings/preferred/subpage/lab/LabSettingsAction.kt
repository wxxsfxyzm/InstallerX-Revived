// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.lab

import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.RootImplementation

sealed class LabSettingsAction {
    data class LabChangeRootModuleFlash(val enable: Boolean) : LabSettingsAction()
    data class LabChangeRootShowModuleArt(val enable: Boolean) : LabSettingsAction()
    data class LabChangeRootModuleAlwaysUseRoot(val enable: Boolean) : LabSettingsAction()
    data class LabChangeRootImplementation(val implementation: RootImplementation) : LabSettingsAction()
    data class LabChangeUseMiIsland(val enable: Boolean) : LabSettingsAction()
    data class LabChangeSetInstallRequester(val enable: Boolean) : LabSettingsAction()
    data class LabChangeHttpProfile(val profile: HttpProfile) : LabSettingsAction()
    data class LabChangeHttpSaveFile(val enable: Boolean) : LabSettingsAction()
    data class LabChangeTapIconToShare(val enable: Boolean) : LabSettingsAction()
}
