package com.rosan.installer.data.settings.model.room.entity.converter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

object PackageSourceConverter {
    @TypeConverter
    fun convert(packageSource: ConfigEntity.PackageSource): Int = packageSource.value


    @TypeConverter
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun revert(value: Int): ConfigEntity.PackageSource = ConfigEntity.PackageSource.fromInt(value)

}