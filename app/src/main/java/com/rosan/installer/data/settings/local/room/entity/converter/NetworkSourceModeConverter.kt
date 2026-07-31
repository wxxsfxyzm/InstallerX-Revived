// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room3.ColumnTypeConverter
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode

object NetworkSourceModeConverter {
    @ColumnTypeConverter
    @JvmStatic
    fun revert(value: String): NetworkSourceMode = NetworkSourceMode.fromValue(value)

    @ColumnTypeConverter
    @JvmStatic
    fun convert(value: NetworkSourceMode): String = value.value
}
