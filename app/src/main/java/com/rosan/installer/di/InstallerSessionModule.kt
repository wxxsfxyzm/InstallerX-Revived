// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.session.manager.InstallerSessionManager
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val installerModule = module {
    singleOf(::InstallerSessionManager)

    factory<InstallerSessionRepository> { (id: String, onClose: () -> Unit) ->
        InstallerSessionRepositoryImpl(
            id = id,
            onClose = onClose
        )
    }
}