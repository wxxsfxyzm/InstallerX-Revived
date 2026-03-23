// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.engine.parser.ApkParser
import com.rosan.installer.data.engine.parser.FileTypeDetector
import com.rosan.installer.data.engine.parser.UnifiedContainerAnalyser
import com.rosan.installer.data.engine.parser.strategy.ApkmStrategy
import com.rosan.installer.data.engine.parser.strategy.ApksStrategy
import com.rosan.installer.data.engine.parser.strategy.ModuleStrategy
import com.rosan.installer.data.engine.parser.strategy.MultiApkZipStrategy
import com.rosan.installer.data.engine.parser.strategy.SingleApkStrategy
import com.rosan.installer.data.engine.parser.strategy.XApkStrategy
import com.rosan.installer.data.engine.repository.AnalyserRepositoryImpl
import com.rosan.installer.data.engine.repository.AppIconRepositoryImpl
import com.rosan.installer.data.engine.repository.AppInstallerRepositoryImpl
import com.rosan.installer.data.engine.repository.ModuleInstallerRepositoryImpl
import com.rosan.installer.domain.engine.repository.AnalyserRepository
import com.rosan.installer.domain.engine.repository.AppIconRepository
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.engine.repository.ModuleInstallerRepository
import com.rosan.installer.domain.engine.usecase.AnalyzeInstallStateUseCase
import com.rosan.installer.domain.engine.usecase.AnalyzePackageUseCase
import com.rosan.installer.domain.engine.usecase.ApproveSessionUseCase
import com.rosan.installer.domain.engine.usecase.ClearAppIconCacheUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.engine.usecase.GetSessionConfirmationDetailsUseCase
import com.rosan.installer.domain.engine.usecase.ProcessInstallationUseCase
import com.rosan.installer.domain.engine.usecase.ProcessUninstallUseCase
import com.rosan.installer.domain.engine.usecase.SelectOptimalSplitsUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val engineModule = module {
    // Parser
    singleOf(::ApkParser)
    // Parser Tools
    singleOf(::FileTypeDetector)
    // Strategies
    singleOf(::SingleApkStrategy)
    singleOf(::MultiApkZipStrategy)
    singleOf(::ApksStrategy)
    singleOf(::XApkStrategy)
    singleOf(::ApkmStrategy)
    singleOf(::ModuleStrategy)
    // Unified Analyzer
    singleOf(::UnifiedContainerAnalyser)

    // Repositories
    singleOf(::AppIconRepositoryImpl) { bind<AppIconRepository>() }
    singleOf(::AnalyserRepositoryImpl) { bind<AnalyserRepository>() }
    singleOf(::AppInstallerRepositoryImpl) { bind<AppInstallerRepository>() }
    singleOf(::ModuleInstallerRepositoryImpl) { bind<ModuleInstallerRepository>() }

    // UseCases
    factoryOf(::AnalyzeInstallStateUseCase)
    factoryOf(::AnalyzePackageUseCase)
    factoryOf(::ApproveSessionUseCase)
    factoryOf(::GetSessionConfirmationDetailsUseCase)
    factoryOf(::ProcessInstallationUseCase)
    factoryOf(::ProcessUninstallUseCase)
    factoryOf(::SelectOptimalSplitsUseCase)
    factoryOf(::GetAppIconUseCase)
    factoryOf(::GetAppIconColorUseCase)
    factoryOf(::ClearAppIconCacheUseCase)
}
