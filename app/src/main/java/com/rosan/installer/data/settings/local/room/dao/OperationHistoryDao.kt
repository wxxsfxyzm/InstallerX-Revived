// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.rosan.installer.data.settings.local.room.entity.OperationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationHistoryDao {
    @Query("select * from operation_history order by timestamp desc, id desc")
    suspend fun all(): List<OperationHistoryEntity>

    @Query("select * from operation_history order by timestamp desc, id desc limit :limit")
    suspend fun all(limit: Int): List<OperationHistoryEntity>

    @Query("select * from operation_history order by timestamp desc, id desc limit :limit")
    fun flowAll(limit: Int): Flow<List<OperationHistoryEntity>>

    @Insert
    suspend fun insert(entity: OperationHistoryEntity): Long

    @Query(
        """
        delete from operation_history
        where id not in (
            select id from operation_history
            order by timestamp desc, id desc
            limit :limit
        )
        """
    )
    suspend fun trimToLimit(limit: Int)

    @Query("delete from operation_history")
    suspend fun clear()
}
