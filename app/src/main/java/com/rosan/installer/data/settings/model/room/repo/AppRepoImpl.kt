package com.rosan.installer.data.settings.model.room.repo

import com.rosan.installer.data.settings.model.room.dao.AppDao
import com.rosan.installer.data.settings.model.room.entity.AppEntity
import com.rosan.installer.data.settings.repo.AppRepo
import com.rosan.installer.data.settings.util.AppOrder
import com.rosan.installer.data.settings.util.OrderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepoImpl(
    private val dao: AppDao
) : AppRepo {
    override fun all(order: AppOrder): List<AppEntity> {
        val apps = dao.all()
        return when (order.orderType) {
            OrderType.Ascending -> {
                when (order) {
                    is AppOrder.Id -> apps.sortedBy { it.id }
                    is AppOrder.PackageName -> apps.sortedBy { it.packageName }
                    is AppOrder.ConfigId -> apps.sortedBy { it.configId }
                    is AppOrder.CreateAt -> apps.sortedBy { it.createdAt }
                    is AppOrder.ModifiedAt -> apps.sortedBy { it.modifiedAt }
                }
            }
            OrderType.Descending -> {
                when (order) {
                    is AppOrder.Id -> apps.sortedByDescending { it.id }
                    is AppOrder.PackageName -> apps.sortedByDescending { it.packageName }
                    is AppOrder.ConfigId -> apps.sortedByDescending { it.configId }
                    is AppOrder.CreateAt -> apps.sortedByDescending { it.createdAt }
                    is AppOrder.ModifiedAt -> apps.sortedByDescending { it.modifiedAt }
                }
            }
        }
    }

    override fun flowAll(order: AppOrder): Flow<List<AppEntity>> {
        return dao.flowAll().map { apps ->
            when (order.orderType) {
                OrderType.Ascending -> {
                    when (order) {
                        is AppOrder.Id -> apps.sortedBy { it.id }
                        is AppOrder.PackageName -> apps.sortedBy { it.packageName }
                        is AppOrder.ConfigId -> apps.sortedBy { it.configId }
                        is AppOrder.CreateAt -> apps.sortedBy { it.createdAt }
                        is AppOrder.ModifiedAt -> apps.sortedBy { it.modifiedAt }
                    }
                }
                OrderType.Descending -> {
                    when (order) {
                        is AppOrder.Id -> apps.sortedByDescending { it.id }
                        is AppOrder.PackageName -> apps.sortedByDescending { it.packageName }
                        is AppOrder.ConfigId -> apps.sortedByDescending { it.configId }
                        is AppOrder.CreateAt -> apps.sortedByDescending { it.createdAt }
                        is AppOrder.ModifiedAt -> apps.sortedByDescending { it.modifiedAt }
                    }
                }
            }
        }
    }

    override fun find(id: Long): AppEntity? {
        return dao.find(id)
    }

    override fun flowFind(id: Long): Flow<AppEntity?> {
        return dao.flowFind(id)
    }

    override fun findByPackageName(packageName: String?): AppEntity? {
        return if (packageName == null) dao.findByNullPackageName()
        else dao.findByPackageName(packageName)
    }

    override fun flowFindByPackageName(packageName: String?): Flow<AppEntity?> {
        return if (packageName == null) dao.flowFindByNullPackageName()
        else dao.flowFindByPackageName(packageName)
    }

    override suspend fun update(entity: AppEntity) {
        entity.modifiedAt = System.currentTimeMillis()
        dao.update(entity)
    }

    override suspend fun insert(entity: AppEntity) {
        entity.createdAt = System.currentTimeMillis()
        entity.modifiedAt = System.currentTimeMillis()
        dao.insert(entity)
    }

    override suspend fun delete(entity: AppEntity) {
        dao.delete(entity)
    }
}