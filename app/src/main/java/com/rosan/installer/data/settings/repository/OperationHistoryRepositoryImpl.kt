// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import com.rosan.installer.data.settings.local.room.dao.OperationHistoryDao
import com.rosan.installer.data.settings.mapper.toDomainModel
import com.rosan.installer.data.settings.mapper.toEntity
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.repository.OperationHistoryRepository
import com.rosan.installer.domain.history.repository.OperationHistoryRepository.Companion.DEFAULT_HISTORY_LIMIT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OperationHistoryRepositoryImpl(
    private val dao: OperationHistoryDao
) : OperationHistoryRepository {
    override suspend fun all(limit: Int): List<OperationHistoryModel> =
        dao.all(limit).map { it.toDomainModel() }

    override fun flowAll(limit: Int): Flow<List<OperationHistoryModel>> =
        dao.flowAll(limit).map { list -> list.map { it.toDomainModel() } }

    override suspend fun insert(model: OperationHistoryModel) {
        dao.insert(model.toEntity())
        dao.trimToLimit(DEFAULT_HISTORY_LIMIT)
    }

    override suspend fun clear() {
        dao.clear()
    }
}
