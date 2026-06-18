// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room3.ColumnTypeConverter
import com.rosan.installer.domain.settings.model.config.InstallerMode

object InstallerModeConverter {
    @ColumnTypeConverter
    fun fromInstallerMode(mode: InstallerMode): Int = mode.value

    @ColumnTypeConverter
    fun toInstallerMode(value: Int): InstallerMode =
        InstallerMode.entries.find { it.value == value } ?: InstallerMode.Self
}
