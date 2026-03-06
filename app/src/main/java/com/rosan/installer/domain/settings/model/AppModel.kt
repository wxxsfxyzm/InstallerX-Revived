// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

data class AppModel(
    val id: Long,
    val packageName: String?,
    val configId: Long,
    val createdAt: Long,
    val modifiedAt: Long
)
