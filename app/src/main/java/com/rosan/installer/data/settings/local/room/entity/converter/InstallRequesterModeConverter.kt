// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room3.TypeConverter
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode

object InstallRequesterModeConverter {
    @TypeConverter
    fun fromInstallRequesterMode(mode: InstallRequesterMode): Int = mode.value

    @TypeConverter
    fun toInstallRequesterMode(value: Int): InstallRequesterMode =
        InstallRequesterMode.entries.find { it.value == value } ?: InstallRequesterMode.Disable
}
