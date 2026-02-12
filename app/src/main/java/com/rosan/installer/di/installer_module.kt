// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.model.impl.InstallerSessionManager
import com.rosan.installer.data.installer.repo.InstallerRepo
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val installerModule = module {
    singleOf(::InstallerSessionManager)

    factory<InstallerRepo> { (id: String, onClose: () -> Unit) ->
        InstallerRepoImpl(
            id = id,
            context = androidContext(), // Inject Application Context automatically
            appDataStore = get(),       // Inject AppDataStore automatically
            iconColorExtractor = get(), // Inject IconColorExtractor automatically
            onClose = onClose
        )
    }
}