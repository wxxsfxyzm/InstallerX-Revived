// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.packageupdate.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdate
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdateHistory
import com.rosan.installer.domain.packageupdate.model.PendingSourceDeletion
import com.rosan.installer.domain.packageupdate.repository.SelfUpdateRecoveryRepository
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode
import kotlinx.coroutines.flow.first

class SelfUpdateRecoveryRepositoryImpl(private val dataStore: DataStore<Preferences>) : SelfUpdateRecoveryRepository {
    override suspend fun arm(update: PendingSelfUpdate) {
        dataStore.edit { preferences ->
            preferences.clear()
            preferences[SESSION_ID] = update.sessionId
            preferences[PREVIOUS_UPDATE_TIME] = update.previousUpdateTime
            preferences[ARMED_AT_ELAPSED] = update.armedAtElapsed
            update.history?.let { history -> preferences.setHistory(history) }
        }
    }

    override suspend fun getPendingUpdate(): PendingSelfUpdate? {
        val preferences = dataStore.data.first()
        val sessionId = preferences[SESSION_ID] ?: return null
        return PendingSelfUpdate(
            sessionId = sessionId,
            previousUpdateTime = preferences[PREVIOUS_UPDATE_TIME] ?: 0L,
            armedAtElapsed = preferences[ARMED_AT_ELAPSED] ?: 0L,
            history = preferences.pendingHistory(),
        )
    }

    override suspend fun updatePostInstallState(
        sessionId: String,
        sourceDeletion: PendingSourceDeletion?,
        historyAuthorizer: Authorizer,
    ) {
        dataStore.edit { preferences ->
            if (preferences[SESSION_ID] != sessionId) return@edit

            preferences.remove(DELETE_PATHS)
            preferences.remove(DELETE_AUTHORIZER)
            preferences.remove(DELETE_CUSTOMIZE_AUTHORIZER)
            preferences.remove(SOURCE_DELETION_READY)
            sourceDeletion?.let { deletion ->
                preferences[DELETE_PATHS] = deletion.paths.toSet()
                preferences[DELETE_AUTHORIZER] = deletion.authorizer.value
                preferences[DELETE_CUSTOMIZE_AUTHORIZER] = deletion.customizeAuthorizer
            }
            if (preferences[HISTORY_OPERATION_SESSION_KEY] != null) {
                preferences[HISTORY_AUTHORIZER] = historyAuthorizer.value
            }
        }
    }

