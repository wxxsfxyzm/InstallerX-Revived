package com.rosan.installer.data.installer.model.impl

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.rosan.installer.R
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.impl.installer.ActionHandler
import com.rosan.installer.data.installer.model.impl.installer.BroadcastHandler
import com.rosan.installer.data.installer.model.impl.installer.ForegroundInfoHandler
import com.rosan.installer.data.installer.model.impl.installer.ProgressHandler
import com.rosan.installer.data.installer.repo.InstallerRepo
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

    // Map storing scopes for each active installer ID
    private val installerScopes = mutableMapOf<String, CoroutineScope>()

    private var idleTimeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("InstallerService: onCreate. Creating Notification Channel.")
        createNotificationChannel()

        // Restore state from Manager.
        // If Service crashed/restarted but App Process (and Manager) is alive, re-attach handlers.
        restoreSessions()
    }

    private fun restoreSessions() {
        val activeSessions = sessionManager.getAllSessions()
        if (activeSessions.isNotEmpty()) {
            Timber.i("InstallerService: Restoring ${activeSessions.size} active sessions from Manager.")
            activeSessions.forEach { setupInstallerScope(it) }
            updateForegroundState()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actionString = intent?.action
        val id = intent?.getStringExtra(EXTRA_ID)

        Timber.d("onStartCommand: Action=$actionString, ID=$id")

        // If intent is null, system restarted the service (START_STICKY).
        if (intent == null) {
            restoreSessions()
            return START_STICKY
        }

        when (Action.from(actionString)) {
            Action.Destroy -> stopServiceForcefully()
            Action.Ready -> {
                if (id != null) {
                    sessionManager.get(id)?.let { repo ->
                        setupInstallerScope(repo)
                        updateForegroundState()
                    } ?: run {
                        Timber.w("Received Ready action for ID $id but Repo not found in Manager.")
                    }
                }
            }

            else -> { /* Ignore unknown actions */
            }
        }

        return START_STICKY
    }

    /**
     * Idempotent method to setup handlers for a specific installer.
     * Safe to call multiple times for the same ID.
     */
    private fun setupInstallerScope(installer: InstallerRepo) {
        val id = installer.id

        synchronized(installerScopes) {
            if (installerScopes.containsKey(id)) {
                Timber.d("[id=$id] Scope already exists. Skipping setup.")
                return
            }

            Timber.d("[id=$id] Creating new execution scope and handlers.")
            // Cancel any pending shutdown since we have work to do
            idleTimeoutJob?.cancel()

            val scope = CoroutineScope(Dispatchers.IO + Job())
            installerScopes[id] = scope

            // Initialize Handlers
            val handlers = listOf(
                ActionHandler(scope, installer),
                ProgressHandler(scope, installer),
                ForegroundInfoHandler(scope, installer),
                BroadcastHandler(scope, installer)
            )

            scope.launch {
                Timber.d("[id=$id] Starting handlers.")
                handlers.forEach { it.onStart() }

                // Monitor Repo progress to determine when to clean up THIS scope
                installer.progress.collect { progress ->
                    if (progress is ProgressEntity.Finish) {
                        Timber.d("[id=$id] Finished. Cleaning up handlers.")
                        handlers.forEach { it.onFinish() }
                        detachInstaller(id)
                    }
                }
            }
        }
    }

    /**
     * Removes the scope for a specific installer but keeps the Service alive momentarily
     * to check if other installers are running.
     */
    private fun detachInstaller(id: String) {
        synchronized(installerScopes) {
            installerScopes.remove(id)?.cancel()
            Timber.d("[id=$id] Scope removed and cancelled.")
        }

        // We do NOT remove from SessionManager here.
        // The Repo's own close() logic (via ActionHandler or UI) triggers Manager removal.
        // We only care that *we* are done processing it.

        checkIdleState()
    }

    private fun checkIdleState() {
        synchronized(installerScopes) {
            updateForegroundState()

            if (installerScopes.isEmpty()) {
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

    private fun updateForegroundState() {
        val isRunning = installerScopes.isNotEmpty()
        if (isRunning) {
            startForegroundService()
        } else {
            // If empty, we wait for timeout to stopSelf(), but we can downgrade notification priority or remove it
            // However, stopForeground(true) usually happens inside stopSelf or when we want to hide it.
            // Keeping it simple: if running, ensure foreground.
        }
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

    private fun startForegroundService() {
        val cancelIntent = Intent(this, InstallerService::class.java).apply {
            action = Action.Destroy.value
        }
        val pendingCancel = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open Notification Settings on click
        val settingsIntent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
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

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to start foreground service.")
        }
    }

    private fun stopServiceForcefully() {
        Timber.w("Force destroying service.")
        // Clean up all scopes
        synchronized(installerScopes) {
            installerScopes.values.forEach { it.cancel() }
            installerScopes.clear()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.d("InstallerService: onDestroy")
        serviceScope.cancel()
        synchronized(installerScopes) {
            installerScopes.values.forEach { it.cancel() }
            installerScopes.clear()
        }
        super.onDestroy()
    }
}