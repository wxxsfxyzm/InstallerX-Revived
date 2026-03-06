// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.settings.provider.PrivilegedProviderImpl
import com.rosan.installer.data.settings.provider.SystemEnvProviderImpl
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val providerModule = module {
    single<SystemEnvProvider> { SystemEnvProviderImpl(androidContext()) }
    single<PrivilegedProvider> { PrivilegedProviderImpl(androidContext()) }
}
