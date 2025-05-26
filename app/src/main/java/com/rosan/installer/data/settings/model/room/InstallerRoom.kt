package com.rosan.installer.data.settings.model.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.rosan.installer.data.settings.model.room.dao.AppDao
import com.rosan.installer.data.settings.model.room.dao.ConfigDao
import com.rosan.installer.data.settings.model.room.entity.AppEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Database(
    entities = [AppEntity::class, ConfigEntity::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = InstallerRoom.AUTO_MIGRATION_2_3::class),
        AutoMigration(from = 3, to = 4),
    ]
)
@TypeConverters(
    AuthorizerConverter::class,
    InstallModeConverter::class
)
abstract class InstallerRoom : RoomDatabase() {
    @DeleteColumn(tableName = "config", columnName = "analyser")
    @DeleteColumn(tableName = "config", columnName = "compat_mode")
    class AUTO_MIGRATION_2_3 : AutoMigrationSpec

    companion object : KoinComponent {
        fun createInstance(): InstallerRoom {
            return Room.databaseBuilder(
                get(),
                InstallerRoom::class.java,
                "installer.db",
            ).build()
        }
    }

    abstract val appDao: AppDao

    abstract val configDao: ConfigDao
}