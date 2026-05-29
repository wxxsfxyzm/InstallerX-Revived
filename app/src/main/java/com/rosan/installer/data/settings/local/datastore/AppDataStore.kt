// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rosan.installer.domain.settings.model.app.NamedPackage
import com.rosan.installer.domain.settings.model.app.SharedUid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

class AppDataStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    // Expose the raw data flow for synchronous mapping in the repository layer
    val data: Flow<Preferences> = dataStore.data

    enum class PreferenceValueType {
        STRING,
        INT,
        BOOLEAN
    }

    data class SupportedPreferenceKey<T>(
        val key: Preferences.Key<T>,
        val type: PreferenceValueType
    )

    companion object {
        private val mutableSupportedKeys = linkedMapOf<String, SupportedPreferenceKey<*>>()

        val supportedKeys: Map<String, SupportedPreferenceKey<*>>
            get() = mutableSupportedKeys

        private fun <T> register(
            key: Preferences.Key<T>,
            type: PreferenceValueType
        ): Preferences.Key<T> {
            mutableSupportedKeys[key.name] = SupportedPreferenceKey(key, type)
            return key
        }

        // UI Related
        val UI_USE_BLUR = register(booleanPreferencesKey("ui_use_blur"), PreferenceValueType.BOOLEAN)
        val THEME_MODE = register(stringPreferencesKey("theme_mode"), PreferenceValueType.STRING)
        val THEME_PALETTE_STYLE = register(stringPreferencesKey("theme_palette_style"), PreferenceValueType.STRING)
        val THEME_COLOR_SPEC = register(stringPreferencesKey("theme_color_spec"), PreferenceValueType.STRING)
        val THEME_USE_DYNAMIC_COLOR = register(booleanPreferencesKey("theme_use_dynamic_color"), PreferenceValueType.BOOLEAN)
        val THEME_SEED_COLOR = register(intPreferencesKey("theme_seed_color"), PreferenceValueType.INT)
        val UI_USE_MIUIX = register(booleanPreferencesKey("ui_use_miui_x"), PreferenceValueType.BOOLEAN)
        val UI_USE_MIUIX_MONET = register(booleanPreferencesKey("ui_use_miui_x_monet"), PreferenceValueType.BOOLEAN)
        val UI_USE_APPLE_FLOATING_BAR = register(booleanPreferencesKey("ui_use_apple_floating_bar"), PreferenceValueType.BOOLEAN)
        val UI_DYN_COLOR_FOLLOW_PKG_ICON = register(booleanPreferencesKey("ui_dyn_color_follow_pkg_icon"), PreferenceValueType.BOOLEAN)
        val LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON =
            register(booleanPreferencesKey("live_activity_dyn_color_follow_pkg_icon"), PreferenceValueType.BOOLEAN)
        val PREDICTIVE_BACK_ANIMATION = register(stringPreferencesKey("predictive_back_animation"), PreferenceValueType.STRING)
        val PREDICTIVE_BACK_EXIT_DIRECTION =
            register(stringPreferencesKey("predictive_back_exit_direction"), PreferenceValueType.STRING)

        // Show Live Activity
        val SHOW_LIVE_ACTIVITY = register(booleanPreferencesKey("show_live_activity"), PreferenceValueType.BOOLEAN)

        // Show Mi Island
        val SHOW_MI_ISLAND = register(booleanPreferencesKey("show_mi_island"), PreferenceValueType.BOOLEAN)
        val SHOW_MI_ISLAND_BYPASS_RESTRICTION =
            register(booleanPreferencesKey("show_mi_island_bypass_restriction"), PreferenceValueType.BOOLEAN)
        val SHOW_MI_ISLAND_OUTER_GLOW = register(booleanPreferencesKey("show_mi_island_outer_glow"), PreferenceValueType.BOOLEAN)

        // The duration to keep the network blocked to bypass Xiaomi's notification scanner
        val SHOW_MI_ISLAND_BLOCKING_INTERVAL_MS =
            register(intPreferencesKey("show_mi_island_blocking_interval"), PreferenceValueType.INT)

        // Use Biometric Auth Install
        val INSTALLER_REQUIRE_BIOMETRIC_AUTH =
            register(stringPreferencesKey("installer_require_biometric_auth_mode"), PreferenceValueType.STRING)

        // Use Biometric Auth Uninstall
        val UNINSTALLER_REQUIRE_BIOMETRIC_AUTH =
            register(booleanPreferencesKey("uninstaller_use_biometric_auth"), PreferenceValueType.BOOLEAN)

        // Show Launcher Icon
        val SHOW_LAUNCHER_ICON = register(booleanPreferencesKey("show_launcher_icon"), PreferenceValueType.BOOLEAN)

        // Show System Icon for Packages
        val PREFER_SYSTEM_ICON_FOR_INSTALL =
            register(booleanPreferencesKey("prefer_system_icon_for_updates"), PreferenceValueType.BOOLEAN)

        // ForegroundInfoHandler
        val SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION =
            register(booleanPreferencesKey("show_dialog_when_pressing_notification"), PreferenceValueType.BOOLEAN)
        val NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS =
            register(intPreferencesKey("notification_success_auto_clear_seconds"), PreferenceValueType.INT)

        // Auto Lock Installer
        val AUTO_LOCK_INSTALLER = register(booleanPreferencesKey("auto_lock_installer"), PreferenceValueType.BOOLEAN)

        // ConfigUtil
        val AUTHORIZER = register(stringPreferencesKey("authorizer"), PreferenceValueType.STRING)
        val CUSTOMIZE_AUTHORIZER = register(stringPreferencesKey("customize_authorizer"), PreferenceValueType.STRING)
        val UNINSTALL_FLAGS = register(intPreferencesKey("uninstall_flags"), PreferenceValueType.INT)
        val USER_SET_LSPOSED_ACTIVE = register(booleanPreferencesKey("is_lsposed_active"), PreferenceValueType.BOOLEAN)

        // ApplyViewModel
        val USER_READ_SCOPE_TIPS = register(booleanPreferencesKey("user_read_scope_tips"), PreferenceValueType.BOOLEAN)
        val APPLY_ORDER_TYPE = register(stringPreferencesKey("apply_order_type"), PreferenceValueType.STRING)
        val APPLY_ORDER_IN_REVERSE = register(booleanPreferencesKey("apply_order_in_reverse"), PreferenceValueType.BOOLEAN)
        val APPLY_SELECTED_FIRST = register(booleanPreferencesKey("apply_selected_first"), PreferenceValueType.BOOLEAN)
        val APPLY_SHOW_SYSTEM_APP = register(booleanPreferencesKey("apply_show_system_app"), PreferenceValueType.BOOLEAN)
        val APPLY_SHOW_PACKAGE_NAME = register(booleanPreferencesKey("apply_show_package_name"), PreferenceValueType.BOOLEAN)

        // InstallerViewModel
        val DIALOG_VERSION_COMPARE_SINGLE_LINE =
            register(booleanPreferencesKey("show_dialog_version_compare_single_line"), PreferenceValueType.BOOLEAN)
        val DIALOG_SDK_COMPARE_MULTI_LINE =
            register(booleanPreferencesKey("show_dialog_sdk_compare_multi_line"), PreferenceValueType.BOOLEAN)
        val CLOSE_SESSION_COUNTDOWN =
            register(intPreferencesKey("show_dhizuku_auto_close_count_down_menu"), PreferenceValueType.INT)
        val DIALOG_SHOW_EXTENDED_MENU =
            register(booleanPreferencesKey("show_dialog_install_extended_menu"), PreferenceValueType.BOOLEAN)
        val DIALOG_SHOW_INTELLIGENT_SUGGESTION =
            register(booleanPreferencesKey("show_dialog_install_intelligent_suggestion"), PreferenceValueType.BOOLEAN)
        val DIALOG_DISABLE_NOTIFICATION_ON_DISMISS =
            register(booleanPreferencesKey("show_disable_notification_for_dialog_install"), PreferenceValueType.BOOLEAN)
        val DIALOG_SHOW_OPPO_SPECIAL =
            register(booleanPreferencesKey("show_oppo_special"), PreferenceValueType.BOOLEAN)
        val DIALOG_AUTO_SILENT_INSTALL =
            register(booleanPreferencesKey("auto_silent_install"), PreferenceValueType.BOOLEAN)
        val DIALOG_LONG_CLICK_BACKGROUND_INSTALL =
            register(booleanPreferencesKey("long_click_background_install"), PreferenceValueType.BOOLEAN)
        val DETECT_XPOSED_MODULE =
            register(booleanPreferencesKey("detect_xposed_module"), PreferenceValueType.BOOLEAN)
        val QUICK_OPEN_LSPOSED =
            register(booleanPreferencesKey("quick_open_lsposed"), PreferenceValueType.BOOLEAN)

        // Customize Installer
        val MANAGED_INSTALLER_PACKAGES_LIST =
            register(stringPreferencesKey("managed_packages_list"), PreferenceValueType.STRING)
        val DEFAULT_MANAGED_INSTALLER_PACKAGES = listOf(
            NamedPackage("Google Play Store", "com.android.vending"),
            NamedPackage("Shell", "com.android.shell")
        )
        val MANAGED_BLACKLIST_PACKAGES_LIST =
            register(stringPreferencesKey("managed_blacklist_packages_list"), PreferenceValueType.STRING)
        val MANAGED_SHARED_USER_ID_BLACKLIST =
            register(stringPreferencesKey("managed_shared_user_id_blacklist"), PreferenceValueType.STRING)
        val MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST =
            register(stringPreferencesKey("managed_shared_user_id_blacklist_exempted_packages_list"), PreferenceValueType.STRING)
        val ALWAYS_USE_ROOT_IN_SYSTEM =
            register(booleanPreferencesKey("always_use_root_in_system"), PreferenceValueType.BOOLEAN)

        // Lab
        val LAB_ENABLE_MODULE_FLASH = register(booleanPreferencesKey("enable_module_flash"), PreferenceValueType.BOOLEAN)
        val LAB_MODULE_FLASH_SHOW_ART = register(booleanPreferencesKey("module_flash_show_art"), PreferenceValueType.BOOLEAN)
        val LAB_ROOT_IMPLEMENTATION = register(stringPreferencesKey("lab_root_implementation"), PreferenceValueType.STRING)
        val LAB_HTTP_PROFILE = register(stringPreferencesKey("lab_http_profile"), PreferenceValueType.STRING)
        val LAB_HTTP_SAVE_FILE = register(booleanPreferencesKey("lab_http_save_file"), PreferenceValueType.BOOLEAN)
        val LAB_SET_INSTALL_REQUESTER = register(booleanPreferencesKey("lab_set_install_requester"), PreferenceValueType.BOOLEAN)
        val LAB_TAP_ICON_TO_SHARE = register(booleanPreferencesKey("lab_tap_icon_to_share"), PreferenceValueType.BOOLEAN)
        val LAB_SHOW_FILE_PATH = register(booleanPreferencesKey("lab_show_file_path"), PreferenceValueType.BOOLEAN)
        val LAB_SHOW_INSTALL_INITIATOR = register(booleanPreferencesKey("lab_show_install_initiator"), PreferenceValueType.BOOLEAN)
        val LAB_INSTALL_WITHOUT_USER_ACTION =
            register(booleanPreferencesKey("lab_install_without_user_action"), PreferenceValueType.BOOLEAN)

        // Debug
        val ENABLE_FILE_LOGGING = register(booleanPreferencesKey("enable_file_logging"), PreferenceValueType.BOOLEAN)

        // Updater
        val GITHUB_UPDATE_CHANNEL = register(stringPreferencesKey("github_update_channel"), PreferenceValueType.STRING)
        val CUSTOM_GITHUB_PROXY_URL = register(stringPreferencesKey("custom_github_proxy_url"), PreferenceValueType.STRING)
    }

    suspend fun putString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    fun getInt(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> =
        dataStore.data.map { it[key] ?: default }

    /**
     * Saves a list of NamedPackage objects to DataStore after converting it to a JSON string.
     * @param key The Preferences.Key<String> to save the list under.
     * @param packages The list of packages to save.
     */
    suspend fun putNamedPackageList(key: Preferences.Key<String>, packages: List<NamedPackage>) =
        putString(key, json.encodeToString(packages))

    /**
     * Retrieves a Flow of a list of NamedPackage objects from DataStore.
     * It reads the JSON string and deserializes it.
     */
    fun getNamedPackageList(
        key: Preferences.Key<String>,
        default: List<NamedPackage> = emptyList()
    ): Flow<List<NamedPackage>> =
        getString(key, json.encodeToString(default)).map { jsonString ->
            try {
                json.decodeFromString<List<NamedPackage>>(jsonString)
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Failed to decode NamedPackage list from DataStore. Returning empty list."
                )
                emptyList()
            }
        }

    /**
     * Synchronously parses a list of NamedPackage objects from a given Preferences snapshot.
     * This avoids Flow creation and is ideal for use inside map { } operations.
     *
     * @param prefs The raw Preferences object snapshot.
     * @param key The Preferences.Key<String> to read.
     * @param default The default list to return if missing or on error.
     */
    fun parseNamedPackageList(
        prefs: Preferences,
        key: Preferences.Key<String>,
        default: List<NamedPackage> = emptyList()
    ): List<NamedPackage> {
        val jsonString = prefs[key] ?: return default
        return try {
            json.decodeFromString<List<NamedPackage>>(jsonString)
        } catch (e: Exception) {
            Timber.e(
                e,
                "Failed to synchronously decode NamedPackage list. Returning default list."
            )
            default
        }
    }

    /**
     * Saves a list of SharedUid objects to DataStore after converting it to a JSON string.
     */
    suspend fun putSharedUidList(key: Preferences.Key<String>, uids: List<SharedUid>) =
        putString(key, json.encodeToString(uids))

    /**
     * Retrieves a Flow of a list of SharedUid objects from DataStore.
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

    /**
     * Synchronously parses a list of SharedUid objects from a given Preferences snapshot.
     *
     * @param prefs The raw Preferences object snapshot.
     * @param key The Preferences.Key<String> to read.
     * @param default The default list to return if missing or on error.
     */
    fun parseSharedUidList(
        prefs: Preferences,
        key: Preferences.Key<String>,
        default: List<SharedUid> = emptyList()
    ): List<SharedUid> {
        val jsonString = prefs[key] ?: return default
        return try {
            json.decodeFromString<List<SharedUid>>(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to synchronously decode SharedUid list. Returning default list.")
            default
        }
    }

    /**
     * Updates the uninstall flags in DataStore using the provided transform function.
     */
    suspend fun updateUninstallFlags(transform: (Int) -> Int) {
        dataStore.edit { preferences ->
            val current = preferences[UNINSTALL_FLAGS] ?: 0
            preferences[UNINSTALL_FLAGS] = transform(current)
        }
    }
}
