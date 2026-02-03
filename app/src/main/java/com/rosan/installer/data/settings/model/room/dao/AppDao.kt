package com.rosan.installer.data.settings.model.room.dao

import androidx.room.*
import com.rosan.installer.data.settings.model.room.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("select * from app")
    fun all(): List<AppEntity>

    @Query("select * from app")
    fun flowAll(): Flow<List<AppEntity>>

    @Query("select * from app where id = :id limit 1")
    fun find(id: Long): AppEntity?

    @Query("select * from app where id = :id limit 1")
    fun flowFind(id: Long): Flow<AppEntity?>

    @Query("select * from app where package_name = :packageName limit 1")
    fun findByPackageName(packageName: String): AppEntity?

    @Query("select * from app where package_name is null limit 1")
    fun findByNullPackageName(): AppEntity?

    @Query("select * from app where package_name = :packageName limit 1")
    fun flowFindByPackageName(packageName: String): Flow<AppEntity?>

    @Query("select * from app where package_name is null limit 1")
    fun flowFindByNullPackageName(): Flow<AppEntity?>

    @Update
    suspend fun update(appEntity: AppEntity)

    @Insert
    suspend fun insert(appEntity: AppEntity)

    @Delete
    suspend fun delete(appEntity: AppEntity)
}