// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.repository.AppRepository

class ToggleAppTargetConfigUseCase(
    private val appRepo: AppRepository
) {
    suspend operator fun invoke(packageName: String, configId: Long, applied: Boolean) {
        val model = appRepo.findByPackageName(packageName)

        if (applied) {
            if (model != null) {
                appRepo.update(model.copy(configId = configId))
            } else {
                appRepo.insert(
                    AppModel(
                        id = 0L,
                        packageName = packageName,
                        configId = configId,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis()
                    )
                )
            }
        } else {
            model?.let { appRepo.delete(it) }
        }
    }
}
