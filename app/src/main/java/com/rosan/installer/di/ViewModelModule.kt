// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.di

import androidx.navigation.NavController
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { (installer: InstallerSessionRepository) ->
        InstallerViewModel(
            repo = installer,
            appSettingsRepo = get(),
            appIconRepo = get(),
            systemInfoProvider = get()
        )
    }

    viewModel {
        PreferredViewModel(
            appSettingsRepo = get(),
            updateRepo = get(),
            systemEnvProvider = get(),
            privilegedProvider = get(),
            toggleUninstallFlagUseCase = get(),
            performAppUpdateUseCase = get(),
            setLauncherIconUseCase = get()
        )
    }

    viewModel { (navController: NavController) ->
        AllViewModel(
            navController = navController,
            repo = get(),
            appSettingsRepo = get()
        )
    }

    viewModel { (id: Long?) ->
        EditViewModel(
            appSettingsRepo = get(),
            getConfigDraftUseCase = get(),
            saveConfigUseCase = get(),
            systemInfoProvider = get(),
            id = id
        )
    }

    viewModel { (id: Long) ->
        ApplyViewModel(
            configRepo = get(),
            appRepo = get(),
            id = id,
            appSettingsRepo = get()
        )
    }
}