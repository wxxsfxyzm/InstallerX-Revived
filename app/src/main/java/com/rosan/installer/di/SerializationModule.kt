// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.dsl.module

val serializationModule = module {
    single {
        SerializersModule {
        }
    }

    single {
        Json {
            serializersModule = get()
            ignoreUnknownKeys = true
        }
    }
}