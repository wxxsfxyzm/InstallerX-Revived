// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.rosan.installer.domain.settings.model.PackageSource

object PackageSourceConverter {
    @TypeConverter
    fun convert(packageSource: PackageSource): Int = packageSource.value

    @TypeConverter
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun revert(value: Int): PackageSource = PackageSource.fromInt(value)
}
