// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.session.resolver.OkHttpNetworkResolver
import com.rosan.installer.data.updater.provider.InAppInstallProviderImpl
import com.rosan.installer.data.updater.repository.OnlineUpdateRepositoryImpl
import com.rosan.installer.domain.session.repository.NetworkResolver
import com.rosan.installer.domain.updater.provider.InAppInstallProvider
import com.rosan.installer.domain.updater.repository.UpdateRepository
import com.rosan.installer.domain.updater.usecase.PerformAppUpdateUseCase
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS) // Connection Timeout
            .readTimeout(15, TimeUnit.SECONDS)    // Read Timeout
            .writeTimeout(15, TimeUnit.SECONDS)   // Write Timeout
            .followRedirects(true)                 // Allow Redirect
            .followSslRedirects(true)       // Allow SSL Redirect
            .connectionSpecs(
                listOf(
                    ConnectionSpec.MODERN_TLS,
                    // ConnectionSpec.CLEARTEXT  // Allow HTTP
                )
            )
            .build()
    }

    single<NetworkResolver> {
        OkHttpNetworkResolver(
            context = androidContext(),
            okHttpClient = get(),
            appSettingsRepo = get()
        )
    }
}

val updateModule = module {
    // Data Layer implementations
    single<UpdateRepository> { OnlineUpdateRepositoryImpl(get(), get(), get()) }
    single<InAppInstallProvider> { InAppInstallProviderImpl(get(), get()) }

    // Domain Layer UseCases
    factory { PerformAppUpdateUseCase(get(), get()) }
}
