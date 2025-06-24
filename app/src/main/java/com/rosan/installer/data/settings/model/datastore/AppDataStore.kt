package com.rosan.installer.data.settings.model.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppDataStore(private val dataStore: DataStore<Preferences>) {

    suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    fun getString(key: String, default: String = ""): Flow<String> {
        return dataStore.data.map { it[stringPreferencesKey(key)] ?: default }
    }

    suspend fun putInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    fun getInt(key: String, default: Int = 0): Flow<Int> {
        return dataStore.data.map { it[intPreferencesKey(key)] ?: default }
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    fun getBoolean(key: String, default: Boolean = false): Flow<Boolean> {
        return dataStore.data.map { it[booleanPreferencesKey(key)] ?: default }
    }
}