// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

data class InstallResult(
    val entity: SelectInstallEntity,
    val success: Boolean,
    val error: Throwable? = null
)
