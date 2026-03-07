// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.di

import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import com.rosan.installer.domain.settings.usecase.config.SaveConfigUseCase
import com.rosan.installer.domain.settings.usecase.config.ToggleAppTargetConfigUseCase
import com.rosan.installer.domain.settings.usecase.settings.ManagePackageListUseCase
import com.rosan.installer.domain.settings.usecase.settings.ManageSharedUidListUseCase
import com.rosan.installer.domain.settings.usecase.settings.SetLauncherIconUseCase
import com.rosan.installer.domain.settings.usecase.settings.ToggleUninstallFlagUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsModule = module {
    // Room Database Injection
    single { InstallerRoom.createInstance() }

    single<AppRepository> {
        val roomDatabase: InstallerRoom = get()
        AppRepositoryImpl(roomDatabase.appDao)
    }

    single<ConfigRepo> {
        val roomDatabase: InstallerRoom = get()
        ConfigRepoImpl(roomDatabase.configDao)
    }

    // DataStore Injection
    single {
        PreferenceDataStoreFactory.create(
            migrations = listOf(
                // Migration from SharedPreferences to DataStore
                SharedPreferencesMigration(androidContext(), "app")
            )
        ) {
            androidContext().preferencesDataStoreFile("app_settings")
        }
    }

    single { AppDataStore(get(), get()) }

    single<AppSettingsRepo> { AppSettingsRepoImpl(get()) }

    // Providers
    single<SystemEnvProvider> { SystemEnvProviderImpl(androidContext()) }
    single<SystemAppProvider> { SystemAppProviderImpl(androidContext()) }
    single<PrivilegedProvider> { PrivilegedProviderImpl(androidContext(), get()) }
    single { ThemeStateProvider(get()) }

    // UseCases
    factory { GetConfigDraftUseCase(get(), get()) }
    factory { SaveConfigUseCase(get()) }
    factory { UpdateSettingUseCase(get()) }
    factory { ToggleUninstallFlagUseCase(get()) }
    factory { SetLauncherIconUseCase(get(), get()) }
    factory { ToggleAppTargetConfigUseCase(get()) }
    factory { ManagePackageListUseCase(get()) }
    factory { ManageSharedUidListUseCase(get()) }
}
