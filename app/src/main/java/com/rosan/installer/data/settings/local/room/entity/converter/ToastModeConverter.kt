// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room3.ColumnTypeConverter
import com.rosan.installer.domain.settings.model.config.ToastMode

object ToastModeConverter {
    @ColumnTypeConverter
    fun fromToastMode(mode: ToastMode): Int = mode.value

    @ColumnTypeConverter
    fun toToastMode(value: Int): ToastMode = ToastMode.fromValue(value)
}