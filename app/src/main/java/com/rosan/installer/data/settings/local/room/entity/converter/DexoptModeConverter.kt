// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.domain.settings.model.DexoptMode

object DexoptModeConverter {
    @TypeConverter
    @JvmStatic
    fun revert(value: String): DexoptMode =
        DexoptMode.entries.firstOrNull { it.value == value } ?: DexoptMode.SpeedProfile

    @TypeConverter
    @JvmStatic
    fun convert(value: DexoptMode): String = value.value
}
