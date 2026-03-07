// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.util.timber.LogController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val logModule = module {
    single(createdAtStart = true) {
        LogController(androidContext(), get()).apply { init() }
    }
}