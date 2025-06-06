package com.rosan.installer.di

import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val datastoreModule = module {
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

    single {
        AppDataStore(get())
    }
}