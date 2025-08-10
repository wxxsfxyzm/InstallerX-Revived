package com.rosan.installer.data.settings.model.room.repo

import com.rosan.installer.data.settings.model.room.dao.ConfigDao
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.repo.ConfigRepo
import com.rosan.installer.data.settings.util.ConfigOrder
import com.rosan.installer.data.settings.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepoImpl(
    private val dao: ConfigDao
) : ConfigRepo {
    override suspend fun all(order: ConfigOrder): List<ConfigEntity> {
        val configs = dao.all()
        return when (order.orderType) {
            OrderType.Ascending -> {
                when (order) {
                    is ConfigOrder.Id -> configs.sortedBy { it.id }
                    is ConfigOrder.Name -> configs.sortedBy { it.name }
                    is ConfigOrder.CreatedAt -> configs.sortedBy { it.createdAt }
                    is ConfigOrder.ModifiedAt -> configs.sortedBy { it.modifiedAt }
                }
            }

            OrderType.Descending -> {
                when (order) {
                    is ConfigOrder.Id -> configs.sortedByDescending { it.id }
                    is ConfigOrder.Name -> configs.sortedByDescending { it.name }
                    is ConfigOrder.CreatedAt -> configs.sortedByDescending { it.createdAt }
                    is ConfigOrder.ModifiedAt -> configs.sortedByDescending { it.modifiedAt }
                }
            }
        }
    }

    override fun flowAll(order: ConfigOrder): Flow<List<ConfigEntity>> {
        return dao.flowAll().map { configs ->
            when (order.orderType) {
                OrderType.Ascending -> {
                    when (order) {
                        is ConfigOrder.Id -> configs.sortedBy { it.id }
                        is ConfigOrder.Name -> configs.sortedBy { it.name }
                        is ConfigOrder.CreatedAt -> configs.sortedBy { it.createdAt }
                        is ConfigOrder.ModifiedAt -> configs.sortedBy { it.modifiedAt }
                    }
                }

                OrderType.Descending -> {
                    when (order) {
                        is ConfigOrder.Id -> configs.sortedByDescending { it.id }
                        is ConfigOrder.Name -> configs.sortedByDescending { it.name }
                        is ConfigOrder.CreatedAt -> configs.sortedByDescending { it.createdAt }
                        is ConfigOrder.ModifiedAt -> configs.sortedByDescending { it.modifiedAt }
                    }
                }
            }
        }
    }

    override suspend fun find(id: Long): ConfigEntity? {
        return dao.find(id)
    }

    override fun flowFind(id: Long): Flow<ConfigEntity?> {
        return dao.flowFind(id)
    }

    override suspend fun update(entity: ConfigEntity) {
        entity.modifiedAt = System.currentTimeMillis()
        dao.update(entity)
    }

    override suspend fun insert(entity: ConfigEntity) {
        entity.createdAt = System.currentTimeMillis()
        entity.modifiedAt = System.currentTimeMillis()
        dao.insert(entity)
    }

    override suspend fun delete(entity: ConfigEntity) {
        dao.delete(entity)
    }
}