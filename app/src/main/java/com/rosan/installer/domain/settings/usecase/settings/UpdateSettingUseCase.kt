// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.StringSetting

class UpdateSettingUseCase(
    private val appSettingsRepo: AppSettingsRepository
) {
    suspend operator fun invoke(setting: BooleanSetting, value: Boolean) {
        appSettingsRepo.putBoolean(setting, value)
    }

    suspend operator fun invoke(setting: StringSetting, value: String) {
        appSettingsRepo.putString(setting, value)
    }

    suspend operator fun invoke(setting: IntSetting, value: Int) {
        appSettingsRepo.putInt(setting, value)
    }
}
