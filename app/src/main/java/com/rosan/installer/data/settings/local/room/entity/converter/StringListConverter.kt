// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room3.TypeConverter
import kotlinx.serialization.json.Json

object StringListConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun revert(value: String?): List<String> =
        value?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()

    @TypeConverter
    fun convert(value: List<String>): String = json.encodeToString(value)
}
