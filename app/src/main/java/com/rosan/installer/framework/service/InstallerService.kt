// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.rosan.installer.R
import com.rosan.installer.data.session.handler.ActionHandler
import com.rosan.installer.data.session.handler.BroadcastHandler
import com.rosan.installer.data.session.handler.ForegroundInfoHandler
import com.rosan.installer.data.session.handler.ProgressHandler
import com.rosan.installer.data.session.manager.InstallerSessionManager
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class InstallerService : Service() {
    companion object {
        const val EXTRA_ID = "id"
        private const val IDLE_TIMEOUT_MS = 1000L
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "installer_background_channel"
    }

    enum class Action(val value: String) {
        Ready("ready"),
        Destroy("destroy");

        companion object {
            fun from(value: String?): Action? = entries.find { it.value == value }
        }
    }

    private val sessionManager: InstallerSessionManager by inject()

    // Lifecycle scope for the Service itself (not specific installers)
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Map storing scopes for each active session ID
    private val sessionScopes = mutableMapOf<String, CoroutineScope>()

    private var idleTimeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actionString = intent?.action
        val id = intent?.getStringExtra(EXTRA_ID)

        Timber.d("onStartCommand: Action=$actionString, ID=$id")

        // Unconditionally fulfill the Android foreground service contract.
        // Even if the intent is invalid or the session was canceled,
        // we MUST call startForeground() to prevent the strict 5-second crash.
        promoteToForeground()

        // If intent is null, system restarted the service.
        if (intent == null) {
            restoreSessions()
            checkIdleState()
            // Return START_NOT_STICKY because our session state is in-memory.
            // If the process is killed, we cannot recover the installation state anyway.
            return START_NOT_STICKY
        }

        when (Action.from(actionString)) {
            Action.Destroy -> stopServiceForcefully()
            Action.Ready -> {
                if (id != null) {
                    sessionManager.get(id)?.let { session ->
                        setupSessionScope(session)
                    } ?: run {
                        Timber.w("Received Ready action for ID $id but Repo not found in Manager.")
                    }
                }
            }

            else -> { /* Ignore unknown actions */
            }
        }

        // Always check if we need to schedule a shutdown.
        // If the Repo wasn't found (race condition), this will ensure the service stops cleanly.
        checkIdleState()

        // Return START_NOT_STICKY to avoid zombie restarts
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("InstallerService: onCreate. Creating Notification Channel.")
        createNotificationChannel()

        // Restore state from Manager.
        // If Service crashed/restarted but App Process (and Manager) is alive, re-attach handlers.
        restoreSessions()
    }

    override fun onDestroy() {
        Timber.d("InstallerService: onDestroy")
        serviceScope.cancel()
        synchronized(sessionScopes) {
            sessionScopes.values.forEach { it.cancel() }
            sessionScopes.clear()
        }
        super.onDestroy()
    }

    private fun restoreSessions() {
        val activeSessions = sessionManager.getAllSessions()
        if (activeSessions.isNotEmpty()) {
            Timber.i("InstallerService: Restoring ${activeSessions.size} active sessions from Manager.")
            activeSessions.forEach { setupSessionScope(it) }
        }
    }

    private fun setupSessionScope(session: InstallerSessionRepository) {
        val id = session.id

        synchronized(sessionScopes) {
            if (sessionScopes.containsKey(id)) {
                Timber.d("[id=$id] Scope already exists. Skipping setup.")
                return
            }

            Timber.d("[id=$id] Creating new execution scope and handlers.")
            idleTimeoutJob?.cancel()

            val scope = CoroutineScope(Dispatchers.IO + Job())
            sessionScopes[id] = scope

            val handlers = listOf(
                ActionHandler(scope, session),
                ProgressHandler(scope, session),
                ForegroundInfoHandler(scope, session),
                BroadcastHandler(scope, session)
            )

            // Start observing UI effects (Toasts) on the main thread
            observeUiEffectsForSession(session)

            scope.launch {
                Timber.d("[id=$id] Starting handlers.")
                handlers.forEach { it.onStart() }

                session.progress.collect { progress ->
                    if (progress is ProgressEntity.Finish) {
                        Timber.d("[id=$id] Finished. Cleaning up handlers.")
                        handlers.forEach { it.onFinish() }
                        detachSession(id)
                    }
                }
            }
        }
    }

    private fun checkIdleState() {
        synchronized(sessionScopes) {
            // Note: updateForegroundState() is completely removed.
            // We already promoted to foreground at the top of onStartCommand.

            if (sessionScopes.isEmpty()) {
                Timber.d("No active scopes. Scheduling shutdown in $IDLE_TIMEOUT_MS ms.")
                idleTimeoutJob?.cancel()
                idleTimeoutJob = serviceScope.launch {
                    delay(IDLE_TIMEOUT_MS)
                    Timber.i("Idle timeout reached. Stopping service.")
                    stopSelf()
                }
            }
        }
    }

    private fun promoteToForeground() {
        val cancelIntent = Intent(this, InstallerService::class.java).apply {
            action = Action.Destroy.value
        }
        val pendingCancel = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val settingsIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingSettings = PendingIntent.getActivity(
            this, 1, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(getString(R.string.installer_running))
            .setContentText(getString(R.string.installer_notification_tap_to_disable))
            .setContentIntent(pendingSettings)
            .addAction(0, getString(R.string.close), pendingCancel)
            .setOngoing(true)
            .build()

        // Use 0 for API 33 and below to avoid manifest mismatch crashes.
        // Android 14+ requires explicit foreground service types.
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        // DO NOT wrap this in a try-catch block.
        // Swallowing exceptions here breaks the OS contract and causes delayed, confusing timeout crashes.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }

    /**
     * Removes the scope for a specific installer but keeps the Service alive momentarily
     * to check if other installers are running.
     */
    private fun detachSession(id: String) {
        synchronized(sessionScopes) {
            sessionScopes.remove(id)?.cancel()
            Timber.d("[id=$id] Scope removed and cancelled.")
        }

        // We do NOT remove from SessionManager here.
        // The Repo's own close() logic (via ActionHandler or UI) triggers Manager removal.
        // We only care that *we* are done processing it.

        checkIdleState()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName(getString(R.string.installer_background_channel_name))
            .setDescription(getString(R.string.installer_notification_desc))
            .build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun stopServiceForcefully() {
        Timber.w("Force destroying service.")
        // Clean up all scopes
        synchronized(sessionScopes) {
            sessionScopes.values.forEach { it.cancel() }
            sessionScopes.clear()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Observes session progress to trigger UI effects like Toasts.
     * Executed within serviceScope which runs on Dispatchers.Main.
     */
    private fun observeUiEffectsForSession(session: InstallerSessionRepository) {
        serviceScope.launch {
            session.progress.collect { progress ->
                // Disable toast according to user preference
                if (!session.config.showToast) return@collect
                when (progress) {
                    is ProgressEntity.InstallSuccess -> {
                        toast(R.string.installer_install_success)
                    }

                    is ProgressEntity.InstallCompleted -> {
                        val successCount = progress.results.count { it.success }
                        val totalCount = progress.results.size
                        toast(R.string.batch_install_result_message, successCount, totalCount)
                    }

                    is ProgressEntity.UninstallSuccess -> {
                        toast(R.string.uninstall_success_message)
                    }

                    is ProgressEntity.InstallFailed,
                    is ProgressEntity.InstallAnalysedFailed,
                    is ProgressEntity.InstallResolvedFailed -> {
                        toast(R.string.installer_install_failed)
                    }

                    is ProgressEntity.UninstallFailed -> {
                        toast(R.string.uninstall_failed_message)
                    }

                    else -> {
                        // Safely ignore transitional states
                    }
                }
            }
        }
    }
}
