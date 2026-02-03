package com.rosan.installer.data.settings.repo

import com.rosan.installer.data.settings.model.room.entity.AppEntity
import com.rosan.installer.data.settings.util.AppOrder
import com.rosan.installer.data.settings.util.OrderType
import kotlinx.coroutines.flow.Flow

interface AppRepo {
    fun all(order: AppOrder = AppOrder.Id(OrderType.Ascending)): List<AppEntity>

    fun flowAll(order: AppOrder = AppOrder.Id(OrderType.Ascending)): Flow<List<AppEntity>>

    fun find(id: Long): AppEntity?

    fun flowFind(id: Long): Flow<AppEntity?>

    fun findByPackageName(packageName: String?): AppEntity?

    fun flowFindByPackageName(packageName: String?): Flow<AppEntity?>

    suspend fun update(entity: AppEntity)

    suspend fun insert(entity: AppEntity)

    suspend fun delete(entity: AppEntity)
}