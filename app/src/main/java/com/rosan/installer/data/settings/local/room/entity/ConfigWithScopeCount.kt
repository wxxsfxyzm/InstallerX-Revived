// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Embedded

data class ConfigWithScopeCount(
    @Embedded
    val config: ConfigEntity,

    // The exact name used in the SQL AS clause
    @ColumnInfo(name = "scope_count")
    val scopeCount: Int,
)
