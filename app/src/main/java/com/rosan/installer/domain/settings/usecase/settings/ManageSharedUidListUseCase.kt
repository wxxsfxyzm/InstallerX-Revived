// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.settings.model.SharedUid
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import kotlinx.coroutines.flow.firstOrNull

class ManageSharedUidListUseCase(
    private val appSettingsRepo: AppSettingsRepository
) {
    /**
     * Adds a SharedUid item to the specified list setting.
     */
    suspend fun addUid(setting: SharedUidListSetting, uid: SharedUid) {
        val currentList = appSettingsRepo.getSharedUidList(setting).firstOrNull()?.toMutableList() ?: mutableListOf()
        if (!currentList.contains(uid)) {
            currentList.add(uid)
            appSettingsRepo.putSharedUidList(setting, currentList)
        }
    }

    /**
     * Removes a SharedUid item from the specified list setting.
     */
    suspend fun removeUid(setting: SharedUidListSetting, uid: SharedUid) {
        val currentList = appSettingsRepo.getSharedUidList(setting).firstOrNull()?.toMutableList() ?: return
        if (currentList.remove(uid)) {
            appSettingsRepo.putSharedUidList(setting, currentList)
        }
    }

    /**
     * Reorders a SharedUid item within the specified list setting.
     */
    suspend fun moveUid(setting: SharedUidListSetting, fromIndex: Int, toIndex: Int) {
        // Skip unnecessary operations if the item is dropped at its original position
        if (fromIndex == toIndex) return

        val currentList = appSettingsRepo.getSharedUidList(setting).firstOrNull()?.toMutableList() ?: return

        // Validate indices to prevent IndexOutOfBoundsException during fast UI interactions
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            appSettingsRepo.putSharedUidList(setting, currentList)
        }
    }
}
