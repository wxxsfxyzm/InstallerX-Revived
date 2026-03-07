// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.domain.settings.model.InstallMode

object InstallModeConverter {
    @TypeConverter
    fun revert(value: String?): InstallMode =
        (if (value != null) InstallMode.entries.find { it.value == value }
        else null) ?: InstallMode.Dialog

    @TypeConverter
    fun convert(value: InstallMode): String = value.value
}
