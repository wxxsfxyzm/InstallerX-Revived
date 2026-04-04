// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.session.manager.InstallerSessionManager
import com.rosan.installer.data.session.repository.InstallerSessionRepositoryImpl
import com.rosan.installer.data.session.resolver.ConfigResolver
import com.rosan.installer.domain.notification.SessionNotifier
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.framework.notification.SessionNotifierImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val installerModule = module {
    singleOf(::InstallerSessionManager)

    factoryOf(::SessionNotifierImpl) { bind<SessionNotifier>() }
    factory<InstallerSessionRepository> { (id: String, onClose: () -> Unit) ->
        InstallerSessionRepositoryImpl(
            id = id,
            onClose = onClose
        )
    }
    factoryOf(::ConfigResolver)
}