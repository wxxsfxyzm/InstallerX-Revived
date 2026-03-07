// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.rosan.installer.data.settings.local.room.dao.AppDao
import com.rosan.installer.data.settings.mapper.toDomainModel
import com.rosan.installer.data.settings.mapper.toEntity
import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.util.AppOrder
import com.rosan.installer.domain.settings.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(
    private val dao: AppDao
) : AppRepository {

    // Helper method to build dynamic SQL queries for sorting
    private fun buildOrderQuery(order: AppOrder): SimpleSQLiteQuery {
        val column = when (order) {
            is AppOrder.Id -> "id"
            is AppOrder.PackageName -> "package_name"
            is AppOrder.ConfigId -> "config_id"
            is AppOrder.CreateAt -> "created_at"
            is AppOrder.ModifiedAt -> "modified_at"
        }
        val direction = when (order.orderType) {
            OrderType.Ascending -> "ASC"
            OrderType.Descending -> "DESC"
        }
        return SimpleSQLiteQuery("SELECT * FROM app ORDER BY $column $direction")
    }

    override suspend fun all(order: AppOrder): List<AppModel> {
        val query = buildOrderQuery(order)
        return dao.getAllDynamically(query).map { it.toDomainModel() }
    }

    override fun flowAll(order: AppOrder): Flow<List<AppModel>> {
        val query = buildOrderQuery(order)
        return dao.flowAllDynamically(query).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun find(id: Long): AppModel? {
        return dao.find(id)?.toDomainModel()
    }

    override fun flowFind(id: Long): Flow<AppModel?> {
        return dao.flowFind(id).map { it?.toDomainModel() }
    }

    override suspend fun findByPackageName(packageName: String?): AppModel? {
        val entity = if (packageName == null) dao.findByNullPackageName()
        else dao.findByPackageName(packageName)
        return entity?.toDomainModel()
    }

    override fun flowFindByPackageName(packageName: String?): Flow<AppModel?> {
        val flow = if (packageName == null) dao.flowFindByNullPackageName()
        else dao.flowFindByPackageName(packageName)
        return flow.map { it?.toDomainModel() }
    }

    override suspend fun update(model: AppModel) {
        val entity = model.toEntity()
        entity.modifiedAt = System.currentTimeMillis()
        dao.update(entity)
    }

    override suspend fun insert(model: AppModel) {
        val entity = model.toEntity()
        entity.createdAt = System.currentTimeMillis()
        entity.modifiedAt = System.currentTimeMillis()
        dao.insert(entity)
    }

    override suspend fun delete(model: AppModel) {
        dao.delete(model.toEntity())
    }
}