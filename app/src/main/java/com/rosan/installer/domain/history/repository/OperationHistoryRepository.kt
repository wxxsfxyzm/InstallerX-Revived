// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.history.repository

import com.rosan.installer.domain.history.model.OperationHistoryModel
import kotlinx.coroutines.flow.Flow

interface OperationHistoryRepository {
    suspend fun all(limit: Int = DEFAULT_HISTORY_LIMIT): List<OperationHistoryModel>

    fun flowAll(limit: Int = DEFAULT_HISTORY_LIMIT): Flow<List<OperationHistoryModel>>

    suspend fun insert(model: OperationHistoryModel)

    suspend fun clear()

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 100
    }
}
