// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid
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

    companion object {
        // UI Related
        val UI_USE_BLUR = booleanPreferencesKey("ui_use_blur")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE_STYLE = stringPreferencesKey("theme_palette_style")
        val THEME_COLOR_SPEC = stringPreferencesKey("theme_color_spec")
        val THEME_USE_DYNAMIC_COLOR = booleanPreferencesKey("theme_use_dynamic_color")
        val THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
        val UI_USE_MIUIX = booleanPreferencesKey("ui_use_miui_x")
        val UI_USE_MIUIX_MONET = booleanPreferencesKey("ui_use_miui_x_monet")
        val UI_USE_APPLE_FLOATING_BAR = booleanPreferencesKey("ui_use_apple_floating_bar")
        val UI_DYN_COLOR_FOLLOW_PKG_ICON = booleanPreferencesKey("ui_dyn_color_follow_pkg_icon")
        val LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON = booleanPreferencesKey("live_activity_dyn_color_follow_pkg_icon")
        val PREDICTIVE_BACK_ANIMATION = stringPreferencesKey("predictive_back_animation")
        val PREDICTIVE_BACK_EXIT_DIRECTION = stringPreferencesKey("predictive_back_exit_direction")

        // Show Live Activity
        val SHOW_LIVE_ACTIVITY = booleanPreferencesKey("show_live_activity")

        // Show Mi Island
        val SHOW_MI_ISLAND = booleanPreferencesKey("show_mi_island")
        val SHOW_MI_ISLAND_BYPASS_RESTRICTION = booleanPreferencesKey("show_mi_island_bypass_restriction")
        val SHOW_MI_ISLAND_OUTER_GLOW = booleanPreferencesKey("show_mi_island_outer_glow")

        // The duration to keep the network blocked to bypass Xiaomi's notification scanner
        val SHOW_MI_ISLAND_BLOCKING_INTERVAL_MS = intPreferencesKey("show_mi_island_blocking_interval")

        // Use Biometric Auth Install
        val INSTALLER_REQUIRE_BIOMETRIC_AUTH = stringPreferencesKey("installer_require_biometric_auth_mode")

        // Use Biometric Auth Uninstall
        val UNINSTALLER_REQUIRE_BIOMETRIC_AUTH = booleanPreferencesKey("uninstaller_use_biometric_auth")

        // Show Launcher Icon
        val SHOW_LAUNCHER_ICON = booleanPreferencesKey("show_launcher_icon")

        // Show System Icon for Packages
        val PREFER_SYSTEM_ICON_FOR_INSTALL = booleanPreferencesKey("prefer_system_icon_for_updates")

        // ForegroundInfoHandler
        val SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION = booleanPreferencesKey("show_dialog_when_pressing_notification")
        val NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS = intPreferencesKey("notification_success_auto_clear_seconds")

        // Auto Lock Installer
        val AUTO_LOCK_INSTALLER = booleanPreferencesKey("auto_lock_installer")

        // ConfigUtil
        val AUTHORIZER = stringPreferencesKey("authorizer")
        val CUSTOMIZE_AUTHORIZER = stringPreferencesKey("customize_authorizer")
        val UNINSTALL_FLAGS = intPreferencesKey("uninstall_flags")
        val USER_SET_LSPOSED_ACTIVE = booleanPreferencesKey("is_lsposed_active")

        // ApplyViewModel
        val USER_READ_SCOPE_TIPS = booleanPreferencesKey("user_read_scope_tips")
        val APPLY_ORDER_TYPE = stringPreferencesKey("apply_order_type")
        val APPLY_ORDER_IN_REVERSE = booleanPreferencesKey("apply_order_in_reverse")
        val APPLY_SELECTED_FIRST = booleanPreferencesKey("apply_selected_first")
        val APPLY_SHOW_SYSTEM_APP = booleanPreferencesKey("apply_show_system_app")
        val APPLY_SHOW_PACKAGE_NAME = booleanPreferencesKey("apply_show_package_name")

        // InstallerViewModel
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
        val DEFAULT_MANAGED_INSTALLER_PACKAGES = listOf(
            NamedPackage("Google Play Store", "com.android.vending"),
            NamedPackage("Shell", "com.android.shell")
        )
        val MANAGED_BLACKLIST_PACKAGES_LIST =
            stringPreferencesKey("managed_blacklist_packages_list")
        val MANAGED_SHARED_USER_ID_BLACKLIST =
            stringPreferencesKey("managed_shared_user_id_blacklist")
        val MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST =
            stringPreferencesKey("managed_shared_user_id_blacklist_exempted_packages_list")
        val ALWAYS_USE_ROOT_IN_SYSTEM =
            booleanPreferencesKey("always_use_root_in_system")

        // Lab
        val LAB_ENABLE_MODULE_FLASH = booleanPreferencesKey("enable_module_flash")
        val LAB_MODULE_FLASH_SHOW_ART = booleanPreferencesKey("module_flash_show_art")
        val LAB_ROOT_IMPLEMENTATION = stringPreferencesKey("lab_root_implementation")
        val LAB_HTTP_PROFILE = stringPreferencesKey("lab_http_profile")
        val LAB_HTTP_SAVE_FILE = booleanPreferencesKey("lab_http_save_file")
        val LAB_SET_INSTALL_REQUESTER = booleanPreferencesKey("lab_set_install_requester")
        val LAB_TAP_ICON_TO_SHARE = booleanPreferencesKey("lab_tap_icon_to_share")
        val LAB_SHOW_FILE_PATH = booleanPreferencesKey("lab_show_file_path")
        val LAB_SHOW_INSTALL_INITIATOR = booleanPreferencesKey("lab_show_install_initiator")
        val LAB_INSTALL_WITHOUT_USER_ACTION = booleanPreferencesKey("lab_install_without_user_action")

        // Debug
        val ENABLE_FILE_LOGGING = booleanPreferencesKey("enable_file_logging")

        // Updater
        val GITHUB_UPDATE_CHANNEL = stringPreferencesKey("github_update_channel")
        val CUSTOM_GITHUB_PROXY_URL = stringPreferencesKey("custom_github_proxy_url")
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
