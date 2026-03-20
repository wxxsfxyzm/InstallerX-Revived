// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.data.privileged.provider.AppOpsProviderImpl
import com.rosan.installer.data.privileged.provider.ComponentOpsProviderImpl
import com.rosan.installer.data.privileged.provider.PermissionProviderImpl
import com.rosan.installer.data.privileged.provider.PostInstallTaskProviderImpl
import com.rosan.installer.data.privileged.provider.ShellExecutionProviderImpl
import com.rosan.installer.data.privileged.provider.SystemInfoProviderImpl
import com.rosan.installer.data.privileged.service.AutoLockService
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.privileged.provider.PermissionProvider
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.privileged.provider.ShellExecutionProvider
import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.privileged.usecase.OpenLSPosedUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val privilegedModule = module {
    // Providers
    singleOf(::AppOpsProviderImpl) { bind<AppOpsProvider>() }
    singleOf(::ComponentOpsProviderImpl) { bind<ComponentOpsProvider>() }
    singleOf(::PermissionProviderImpl) { bind<PermissionProvider>() }
    singleOf(::ShellExecutionProviderImpl) { bind<ShellExecutionProvider>() }
    singleOf(::PostInstallTaskProviderImpl) { bind<PostInstallTaskProvider>() }
    singleOf(::SystemInfoProviderImpl) { bind<SystemInfoProvider>() }

    // Services
    singleOf(::AutoLockService)

    // UseCases
    factoryOf(::OpenAppUseCase)
    factoryOf(::OpenLSPosedUseCase)
}