// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting

class SetLauncherIconUseCase(
    private val appSettingsRepo: AppSettingsRepository,
    private val systemEnvProvider: SystemEnvProvider
) {
    suspend operator fun invoke(show: Boolean) {
        appSettingsRepo.putBoolean(BooleanSetting.ShowLauncherIcon, show)
        systemEnvProvider.setLauncherAliasEnabled(show)
    }
}
