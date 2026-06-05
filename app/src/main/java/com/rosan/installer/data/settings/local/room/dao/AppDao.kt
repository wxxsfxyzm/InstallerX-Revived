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
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Allows dynamic sorting execution in SQLite
    @RawQuery
    suspend fun getAllDynamically(query: RoomRawQuery): List<AppEntity>

    // Must specify observedEntities for Flow to react to database changes
    @RawQuery(observedEntities = [AppEntity::class])
    fun flowAllDynamically(query: RoomRawQuery): Flow<List<AppEntity>>

    @Query("select * from app")
    fun all(): List<AppEntity>

    @Query("select * from app")
    suspend fun allSuspend(): List<AppEntity>

    @Query("select * from app")
    fun flowAll(): Flow<List<AppEntity>>

    @Query("select * from app where id = :id limit 1")
    fun find(id: Long): AppEntity?

    @Query("select * from app where id = :id limit 1")
    fun flowFind(id: Long): Flow<AppEntity?>

    @Query("select * from app where package_name = :packageName limit 1")
    fun findByPackageName(packageName: String): AppEntity?

    @Query("select * from app where package_name is null limit 1")
    fun findByNullPackageName(): AppEntity?

    @Query("select * from app where package_name = :packageName limit 1")
    fun flowFindByPackageName(packageName: String): Flow<AppEntity?>

    @Query("select * from app where package_name is null limit 1")
    fun flowFindByNullPackageName(): Flow<AppEntity?>

    @Update
    suspend fun update(appEntity: AppEntity)

    @Insert
    suspend fun insert(appEntity: AppEntity)

    @Insert
    suspend fun insertAll(appEntities: List<AppEntity>)

    @Delete
    suspend fun delete(appEntity: AppEntity)

    @Query("delete from app")
    suspend fun deleteAll()
}
