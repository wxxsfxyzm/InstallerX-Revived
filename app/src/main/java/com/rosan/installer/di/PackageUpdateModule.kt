// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rosan.installer.data.packageupdate.repository.SelfUpdateRecoveryRepositoryImpl
import com.rosan.installer.domain.packageupdate.repository.SelfUpdateRecoveryRepository
import com.rosan.installer.framework.packageupdate.SelfUpdateRecoveryManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val selfUpdateRecoveryDataStore = named("SelfUpdateRecoveryDataStore")

val packageUpdateModule = module {
    // Keep transient recovery state separate from user settings and settings backup/export.
    single<DataStore<Preferences>>(selfUpdateRecoveryDataStore) {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("self_update_recovery")
        }
    }

    single<SelfUpdateRecoveryRepository> {
        SelfUpdateRecoveryRepositoryImpl(get(selfUpdateRecoveryDataStore))
    }

    single {
        SelfUpdateRecoveryManager(
            context = androidContext(),
            recoveryRepository = get(),
            postInstallTaskProvider = get()
        )
    }
}
