// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import kotlinx.coroutines.flow.first

class ManagePackageListUseCase(
    private val appSettingsRepo: AppSettingsRepository
) {
    /**
     * Adds a NamedPackage item to the specified list setting.
     */
    suspend fun addPackage(setting: NamedPackageListSetting, pkg: NamedPackage) {
        val currentList = appSettingsRepo.getNamedPackageList(setting).first().toMutableList()
        if (!currentList.contains(pkg)) {
            currentList.add(pkg)
            appSettingsRepo.putNamedPackageList(setting, currentList)
        }
    }

    /**
     * Removes a NamedPackage item from the specified list setting.
     */
    suspend fun removePackage(setting: NamedPackageListSetting, pkg: NamedPackage) {
        val currentList = appSettingsRepo.getNamedPackageList(setting).first().toMutableList()
        if (currentList.remove(pkg)) {
            appSettingsRepo.putNamedPackageList(setting, currentList)
        }
    }

    /**
     * Reorders a NamedPackage item within the specified list setting.
     */
    suspend fun movePackage(setting: NamedPackageListSetting, fromIndex: Int, toIndex: Int) {
        // Skip unnecessary operations if the item is dropped at its original position
        if (fromIndex == toIndex) return

        val currentList = appSettingsRepo.getNamedPackageList(setting).first().toMutableList()

        // Validate indices to prevent IndexOutOfBoundsException during fast UI interactions
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            appSettingsRepo.putNamedPackageList(setting, currentList)
        }
    }
}
