// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.domain.settings.repository

import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.util.ConfigOrder
import com.rosan.installer.domain.settings.util.OrderType
import kotlinx.coroutines.flow.Flow

// Repository interface in domain layer
interface ConfigRepo {
    suspend fun all(order: ConfigOrder = ConfigOrder.Id(OrderType.Ascending)): List<ConfigModel>

    fun flowAll(order: ConfigOrder = ConfigOrder.Id(OrderType.Ascending)): Flow<List<ConfigModel>>

    suspend fun find(id: Long): ConfigModel?

    fun flowFind(id: Long): Flow<ConfigModel?>

    suspend fun update(model: ConfigModel)

    suspend fun insert(model: ConfigModel)

    suspend fun delete(model: ConfigModel)
}