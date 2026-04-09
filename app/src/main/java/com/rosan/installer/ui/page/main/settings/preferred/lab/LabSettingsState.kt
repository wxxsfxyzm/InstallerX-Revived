// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import com.rosan.installer.domain.settings.model.HttpProfile
import com.rosan.installer.domain.settings.model.RootMode

data class LabSettingsState(
    val useBlur: Boolean = true,
    val labRootEnableModuleFlash: Boolean = false,
    val labRootShowModuleArt: Boolean = true,
    val labRootMode: RootMode = RootMode.Magisk,
    val labSetInstallRequester: Boolean = false,
    val labHttpProfile: HttpProfile = HttpProfile.ALLOW_SECURE,
    val labHttpSaveFile: Boolean = false,
    val labTapIconToShare: Boolean = false
)
