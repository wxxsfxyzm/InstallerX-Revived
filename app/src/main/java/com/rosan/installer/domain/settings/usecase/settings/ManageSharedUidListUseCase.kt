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
    suspend fun addUid(setting: SharedUidListSetting, uid: SharedUid) {
        val currentList = appSettingsRepo.getSharedUidList(setting).firstOrNull()?.toMutableList() ?: mutableListOf()
        if (!currentList.contains(uid)) {
            currentList.add(uid)
            appSettingsRepo.putSharedUidList(setting, currentList)
        }
    }

    suspend fun removeUid(setting: SharedUidListSetting, uid: SharedUid) {
        val currentList = appSettingsRepo.getSharedUidList(setting).firstOrNull()?.toMutableList() ?: return
        if (currentList.remove(uid)) {
            appSettingsRepo.putSharedUidList(setting, currentList)
        }
    }
}
