// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.engine.repository.AnalyserRepositoryImpl
import com.rosan.installer.data.engine.repository.AppIconRepositoryImpl
import com.rosan.installer.domain.engine.repository.AnalyserRepository
import com.rosan.installer.domain.engine.repository.AppIconRepository
import com.rosan.installer.domain.engine.usecase.AnalyzePackageUseCase
import com.rosan.installer.domain.engine.usecase.ExecuteInstallUseCase
import com.rosan.installer.domain.engine.usecase.SelectOptimalSplitsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val engineModule = module {
    // Repositories

    // 图标与颜色提取仓库 (记得根据你实际的构造函数调整 get() 的数量)
    single<AppIconRepository> { AppIconRepositoryImpl(androidContext()) }

    // APK/ZIP 解析仓库
    single<AnalyserRepository> { AnalyserRepositoryImpl(get()) }

    // 注意：如果你的 InstallerRepositoryImpl 是 class 而不是 object，也在这里注册
    // single<InstallerRepository> { InstallerRepositoryImpl(...) }

    // UseCases
    factory { SelectOptimalSplitsUseCase() }
    factory { AnalyzePackageUseCase(get(), get(), get()) }
    factory { ExecuteInstallUseCase(get()) }
}