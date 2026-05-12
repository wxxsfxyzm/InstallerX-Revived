// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.DeleteColumn
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.rosan.installer.data.settings.local.room.dao.AppDao
import com.rosan.installer.data.settings.local.room.dao.ConfigDao
import com.rosan.installer.data.settings.local.room.entity.AppEntity
import com.rosan.installer.data.settings.local.room.entity.ConfigEntity
import com.rosan.installer.data.settings.local.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.local.room.entity.converter.DexoptModeConverter
import com.rosan.installer.data.settings.local.room.entity.converter.InstallModeConverter
import com.rosan.installer.data.settings.local.room.entity.converter.InstallReasonConverter
import com.rosan.installer.data.settings.local.room.entity.converter.InstallerModeConverter
import com.rosan.installer.data.settings.local.room.entity.converter.PackageSourceConverter
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Database(
    entities = [AppEntity::class, ConfigEntity::class],
    version = 15,
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
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
    ]
)
@TypeConverters(
    AuthorizerConverter::class,
    InstallModeConverter::class,
    InstallerModeConverter::class,
    DexoptModeConverter::class,
    PackageSourceConverter::class,
    InstallReasonConverter::class
)
abstract class InstallerRoom : RoomDatabase() {
    companion object : KoinComponent {
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // Add new column with default value 0 (Self)
                // Use executeSQL instead of execSQL in Room 3.0
                connection.execSQL("ALTER TABLE config ADD COLUMN installer_mode INTEGER NOT NULL DEFAULT 0")
                // Update mode to 2 (Custom) for rows that already have a custom installer set
                connection.execSQL("UPDATE config SET installer_mode = 2 WHERE installer IS NOT NULL")
            }
        }

        fun createInstance(): InstallerRoom =
            Room.databaseBuilder(
                get(),
                InstallerRoom::class.java,
                "installer.db",
            )
                .addMigrations(MIGRATION_12_13)
                .build()

    }

    abstract val appDao: AppDao

    abstract val configDao: ConfigDao

    @DeleteColumn(tableName = "config", columnName = "allow_restricted_permissions")
    class Migration7To8 : AutoMigrationSpec
}
