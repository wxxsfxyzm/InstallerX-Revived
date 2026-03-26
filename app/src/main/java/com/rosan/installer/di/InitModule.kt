// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.util.cache.AutoCacheSweeper
import com.rosan.installer.util.timber.LogController
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val initModule = module {
    singleOf(::LogController) { createdAtStart() }
    singleOf(::AutoCacheSweeper) { createdAtStart() }
}
