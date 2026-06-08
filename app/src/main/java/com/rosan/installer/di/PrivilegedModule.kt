// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import com.rosan.installer.framework.privileged.provider.AppOpsProviderImpl
import com.rosan.installer.framework.privileged.provider.ComponentOpsProviderImpl
import com.rosan.installer.framework.privileged.provider.PermissionProviderImpl
import com.rosan.installer.framework.privileged.provider.PostInstallTaskProviderImpl
import com.rosan.installer.framework.privileged.provider.SessionDetailsProviderImpl
import com.rosan.installer.framework.privileged.provider.ShellExecutionProviderImpl
import com.rosan.installer.framework.privileged.provider.SystemInfoProviderImpl
import com.rosan.installer.framework.privileged.lifecycle.RecyclerManager
import com.rosan.installer.framework.privileged.recycler.AppProcessRecycler
import com.rosan.installer.framework.privileged.recycler.ProcessHookRecycler
import com.rosan.installer.framework.privileged.recycler.ProcessUserServiceRecycler
import com.rosan.installer.framework.privileged.recycler.ShizukuHookRecycler
import com.rosan.installer.framework.privileged.recycler.ShizukuUserServiceRecycler
import com.rosan.installer.framework.service.AutoLockService
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.engine.provider.SessionDetailsProvider
import com.rosan.installer.domain.privileged.provider.PermissionProvider
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.privileged.provider.ShellExecutionProvider
import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.domain.privileged.usecase.OpenAppUseCase
import com.rosan.installer.domain.privileged.usecase.OpenLSPosedUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

object RecyclerNames {
    val APP_PROCESS = named("AppProcessManager")
    val SYSTEM_UID_APP_PROCESS = named("SystemUidAppProcessManager")
    val USER_SERVICE = named("ProcessUserServiceManager")
    val SYSTEM_UID_USER_SERVICE = named("SystemUidProcessUserServiceManager")
    val SYSTEM_UID_SHIZUKU_USER_SERVICE = named("SystemUidShizukuUserService")
}

val privilegedModule = module {
    // Providers
    singleOf(::AppOpsProviderImpl) { bind<AppOpsProvider>() }
    singleOf(::ComponentOpsProviderImpl) { bind<ComponentOpsProvider>() }
    singleOf(::PermissionProviderImpl) { bind<PermissionProvider>() }
    singleOf(::ShellExecutionProviderImpl) { bind<ShellExecutionProvider>() }
    singleOf(::PostInstallTaskProviderImpl) { bind<PostInstallTaskProvider>() }
    singleOf(::SystemInfoProviderImpl) { bind<SystemInfoProvider>() }
    singleOf(::SessionDetailsProviderImpl) { bind<SessionDetailsProvider>() }

    // Services
    singleOf(::AutoLockService)

    // UseCases
    factoryOf(::OpenAppUseCase)
    factoryOf(::OpenLSPosedUseCase)
    factoryOf(::GetAvailableUsersUseCase)

    // Recycler Infrastructure

    // 1. Recycler Managers (Singletons)
    // Add named qualifier to distinguish this manager
    single(RecyclerNames.APP_PROCESS) {
        RecyclerManager<String, AppProcessRecycler> { shell ->
            AppProcessRecycler(shell)
        }
    }

    single(RecyclerNames.SYSTEM_UID_APP_PROCESS) {
        RecyclerManager<String, AppProcessRecycler> { shell ->
            AppProcessRecycler(shell)
        }
    }

    // Add named qualifier to distinguish this manager
    single(RecyclerNames.USER_SERVICE) {
        RecyclerManager<String, ProcessUserServiceRecycler> { shell ->
            ProcessUserServiceRecycler(
                shell = shell,
                context = get(),
                appProcessRecyclerManager = get(RecyclerNames.APP_PROCESS)
            )
        }
    }

    single(RecyclerNames.SYSTEM_UID_USER_SERVICE) {
        RecyclerManager<String, ProcessUserServiceRecycler> { shell ->
            ProcessUserServiceRecycler(
                shell = shell,
                context = get(),
                appProcessRecyclerManager = get(RecyclerNames.SYSTEM_UID_APP_PROCESS),
                serviceClass = ProcessUserServiceRecycler.SystemUidAppProcessService::class.java
            )
        }
    }

    // 2. Stateless / Permission-based Recyclers (Singletons)
    // Replaces the old 'object' declarations. Koin now manages their lifecycle.
    singleOf(::ShizukuUserServiceRecycler)
    single(RecyclerNames.SYSTEM_UID_SHIZUKU_USER_SERVICE) {
        ShizukuUserServiceRecycler(
            context = get(),
            serviceClass = ShizukuUserServiceRecycler.SystemUidShizukuUserService::class.java,
            processNameSuffix = "shizuku_system_privileged"
        )
    }
    singleOf(::ShizukuHookRecycler)

    // 3. Shell-dependent Recyclers (Factories)
    // Created dynamically on demand based on the requested shell.
    factory { (shell: String) ->
        ProcessHookRecycler(
            shell = shell,
            context = get(),
            appProcessRecyclerManager = get(RecyclerNames.APP_PROCESS)
        )
    }
}
