// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rosan.installer.data.settings.local.datastore.AppDataStore
import com.rosan.installer.data.settings.local.room.InstallerRoom
import com.rosan.installer.data.settings.provider.PrivilegedProviderImpl
import com.rosan.installer.data.settings.provider.SystemAppProviderImpl
import com.rosan.installer.data.settings.provider.SystemEnvProviderImpl
import com.rosan.installer.data.settings.repository.AppRepositoryImpl
import com.rosan.installer.data.settings.repository.AppSettingsRepoImpl
import com.rosan.installer.data.settings.repository.ConfigRepoImpl
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.provider.SystemAppProvider
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.provider.ThemeStateProvider
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.ConfigRepo
import com.rosan.installer.domain.settings.usecase.config.GetConfigDraftUseCase
import com.rosan.installer.domain.settings.usecase.config.GetResolvedConfigUseCase
import com.rosan.installer.domain.settings.usecase.config.SaveConfigUseCase
import com.rosan.installer.domain.settings.usecase.config.ToggleAppTargetConfigUseCase
import com.rosan.installer.domain.settings.usecase.settings.GetPackageUidUseCase
import com.rosan.installer.domain.settings.usecase.settings.ManagePackageListUseCase
import com.rosan.installer.domain.settings.usecase.settings.ManageSharedUidListUseCase
import com.rosan.installer.domain.settings.usecase.settings.SetLauncherIconUseCase
import com.rosan.installer.domain.settings.usecase.settings.ToggleUninstallFlagUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsModule = module {
    // Provide a global coroutineScope
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    // Room
    single { InstallerRoom.createInstance() }

    single { get<InstallerRoom>().appDao }
    single { get<InstallerRoom>().configDao }

    singleOf(::AppRepositoryImpl) { bind<AppRepository>() }
    singleOf(::ConfigRepoImpl) { bind<ConfigRepo>() }

    // DataStore
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            migrations = listOf(
                SharedPreferencesMigration(androidContext(), "app")
            )
        ) {
            androidContext().preferencesDataStoreFile("app_settings")
        }
    }

    singleOf(::AppDataStore)

    singleOf(::AppSettingsRepoImpl) { bind<AppSettingsRepo>() }

    // Providers
    singleOf(::SystemEnvProviderImpl) { bind<SystemEnvProvider>() }
    singleOf(::SystemAppProviderImpl) { bind<SystemAppProvider>() }
    singleOf(::PrivilegedProviderImpl) { bind<PrivilegedProvider>() }

    singleOf(::ThemeStateProvider)

    // UseCases
    factoryOf(::GetResolvedConfigUseCase)
    factoryOf(::GetConfigDraftUseCase)
    factoryOf(::SaveConfigUseCase)
    factoryOf(::UpdateSettingUseCase)
    factoryOf(::ToggleUninstallFlagUseCase)
    factoryOf(::SetLauncherIconUseCase)
    factoryOf(::ToggleAppTargetConfigUseCase)
    factoryOf(::ManagePackageListUseCase)
    factoryOf(::ManageSharedUidListUseCase)
    factoryOf(::GetPackageUidUseCase)
}
