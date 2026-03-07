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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val engineModule = module {
    // Repositories
    singleOf(::AppIconRepositoryImpl) { bind<AppIconRepository>() }
    singleOf(::AnalyserRepositoryImpl) { bind<AnalyserRepository>() }
    singleOf(::InstallerRepositoryImpl) { bind<InstallerRepository>() }
    singleOf(::ModuleInstallerRepositoryImpl) { bind<ModuleInstallerRepository>() }

    // UseCases
    factoryOf(::SelectOptimalSplitsUseCase)
    factoryOf(::AnalyzePackageUseCase)
    factoryOf(::ExecuteInstallUseCase)
}
