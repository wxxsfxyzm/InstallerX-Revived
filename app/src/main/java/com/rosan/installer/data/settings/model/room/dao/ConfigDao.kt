package com.rosan.installer.data.settings.model.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("select * from config")
    suspend fun all(): List<ConfigEntity>

    @Query("select * from config")
    fun flowAll(): Flow<List<ConfigEntity>>

    @Query("select * from config where id = :id limit 1")
    suspend fun find(id: Long): ConfigEntity?

    @Query("select * from config where id = :id limit 1")
    fun flowFind(id: Long): Flow<ConfigEntity?>

    @Update
    suspend fun update(entity: ConfigEntity)

    @Insert
    suspend fun insert(entity: ConfigEntity)

    @Delete
    suspend fun delete(entity: ConfigEntity)
}