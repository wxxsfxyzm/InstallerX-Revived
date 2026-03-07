// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.domain.settings.repository

import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.util.AppOrder
import com.rosan.installer.domain.settings.util.OrderType
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    // Added suspend keyword and changed return type to AppModel
    suspend fun all(order: AppOrder = AppOrder.Id(OrderType.Ascending)): List<AppModel>

    fun flowAll(order: AppOrder = AppOrder.Id(OrderType.Ascending)): Flow<List<AppModel>>

    suspend fun find(id: Long): AppModel?

    fun flowFind(id: Long): Flow<AppModel?>

    suspend fun findByPackageName(packageName: String?): AppModel?

    fun flowFindByPackageName(packageName: String?): Flow<AppModel?>

    suspend fun update(model: AppModel)

    suspend fun insert(model: AppModel)

    suspend fun delete(model: AppModel)
}
