package com.rosan.installer.di

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
}