// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.impl

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rosan.installer.data.installer.repo.InstallerRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager class responsible for controlling the lifecycle of InstallerRepo instances.
 * Managed as a Singleton by Koin.
 */
class InstallerSessionManager(
    private val context: Context
) : KoinComponent {

    // Thread-safe map to store active installer sessions
    private val sessions = ConcurrentHashMap<String, InstallerRepo>()

    /**
     * Retrieves an existing session or creates a new one.
     */
    fun getOrCreate(id: String?): InstallerRepo {
        id?.let { existingId ->
            sessions[existingId]?.let {
                Timber.d("InstallerSessionManager: Returning existing session for id: $existingId")
                return it
            }
        }

        val newId = id ?: UUID.randomUUID().toString()
        Timber.d("InstallerSessionManager: Creating new session with id: $newId")

        // Define cleanup action: remove from map when repo closes
        val onCloseAction: () -> Unit = {
            remove(newId)
        }

        val repo: InstallerRepo = get { parametersOf(newId, onCloseAction) }
        sessions[newId] = repo

        // Ensure Service is running and aware of this session
        startService(newId)
        return repo
    }

    fun get(id: String): InstallerRepo? = sessions[id]

    /**
     * Returns a snapshot of all active sessions.
     * Crucial for the Service to restore handlers if the process was restarted.
     */
    fun getAllSessions(): List<InstallerRepo> = sessions.values.toList()

    /**
     * Removes a session from the manager.
     * This should usually be called by the repo's onClose callback.
     */
    private fun remove(id: String) {
        if (sessions.remove(id) != null) {
            Timber.d("InstallerSessionManager: Session $id removed from memory.")
        }
    }

    /**
     * Starts the foreground service to ensure execution context exists.
     */
    private fun startService(id: String) {
        val intent = Intent(context, InstallerService::class.java).apply {
            action = InstallerService.Action.Ready.value
            putExtra(InstallerService.EXTRA_ID, id)
        }
        // Using startForegroundService ensures the service promotes itself quickly
        ContextCompat.startForegroundService(context, intent)
    }
}