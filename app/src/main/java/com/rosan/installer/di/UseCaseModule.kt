// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.domain.settings.usecase.config.GetConfigDraftUseCase
import com.rosan.installer.domain.settings.usecase.config.SaveConfigUseCase
import com.rosan.installer.domain.settings.usecase.settings.PerformAppUpdateUseCase
import com.rosan.installer.domain.settings.usecase.settings.SetLauncherIconUseCase
import com.rosan.installer.domain.settings.usecase.settings.ToggleUninstallFlagUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetConfigDraftUseCase(get(), get()) }
    factory { SaveConfigUseCase(get()) }
    factory { ToggleUninstallFlagUseCase(get()) }
    factory { PerformAppUpdateUseCase(get()) }
    factory { SetLauncherIconUseCase(get(), get()) }
}
