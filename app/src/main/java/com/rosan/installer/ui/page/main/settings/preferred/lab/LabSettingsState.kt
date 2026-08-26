// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerCandidate

data class LabSettingsState(
    val labRootEnableModuleFlash: Boolean = false,
    val labRootShowModuleArt: Boolean = true,
    val labRootMode: RootMode = RootMode.Magisk,
    val labAllowInstallWithoutUserAction: Boolean = false,
    val labRespectPlatformInstallPolicy: Boolean = false,
    val tryMultipleAuthorizersOnInstall: Boolean = false,
    val smartAuthorizerCandidates: List<SmartAuthorizerCandidate> = emptyList(),
)
