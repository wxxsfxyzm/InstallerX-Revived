// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.packageupdate.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rosan.installer.domain.packageupdate.model.PendingSourceDeletion
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdate
import com.rosan.installer.domain.packageupdate.repository.SelfUpdateRecoveryRepository
import com.rosan.installer.domain.settings.model.config.Authorizer
import kotlinx.coroutines.flow.first

class SelfUpdateRecoveryRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SelfUpdateRecoveryRepository {
    override suspend fun arm(update: PendingSelfUpdate) {
        dataStore.edit { preferences ->
            preferences.clear()
            preferences[SESSION_ID] = update.sessionId
            preferences[PREVIOUS_UPDATE_TIME] = update.previousUpdateTime
            preferences[ARMED_AT_ELAPSED] = update.armedAtElapsed
        }
    }

    override suspend fun getPendingUpdate(): PendingSelfUpdate? {
        val preferences = dataStore.data.first()
        val sessionId = preferences[SESSION_ID] ?: return null
        return PendingSelfUpdate(
            sessionId = sessionId,
            previousUpdateTime = preferences[PREVIOUS_UPDATE_TIME] ?: 0L,
            armedAtElapsed = preferences[ARMED_AT_ELAPSED] ?: 0L
        )
    }

    override suspend fun updateSourceDeletion(
        sessionId: String,
        sourceDeletion: PendingSourceDeletion?
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

    private fun Preferences.sourceDeletion(): PendingSourceDeletion? {
        val authorizer = this[DELETE_AUTHORIZER]?.let { value ->
            Authorizer.entries.firstOrNull { it.value == value }
        } ?: return null
        val paths = this[DELETE_PATHS].orEmpty().toList()
        if (paths.isEmpty()) return null
        return PendingSourceDeletion(
            paths = paths,
            authorizer = authorizer,
            customizeAuthorizer = this[DELETE_CUSTOMIZE_AUTHORIZER].orEmpty()
        )
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
    }
}
