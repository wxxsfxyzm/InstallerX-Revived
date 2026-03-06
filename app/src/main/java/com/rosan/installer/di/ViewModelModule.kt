// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.di

import androidx.navigation.NavController
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { (installer: InstallerRepo) ->
        InstallerViewModel(installer, get(), get())
    }

    viewModel {
        PreferredViewModel(
            appSettingsRepo = get(),
            updateChecker = get(),
            systemEnvProvider = get(),
            privilegedProvider = get(),
            toggleUninstallFlagUseCase = get(),
            performAppUpdateUseCase = get(),
            setLauncherIconUseCase = get()
        )
    }

    viewModel { (navController: NavController) ->
        AllViewModel(navController, get(), get())
    }

    viewModel { (id: Long?) ->
        EditViewModel(get(), get(), get(), id)
    }

    viewModel { (id: Long) ->
        ApplyViewModel(get(), get(), id, get())
    }
}