// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AllViewModel)
    viewModelOf(::PreferredViewModel)

    viewModel { (installer: InstallerSessionRepository) ->
        InstallerViewModel(
            repo = installer,
            appSettingsRepo = get(),
            appIconRepo = get(),
            systemInfoProvider = get()
        )
    }

    viewModel { (id: Long) ->
        ApplyViewModel(get(), get(), get(), get(), id)
    }

    viewModel { (id: Long?) ->
        EditViewModel(get(), get(), get(), get(), id)
    }
}