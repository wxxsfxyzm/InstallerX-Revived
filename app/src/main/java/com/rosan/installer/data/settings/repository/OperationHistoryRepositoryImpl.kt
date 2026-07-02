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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class OperationHistoryRepositoryImpl(
    private val dao: OperationHistoryDao
) : OperationHistoryRepository {
    private val latestSessionHistoryIds = ConcurrentHashMap<SessionHistoryKey, Long>()
    private val sessionHistoryMutex = Mutex()

    override suspend fun all(limit: Int): List<OperationHistoryModel> =
        dao.all(limit).map { it.toDomainModel() }

    override fun flowAll(limit: Int): Flow<List<OperationHistoryModel>> =
        dao.flowAll(limit).map { list -> list.map { it.toDomainModel() } }

    override suspend fun insert(model: OperationHistoryModel) = sessionHistoryMutex.withLock {
        val key = model.sessionHistoryKey()
        val previousId = key?.let { latestSessionHistoryIds[it] }
        val newId = dao.insert(model.toEntity())

        if (previousId != null && previousId != newId) {
            dao.deleteById(previousId)
        }
        if (key != null) {
            latestSessionHistoryIds[key] = newId
        }

        dao.trimToLimit(DEFAULT_HISTORY_LIMIT)
    }

    override suspend fun clear() = sessionHistoryMutex.withLock {
        dao.clear()
        latestSessionHistoryIds.clear()
    }

    private fun OperationHistoryModel.sessionHistoryKey(): SessionHistoryKey? {
        val sessionKey = operationSessionKey?.takeIf { it.isNotBlank() } ?: return null
        return SessionHistoryKey(
            operationSessionKey = sessionKey,
            operationType = operationType.name,
            packageName = packageName
        )
    }

    private data class SessionHistoryKey(
        val operationSessionKey: String,
        val operationType: String,
        val packageName: String
    )
}
