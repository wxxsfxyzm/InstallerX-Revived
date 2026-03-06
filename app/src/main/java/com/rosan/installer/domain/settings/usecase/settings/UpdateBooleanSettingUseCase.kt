// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting

class UpdateBooleanSettingUseCase(private val appSettingsRepo: AppSettingsRepo) {
    suspend operator fun invoke(setting: BooleanSetting, value: Boolean) {
        // 可以在这里统一拦截、打日志等
        appSettingsRepo.putBoolean(setting, value)
    }
}
