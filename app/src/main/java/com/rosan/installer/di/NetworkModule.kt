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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private const val CONNECTION_TIMEOUT = 15L

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)       // Connection Timeout
            .readTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)          // Read Timeout
            .writeTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)         // Write Timeout
            .followRedirects(true)                             // Allow Redirect
            .followSslRedirects(true)  // Allow SSL Redirect
            .connectionSpecs(
                listOf(
                    ConnectionSpec.MODERN_TLS,
                    // ConnectionSpec.CLEARTEXT  // Allow HTTP
                )
            )
            .build()
    }

    singleOf(::OkHttpNetworkResolver) { bind<NetworkResolver>() }
}

val updateModule = module {
    // Data Layer implementations
    singleOf(::OnlineUpdateRepositoryImpl) { bind<UpdateRepository>() }
    singleOf(::InAppInstallProviderImpl) { bind<InAppInstallProvider>() }

    // Domain Layer UseCases
    factoryOf(::PerformAppUpdateUseCase)
}
