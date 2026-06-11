// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.repository

import androidx.room3.RoomRawQuery
import androidx.room3.withWriteTransaction
import com.rosan.installer.data.settings.local.room.InstallerRoom
import com.rosan.installer.data.settings.local.room.dao.AppDao
import com.rosan.installer.data.settings.local.room.dao.ConfigDao
import com.rosan.installer.data.settings.mapper.toDomainModel
import com.rosan.installer.data.settings.mapper.toEntity
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.DeletedConfigSnapshot
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.domain.settings.util.ConfigOrder
import com.rosan.installer.domain.settings.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepositoryImpl(
    private val dao: ConfigDao,
    private val appDao: AppDao,
    private val room: InstallerRoom
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

    override suspend fun deleteWithScopes(model: ConfigModel): DeletedConfigSnapshot =
        room.withWriteTransaction {
            val scopes = appDao.findByConfigId(model.id).map { it.toDomainModel() }
            val config = dao.find(model.id)?.toDomainModel(scopeCount = scopes.size) ?: model
            dao.delete(config.toEntity())
            DeletedConfigSnapshot(
                configModel = config,
                scopes = scopes
            )
        }

    override suspend fun restoreDeleted(snapshot: DeletedConfigSnapshot) {
        room.withWriteTransaction {
            dao.insert(snapshot.configModel.toEntity())
            snapshot.scopes.forEach { scope ->
                val currentScope = if (scope.packageName == null) {
                    appDao.findByNullPackageName()
                } else {
                    appDao.findByPackageName(scope.packageName)
                }
                val restoredScope = scope.copy(
                    id = currentScope?.id ?: 0L,
                    configId = snapshot.configModel.id
                )

                if (currentScope == null) {
                    appDao.insert(restoredScope.toEntity())
                } else {
                    appDao.update(restoredScope.toEntity())
                }
            }
        }
    }
}
