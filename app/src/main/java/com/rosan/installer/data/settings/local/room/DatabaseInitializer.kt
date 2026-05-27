// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room

import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.ConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DatabaseInitializer(
    private val configRepository: ConfigRepository,
    appScope: CoroutineScope
) {
    init {
        appScope.launch {
            configRepository.findDefault() ?: configRepository.insert(ConfigModel.generateOptimalDefault())
        }
    }
}