// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import androidx.room3.RoomRawQuery
import com.rosan.installer.data.settings.local.room.dao.ConfigDao
import com.rosan.installer.data.settings.mapper.toDomainModel
import com.rosan.installer.data.settings.mapper.toEntity
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.domain.settings.util.ConfigOrder
import com.rosan.installer.domain.settings.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepositoryImpl(
    private val dao: ConfigDao
) : ConfigRepository {

    private fun buildOrderQuery(order: ConfigOrder): RoomRawQuery {
        val column = when (order) {
            is ConfigOrder.Id -> "id"
            is ConfigOrder.Name -> "name"
            is ConfigOrder.CreatedAt -> "createdAt"
            is ConfigOrder.ModifiedAt -> "modifiedAt"
        }
        val direction = if (order.orderType == OrderType.Ascending) "ASC" else "DESC"

        // Subquery to count related apps from the 'app' table
        val queryString = """
            SELECT config.*, 
                   (SELECT COUNT(*) FROM app WHERE app.config_id = config.id) AS scope_count
            FROM config 
            ORDER BY config.$column $direction
        """.trimIndent()

        return RoomRawQuery(queryString)
    }

    override suspend fun all(order: ConfigOrder): List<ConfigModel> {
        val query = buildOrderQuery(order)
        return dao.getAllDynamically(query).map { it.toDomainModel() } // Requires Mapper update
    }

    override fun flowAll(order: ConfigOrder): Flow<List<ConfigModel>> {
        val query = buildOrderQuery(order)
        return dao.flowAllDynamically(query).map { list -> list.map { it.toDomainModel() } } // Requires Mapper update
    }

    override suspend fun find(id: Long): ConfigModel? {
        return dao.find(id)?.toDomainModel()
    }

    override fun flowFind(id: Long): Flow<ConfigModel?> {
        return dao.flowFind(id).map { it?.toDomainModel() }
    }

    override suspend fun findDefault(): ConfigModel? {
        return dao.findDefault()?.toDomainModel()
    }

    override suspend fun update(model: ConfigModel) {
        val entity = model.toEntity()
        entity.modifiedAt = System.currentTimeMillis()
        dao.update(entity)
    }

    override suspend fun insert(model: ConfigModel) {
        val entity = model.toEntity()
        entity.createdAt = System.currentTimeMillis()
        entity.modifiedAt = System.currentTimeMillis()
        dao.insert(entity)
    }

    override suspend fun delete(model: ConfigModel) {
        dao.delete(model.toEntity())
    }
}