// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.domain.settings.model.Authorizer

object AuthorizerConverter {
    @TypeConverter
    fun revert(value: String?): Authorizer {
        if (value == null) return Authorizer.Shizuku
        return Authorizer.entries.find { it.value == value } ?: Authorizer.Shizuku
    }

    @TypeConverter
    fun convert(value: Authorizer): String = value.value
}
