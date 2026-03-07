// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.domain.settings.model.InstallReason

object InstallReasonConverter {
    @TypeConverter
    fun convert(installReason: InstallReason): Int = installReason.value

    @TypeConverter
    fun revert(value: Int): InstallReason = InstallReason.fromInt(value)
}
