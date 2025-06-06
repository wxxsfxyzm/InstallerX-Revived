package com.rosan.installer.data.settings.model.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

object AuthorizerConverter {
    @TypeConverter
    fun revert(value: String?): ConfigEntity.Authorizer =
        (if (value != null) ConfigEntity.Authorizer.entries.find { it.value == value }
        else null) ?: ConfigEntity.Authorizer.Shizuku

    @TypeConverter
    fun convert(value: ConfigEntity.Authorizer): String = value.value
}