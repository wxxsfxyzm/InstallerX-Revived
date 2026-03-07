// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.rosan.installer.data.settings.local.room.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Allows dynamic sorting execution in SQLite
    @RawQuery
    suspend fun getAllDynamically(query: SupportSQLiteQuery): List<AppEntity>

    // Must specify observedEntities for Flow to react to database changes
    @RawQuery(observedEntities = [AppEntity::class])
    fun flowAllDynamically(query: SupportSQLiteQuery): Flow<List<AppEntity>>

    @Query("select * from app")
    fun all(): List<AppEntity>

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

    @Delete
    suspend fun delete(appEntity: AppEntity)
}