    override suspend fun clear(sessionId: String) {
        dataStore.edit { preferences ->
            if (preferences[SESSION_ID] == sessionId) preferences.clear()
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    override suspend fun markCompletionNoticePending() {
        dataStore.edit { preferences ->
            val sourceDeletion = preferences.sourceDeletion()
            val history = preferences.pendingHistory()
            preferences.remove(SESSION_ID)
            preferences.remove(PREVIOUS_UPDATE_TIME)
            preferences.remove(ARMED_AT_ELAPSED)
            preferences[COMPLETION_NOTICE_PENDING] = true
            if (sourceDeletion != null) {
                preferences[SOURCE_DELETION_READY] = true
            } else {
                preferences.remove(DELETE_PATHS)
                preferences.remove(DELETE_AUTHORIZER)
                preferences.remove(DELETE_CUSTOMIZE_AUTHORIZER)
                preferences.remove(SOURCE_DELETION_READY)
            }
            if (history != null) {
                preferences[HISTORY_READY] = true
            } else {
                preferences.clearHistory()
            }
        }
    }

    override suspend fun consumeCompletionNotice(): Boolean {
        var consumed = false
        dataStore.edit { preferences ->
            if (preferences[COMPLETION_NOTICE_PENDING] == true) {
                preferences.remove(COMPLETION_NOTICE_PENDING)
                consumed = true
            }
        }
        return consumed
    }

    override suspend fun getCompletedSourceDeletion(): PendingSourceDeletion? {
        val preferences = dataStore.data.first()
        if (preferences[SOURCE_DELETION_READY] != true) return null
        return preferences.sourceDeletion()
    }

    override suspend fun clearCompletedSourceDeletion() {
        dataStore.edit { preferences ->
            preferences.remove(DELETE_PATHS)
            preferences.remove(DELETE_AUTHORIZER)
            preferences.remove(DELETE_CUSTOMIZE_AUTHORIZER)
            preferences.remove(SOURCE_DELETION_READY)
        }
    }

    override suspend fun getCompletedHistory(): PendingSelfUpdateHistory? {
        val preferences = dataStore.data.first()
        if (preferences[HISTORY_READY] != true) return null
        return preferences.pendingHistory()
    }

    override suspend fun clearCompletedHistory() {
        dataStore.edit { preferences -> preferences.clearHistory() }
    }

    private fun Preferences.sourceDeletion(): PendingSourceDeletion? {
        val authorizer = this[DELETE_AUTHORIZER]?.let { value ->
            Authorizer.entries.firstOrNull { it.value == value }
        } ?: return null
        val paths = this[DELETE_PATHS].orEmpty().toList()
        if (paths.isEmpty()) return null
        return PendingSourceDeletion(
            paths = paths,
            authorizer = authorizer,
            customizeAuthorizer = this[DELETE_CUSTOMIZE_AUTHORIZER].orEmpty(),
        )
    }

    private fun MutablePreferences.setHistory(history: PendingSelfUpdateHistory) {
        this[HISTORY_PACKAGE_NAME] = history.packageName
        putOptional(HISTORY_APP_LABEL, history.appLabel)
        putOptional(HISTORY_OLD_VERSION_NAME, history.oldVersionName)
        history.oldVersionCode?.let { this[HISTORY_OLD_VERSION_CODE] = it }
        putOptional(HISTORY_NEW_VERSION_NAME, history.newVersionName)
        history.newVersionCode?.let { this[HISTORY_NEW_VERSION_CODE] = it }
        this[HISTORY_SOURCE_PATHS] = history.sourcePaths.toSet()
        putOptional(HISTORY_INITIATOR_PACKAGE_NAME, history.initiatorPackageName)
        this[HISTORY_AUTHORIZER] = history.authorizer.value
        this[HISTORY_INSTALL_MODE] = history.installMode.value
        this[HISTORY_OPERATION_SESSION_KEY] = history.operationSessionKey
    }

    private fun Preferences.pendingHistory(): PendingSelfUpdateHistory? {
        val packageName = this[HISTORY_PACKAGE_NAME] ?: return null
        val operationSessionKey = this[HISTORY_OPERATION_SESSION_KEY] ?: return null
        val authorizer = this[HISTORY_AUTHORIZER]?.let(Authorizer::fromValueOrDefault) ?: return null
        val installMode = this[HISTORY_INSTALL_MODE]?.let { value ->
            InstallMode.entries.find { it.value == value }
        } ?: return null
        return PendingSelfUpdateHistory(
            packageName = packageName,
            appLabel = this[HISTORY_APP_LABEL],
            oldVersionName = this[HISTORY_OLD_VERSION_NAME],
            oldVersionCode = this[HISTORY_OLD_VERSION_CODE],
            newVersionName = this[HISTORY_NEW_VERSION_NAME],
            newVersionCode = this[HISTORY_NEW_VERSION_CODE],
            sourcePaths = this[HISTORY_SOURCE_PATHS].orEmpty().toList(),
            initiatorPackageName = this[HISTORY_INITIATOR_PACKAGE_NAME],
            authorizer = authorizer,
            installMode = installMode,
            operationSessionKey = operationSessionKey,
        )
    }

    private fun MutablePreferences.putOptional(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun MutablePreferences.clearHistory() {
        remove(HISTORY_PACKAGE_NAME)
        remove(HISTORY_APP_LABEL)
        remove(HISTORY_OLD_VERSION_NAME)
        remove(HISTORY_OLD_VERSION_CODE)
        remove(HISTORY_NEW_VERSION_NAME)
        remove(HISTORY_NEW_VERSION_CODE)
        remove(HISTORY_SOURCE_PATHS)
        remove(HISTORY_INITIATOR_PACKAGE_NAME)
        remove(HISTORY_AUTHORIZER)
        remove(HISTORY_INSTALL_MODE)
        remove(HISTORY_OPERATION_SESSION_KEY)
        remove(HISTORY_READY)
    }

    private companion object {
        val SESSION_ID = stringPreferencesKey("session_id")
        val PREVIOUS_UPDATE_TIME = longPreferencesKey("previous_update_time")
        val ARMED_AT_ELAPSED = longPreferencesKey("armed_at_elapsed")
        val DELETE_PATHS = stringSetPreferencesKey("delete_paths")
        val DELETE_AUTHORIZER = stringPreferencesKey("delete_authorizer")
        val DELETE_CUSTOMIZE_AUTHORIZER = stringPreferencesKey("delete_customize_authorizer")
        val SOURCE_DELETION_READY = booleanPreferencesKey("source_deletion_ready")
        val COMPLETION_NOTICE_PENDING = booleanPreferencesKey("completion_notice_pending")
        val HISTORY_PACKAGE_NAME = stringPreferencesKey("history_package_name")
        val HISTORY_APP_LABEL = stringPreferencesKey("history_app_label")
        val HISTORY_OLD_VERSION_NAME = stringPreferencesKey("history_old_version_name")
        val HISTORY_OLD_VERSION_CODE = longPreferencesKey("history_old_version_code")
        val HISTORY_NEW_VERSION_NAME = stringPreferencesKey("history_new_version_name")
        val HISTORY_NEW_VERSION_CODE = longPreferencesKey("history_new_version_code")
        val HISTORY_SOURCE_PATHS = stringSetPreferencesKey("history_source_paths")
        val HISTORY_INITIATOR_PACKAGE_NAME = stringPreferencesKey("history_initiator_package_name")
        val HISTORY_AUTHORIZER = stringPreferencesKey("history_authorizer")
        val HISTORY_INSTALL_MODE = stringPreferencesKey("history_install_mode")
        val HISTORY_OPERATION_SESSION_KEY = stringPreferencesKey("history_operation_session_key")
        val HISTORY_READY = booleanPreferencesKey("history_ready")
    }
}
