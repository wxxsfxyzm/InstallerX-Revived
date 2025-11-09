package com.rosan.installer.data.settings.model.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

class AppDataStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    companion object {
        // UI Fresh Switch
        val UI_EXPRESSIVE_SWITCH = booleanPreferencesKey("ui_fresh_switch")
        val UI_USE_MIUIX = booleanPreferencesKey("ui_use_miui_x")

        // Show Live Activity
        val SHOW_LIVE_ACTIVITY = booleanPreferencesKey("show_live_activity")

        // Show Launcher Icon
        val SHOW_LAUNCHER_ICON = booleanPreferencesKey("show_launcher_icon")

        // Show System Icon for Packages
        val PREFER_SYSTEM_ICON_FOR_INSTALL = booleanPreferencesKey("prefer_system_icon_for_updates")

        // ForegroundInfoHandler
        val SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION = booleanPreferencesKey("show_dialog_when_pressing_notification")

        // Auto Lock Installer
        val AUTO_LOCK_INSTALLER = booleanPreferencesKey("auto_lock_installer")

        // ConfigUtil
        val AUTHORIZER = stringPreferencesKey("authorizer")
        val CUSTOMIZE_AUTHORIZER = stringPreferencesKey("customize_authorizer")
        val INSTALL_MODE = stringPreferencesKey("install_mode")

        // ApplyViewModel
        val USER_READ_SCOPE_TIPS = booleanPreferencesKey("user_read_scope_tips")
        val APPLY_ORDER_TYPE = stringPreferencesKey("apply_order_type")
        val APPLY_ORDER_IN_REVERSE = booleanPreferencesKey("apply_order_in_reverse")
        val APPLY_SELECTED_FIRST = booleanPreferencesKey("apply_selected_first")
        val APPLY_SHOW_SYSTEM_APP = booleanPreferencesKey("apply_show_system_app")
        val APPLY_SHOW_PACKAGE_NAME = booleanPreferencesKey("apply_show_package_name")

        // DialogViewModel
        val DIALOG_VERSION_COMPARE_SINGLE_LINE =
            booleanPreferencesKey("show_dialog_version_compare_single_line")
        val DIALOG_SDK_COMPARE_MULTI_LINE =
            booleanPreferencesKey("show_dialog_sdk_compare_multi_line")
        val DIALOG_AUTO_CLOSE_COUNTDOWN =
            intPreferencesKey("show_dhizuku_auto_close_count_down_menu")
        val DIALOG_SHOW_EXTENDED_MENU =
            booleanPreferencesKey("show_dialog_install_extended_menu")
        val DIALOG_SHOW_INTELLIGENT_SUGGESTION =
            booleanPreferencesKey("show_dialog_install_intelligent_suggestion")
        val DIALOG_DISABLE_NOTIFICATION_ON_DISMISS =
            booleanPreferencesKey("show_disable_notification_for_dialog_install")
        val DIALOG_SHOW_OPPO_SPECIAL =
            booleanPreferencesKey("show_oppo_special")
        val DIALOG_AUTO_SILENT_INSTALL =
            booleanPreferencesKey("auto_silent_install")

        // Customize Installer
        val MANAGED_INSTALLER_PACKAGES_LIST =
            stringPreferencesKey("managed_packages_list")
        val MANAGED_BLACKLIST_PACKAGES_LIST =
            stringPreferencesKey("managed_blacklist_packages_list")
        val MANAGED_SHARED_USER_ID_BLACKLIST =
            stringPreferencesKey("managed_shared_user_id_blacklist")
        val MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST =
            stringPreferencesKey("managed_shared_user_id_blacklist_exempted_packages_list")

        // Lab
        val LAB_USE_SHIZUKU_HOOK_MODE = booleanPreferencesKey("use_shizuku_hook_mode")
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

    /**
     * Saves a list of NamedPackage objects to DataStore after converting it to a JSON string.
     * @param key The Preferences.Key<String> to save the list under.
     * @param packages The list of packages to save.
     * @return A Flow emitting the list of packages. Returns an empty list if no data or on error.
     */
    suspend fun putNamedPackageList(key: Preferences.Key<String>, packages: List<NamedPackage>) =
        // Use json.encodeToString to serialize the list
        putString(key, json.encodeToString(packages))

    /**
     * Retrieves a Flow of a list of NamedPackage objects from DataStore.
     * It reads the JSON string and deserializes it.
     *
     * @param key The Preferences.Key<String> to read from DataStore.
     * @param default The default list of packages to return if no data is found.
     * @return A Flow emitting the list of packages. Returns an empty list if no data or on error.
     */
    fun getNamedPackageList(
        key: Preferences.Key<String>,
        default: List<NamedPackage> = emptyList()
    ): Flow<List<NamedPackage>> =
        getString(key, json.encodeToString(default)).map { jsonString ->
            try {
                // Use json.decodeFromString to deserialize the string back into a list
                json.decodeFromString<List<NamedPackage>>(jsonString)
            } catch (e: Exception) {
                // In case of a parsing error, return an empty list
                Timber.e(
                    e,
                    "Failed to decode NamedPackage list from DataStore. Returning empty list."
                )
                emptyList()
            }
        }

    /**
     * Saves a list of SharedUid objects to DataStore after converting it to a JSON string.
     * @param uids The list of Shared UIDs to save.
     * @param key The Preferences.Key<String> to save the list under.
     * @return A Flow emitting the list of packages. Returns an empty list if no data or on error.
     */
    suspend fun putSharedUidList(key: Preferences.Key<String>, uids: List<SharedUid>) =
        putString(key, json.encodeToString(uids))

    /**
     * Retrieves a Flow of a list of SharedUid objects from DataStore.
     * It reads the JSON string and deserializes it.
     * @param key The Preferences.Key<String> to read from DataStore.
     * @return A Flow emitting the list of packages. Returns an empty list if no data or on error.
     */
    fun getSharedUidList(
        key: Preferences.Key<String>,
        default: List<SharedUid> = emptyList()
    ): Flow<List<SharedUid>> =
        getString(key, json.encodeToString(default)).map { jsonString ->
            try {
                json.decodeFromString<List<SharedUid>>(jsonString)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode SharedUid list from DataStore. Returning empty list.")
                emptyList()
            }
        }
}