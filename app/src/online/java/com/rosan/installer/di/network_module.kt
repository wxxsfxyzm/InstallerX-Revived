package com.rosan.installer.di

import com.rosan.installer.data.installer.model.impl.installer.helper.OkHttpNetworkResolver
import com.rosan.installer.data.installer.repo.NetworkResolver
import com.rosan.installer.data.updater.model.impl.OnlineAppUpdater
import com.rosan.installer.data.updater.model.impl.OnlineUpdateChecker
import com.rosan.installer.data.updater.repo.AppUpdater
import com.rosan.installer.data.updater.repo.UpdateChecker
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
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

    single<NetworkResolver> { OkHttpNetworkResolver() }
}

val updateModule = module {
    single<UpdateChecker> { OnlineUpdateChecker(get(), get(), get()) }
    single<AppUpdater> { OnlineAppUpdater(get(), get()) }
}