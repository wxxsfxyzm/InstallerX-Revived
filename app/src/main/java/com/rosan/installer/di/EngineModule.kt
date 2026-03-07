// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.engine.repository.AnalyserRepositoryImpl
import com.rosan.installer.data.engine.repository.AppIconRepositoryImpl
import com.rosan.installer.data.engine.repository.InstallerRepositoryImpl
import com.rosan.installer.data.engine.repository.ModuleInstallerRepositoryImpl
import com.rosan.installer.domain.engine.repository.AnalyserRepository
import com.rosan.installer.domain.engine.repository.AppIconRepository
import com.rosan.installer.domain.engine.repository.InstallerRepository
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.engine.usecase.AnalyzePackageUseCase
import com.rosan.installer.domain.engine.usecase.ExecuteInstallUseCase
import com.rosan.installer.domain.engine.usecase.SelectOptimalSplitsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val engineModule = module {
    // Repositories
    single<AppIconRepository> { AppIconRepositoryImpl(androidContext()) }
    single<AnalyserRepository> { AnalyserRepositoryImpl(get()) }
    single<InstallerRepository> { InstallerRepositoryImpl(get(), get(), get(), get()) }
    single<ModuleInstallerRepository> { ModuleInstallerRepositoryImpl(get()) }

    // UseCases
    factory { SelectOptimalSplitsUseCase() }
    factory { AnalyzePackageUseCase(get(), get(), get()) }
    factory { ExecuteInstallUseCase(get()) }
}
