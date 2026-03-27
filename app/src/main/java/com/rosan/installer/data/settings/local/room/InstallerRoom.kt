// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rosan.installer.data.settings.local.room.dao.AppDao
import com.rosan.installer.data.settings.local.room.dao.ConfigDao
import com.rosan.installer.data.settings.local.room.entity.AppEntity
import com.rosan.installer.data.settings.local.room.entity.ConfigEntity
import com.rosan.installer.data.settings.local.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.local.room.entity.converter.DexoptModeConverter
import com.rosan.installer.data.settings.local.room.entity.converter.InstallModeConverter
import com.rosan.installer.data.settings.local.room.entity.converter.InstallReasonConverter
import com.rosan.installer.data.settings.local.room.entity.converter.PackageSourceConverter
import com.rosan.installer.data.settings.mapper.toEntity
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Database(
    entities = [AppEntity::class, ConfigEntity::class],
    version = 12,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = InstallerRoom.Migration7To8::class),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12)
    ]
)
@TypeConverters(
    AuthorizerConverter::class,
    InstallModeConverter::class,
    DexoptModeConverter::class,
    PackageSourceConverter::class,
    InstallReasonConverter::class
)
abstract class InstallerRoom : RoomDatabase() {
    companion object : KoinComponent {
        fun createInstance(): InstallerRoom {
            return Room.databaseBuilder(
                get(),
                InstallerRoom::class.java,
                "installer.db",
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val database = get<InstallerRoom>()
                    val defaultConfig = ConfigModel.generateOptimalDefault().toEntity()
                    CoroutineScope(Dispatchers.IO).launch {
                        database.configDao.insert(defaultConfig)
                    }
                }
            })
                .build()
        }
    }

    abstract val appDao: AppDao

    abstract val configDao: ConfigDao

    @DeleteColumn(tableName = "config", columnName = "allow_restricted_permissions")
    class Migration7To8 : AutoMigrationSpec
}

