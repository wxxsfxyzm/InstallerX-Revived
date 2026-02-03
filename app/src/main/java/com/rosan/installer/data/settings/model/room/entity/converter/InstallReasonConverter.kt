package com.rosan.installer.data.settings.model.room.entity.converter

import androidx.room.TypeConverter
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

object InstallReasonConverter {
    @TypeConverter
    fun convert(installReason: ConfigEntity.InstallReason): Int = installReason.value


    @TypeConverter
    fun revert(value: Int): ConfigEntity.InstallReason = ConfigEntity.InstallReason.fromInt(value)

}