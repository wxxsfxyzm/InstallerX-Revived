// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room3.ColumnTypeConverter
import com.rosan.installer.domain.settings.model.config.InstallReason

object InstallReasonConverter {
    @ColumnTypeConverter
    fun convert(installReason: InstallReason): Int = installReason.value

    @ColumnTypeConverter
    fun revert(value: Int): InstallReason = InstallReason.fromInt(value)
}
