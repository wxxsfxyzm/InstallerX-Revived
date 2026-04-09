// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SettingsSharedViewModel)
    viewModelOf(::AllViewModel)
    viewModelOf(::PreferredViewModel)
    viewModelOf(::ThemeSettingsViewModel)
    viewModelOf(::InstallerSettingsViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::UninstallerSettingsViewModel)
    viewModelOf(::LabSettingsViewModel)
    viewModelOf(::AboutViewModel)

    viewModel { (session: InstallerSessionRepository) ->
        InstallerViewModel(
            session = session,
            get(),
            get(),
            get(),
            get()
        )
    }

    viewModel { (id: Long) ->
        ApplyViewModel(
            id = id,
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    viewModel { (id: Long?) ->
        EditViewModel(
            id = id,
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}