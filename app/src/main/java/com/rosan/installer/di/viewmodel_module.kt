package com.rosan.installer.di

import androidx.navigation.NavController
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.page.main.installer.dialog.DialogViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { (installer: InstallerRepo) ->
        DialogViewModel(installer, get(), get(), get())
    }

    viewModel {
        PreferredViewModel(get(), get())
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