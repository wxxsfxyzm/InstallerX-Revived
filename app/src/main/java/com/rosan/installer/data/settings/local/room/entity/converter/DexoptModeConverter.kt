package com.rosan.installer.data.settings.local.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.data.settings.local.room.entity.ConfigEntity

object DexoptModeConverter {
    @TypeConverter
    @JvmStatic
    fun revert(value: String): ConfigEntity.DexoptMode =
        ConfigEntity.DexoptMode.entries.firstOrNull { it.value == value } ?: ConfigEntity.DexoptMode.SpeedProfile

    @TypeConverter
    @JvmStatic
    fun convert(value: ConfigEntity.DexoptMode): String = value.value
}