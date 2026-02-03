package com.rosan.installer.data.settings.repo

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigOrder
import com.rosan.installer.data.settings.util.OrderType
import kotlinx.coroutines.flow.Flow

interface ConfigRepo {
    suspend fun all(order: ConfigOrder = ConfigOrder.Id(OrderType.Ascending)): List<ConfigEntity>

    fun flowAll(order: ConfigOrder = ConfigOrder.Id(OrderType.Ascending)): Flow<List<ConfigEntity>>

    suspend fun find(id: Long): ConfigEntity?

    fun flowFind(id: Long): Flow<ConfigEntity?>

    suspend fun update(entity: ConfigEntity)

    suspend fun insert(entity: ConfigEntity)

    suspend fun delete(entity: ConfigEntity)
}