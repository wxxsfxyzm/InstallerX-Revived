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
    companion object {
        // ForegroundInfoHandler
        val SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION = booleanPreferencesKey("show_dialog_when_pressing_notification")

        // ConfigUtil
        val AUTHORIZER = stringPreferencesKey("authorizer")
        val CUSTOMIZE_AUTHORIZER = stringPreferencesKey("customize_authorizer")
        val INSTALL_MODE = stringPreferencesKey("install_mode")

        // ApplyViewModel
        val APPLY_ORDER_TYPE = stringPreferencesKey("apply_order_type")
        val APPLY_ORDER_IN_REVERSE = booleanPreferencesKey("apply_order_in_reverse")
        val APPLY_SELECTED_FIRST = booleanPreferencesKey("apply_selected_first")
        val APPLY_SHOW_SYSTEM_APP = booleanPreferencesKey("apply_show_system_app")
        val APPLY_SHOW_PACKAGE_NAME = booleanPreferencesKey("apply_show_package_name")

        // DialogViewModel
        val DIALOG_AUTO_CLOSE_COUNTDOWN = intPreferencesKey("show_dhizuku_auto_close_count_down_menu")
        val DIALOG_SHOW_EXTENDED_MENU = booleanPreferencesKey("show_dialog_install_extended_menu")
        val DIALOG_SHOW_INTELLIGENT_SUGGESTION = booleanPreferencesKey("show_dialog_install_intelligent_suggestion")
        val DIALOG_DISABLE_NOTIFICATION_ON_DISMISS =
            booleanPreferencesKey("show_disable_notification_for_dialog_install")
    }

    suspend fun putString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> {
        return dataStore.data.map { it[key] ?: default }
    }

    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    fun getInt(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> {
        return dataStore.data.map { it[key] ?: default }
    }

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> {
        return dataStore.data.map { it[key] ?: default }
    }
}