// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.about

import com.rosan.installer.domain.settings.model.Authorizer

data class AboutState(
    val useBlur: Boolean = true,
    val authorizer: Authorizer = Authorizer.None,
    val hasUpdate: Boolean = false,
    val remoteVersion: String = "",
    val enableFileLogging: Boolean = false
)
