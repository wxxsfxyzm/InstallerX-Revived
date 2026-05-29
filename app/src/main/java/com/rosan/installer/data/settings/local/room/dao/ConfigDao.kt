// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Update
import com.rosan.installer.data.settings.local.room.entity.AppEntity
import com.rosan.installer.data.settings.local.room.entity.ConfigEntity
import com.rosan.installer.data.settings.local.room.entity.ConfigWithScopeCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @RawQuery
    suspend fun getAllDynamically(query: RoomRawQuery): List<ConfigEntity>

    // Observe BOTH entities to trigger flow updates when scope changes
    @RawQuery(observedEntities = [ConfigEntity::class, AppEntity::class])
    fun flowAllDynamically(query: RoomRawQuery): Flow<List<ConfigWithScopeCount>>

    @Query("select * from config")
    suspend fun all(): List<ConfigEntity>

    @Query("select * from config")
    fun flowAll(): Flow<List<ConfigEntity>>

    @Query("select * from config where id = :id limit 1")
    suspend fun find(id: Long): ConfigEntity?

    @Query("select * from config where id = :id limit 1")
    fun flowFind(id: Long): Flow<ConfigEntity?>

    @Query("select * from config order by id asc limit 1")
    suspend fun findDefault(): ConfigEntity?

    @Update
    suspend fun update(entity: ConfigEntity)

    @Insert
    suspend fun insert(entity: ConfigEntity)

    @Insert
    suspend fun insertAndReturnId(entity: ConfigEntity): Long

    @Delete
    suspend fun delete(entity: ConfigEntity)

    @Query("delete from config")
    suspend fun deleteAll()
}
