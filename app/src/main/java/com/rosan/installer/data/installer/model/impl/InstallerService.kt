package com.rosan.installer.data.installer.model.impl

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import timber.log.Timber

class InstallerService : Service() {
    companion object {
        const val EXTRA_ID = "id"
        private const val IDLE_TIMEOUT_MS = 1000L
    }

    enum class Action(val value: String) {
        Ready("ready"), Finish("finish"), Destroy("destroy");

        companion object {
            fun revert(value: String): Action = entries.first { it.value == value }
        }
    }

    private val lifecycleScope = CoroutineScope(Dispatchers.IO + Job())
    private val scopes = mutableMapOf<String, CoroutineScope>()
    private var timeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setForeground(enable: Boolean) {
        Timber.d("setForeground called with enable: $enable")
        if (!enable) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        val id = this.hashCode()
        val channelId = "installer_background_channel"
        val channel = NotificationChannelCompat.Builder(
            channelId,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName(getString(R.string.installer_background_channel_name)).build()
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(channel)

        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

        // 1. Prepare Intent for the "Cancel" action button (Stops the service)
        val cancelIntent = Intent(Action.Destroy.value).apply {
            component = ComponentName(this@InstallerService, InstallerService::class.java)
        }
        val cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, flags)

        // 2. Prepare Intent for the notification body click (Opens Notification Settings)
        val settingsIntent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Use a different requestCode (e.g., 1) to distinguish from the cancel intent
        val settingsPendingIntent = PendingIntent.getActivity(this, 1, settingsIntent, flags)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(getString(R.string.installer_running))
            .setContentText(getString(R.string.installer_notification_tap_to_disable))
            // Body click -> Open Settings
            .setContentIntent(settingsPendingIntent)
            // Action button -> Stop Service
            .addAction(0, getString(R.string.close), cancelPendingIntent)
            .setDeleteIntent(cancelPendingIntent) // Handle swipe dismissal if applicable
            .build()

        startForeground(id, notification)
    }

    private fun autoForeground() {
        synchronized(this) {
            val shouldBeForeground = scopes.isNotEmpty()
            Timber.d("autoForeground: scopes.size=${scopes.size}, shouldBeForeground=$shouldBeForeground")
            setForeground(shouldBeForeground)
        }
    }

    override fun onDestroy() {
        Timber.w("onDestroy: Service is being destroyed.")
        scopes.keys.forEach { id ->
            Timber.w("onDestroy: Closing forgotten installer instance: $id")
            InstallerRepoImpl.get(id)?.let { installer ->
                runCatching {
                    installer.close()
                }.onFailure { throwable ->
                    Timber.w(throwable, "Ignoring exception while closing installer $id during onDestroy.")
                }
            }
        }
        lifecycleScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val id = intent?.getStringExtra(EXTRA_ID)
        Timber.d("onStartCommand received. Action: $action, ID: $id")
        intent?.let { onStartCommand(it) }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onStartCommand(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID)
        val installer = id?.let { InstallerRepoImpl.get(it) }
        val action = intent.action ?: return
        Timber.d("Processing action: $action for ID: $id. Installer found: ${installer != null}")
        when (Action.revert(action)) {
            Action.Ready -> ready(installer ?: return)
            Action.Finish -> finish(installer ?: return)
            Action.Destroy -> destroy()
        }
    }

    private fun ready(installer: InstallerRepo) {
        synchronized(this) {
            val id = installer.id
            Timber.d("[id=$id] ready() called.")
            timeoutJob?.cancel().also {
                Timber.d("[id=$id] Previous shutdown timeout cancelled.")
            }

            if (scopes[id] != null) {
                Timber.w("[id=$id] Scope already exists. Ignoring ready() call.")
                return
            }

            Timber.d("[id=$id] Creating new scope and handlers.")
            val scope = CoroutineScope(Dispatchers.IO + Job())
            scopes[id] = scope
            val handlers = listOf(
                ActionHandler(scope, installer),
                ProgressHandler(scope, installer),
                ForegroundInfoHandler(scope, installer),
                BroadcastHandler(scope, installer)
            )

            scope.launch {
                Timber.d("[id=$id] Starting all handlers.")
                handlers.forEach { it.onStart() }
                Timber.d("[id=$id] Starting to collect progress flow.")
                installer.progress.collect { progress ->
                    if (progress is ProgressEntity.Finish) {
                        Timber
                            .d("[id=$id] Detected ProgressEntity.Finish. Cleaning up handlers and calling finish(installer).")
                        handlers.forEach { it.onFinish() }
                        finish(installer)
                    }
                }
            }
            autoForeground()
        }
    }

    private fun finish(installer: InstallerRepo) {
        val id = installer.id
        Timber.d("[id=$id] finish() called.")

        synchronized(this) {
            scopes.remove(id)?.cancel().also {
                Timber.d("[id=$id] Scope removed and cancelled. Existed: ${it != null}")
            }

            InstallerRepoImpl.remove(id)

            timeoutJob?.cancel()

            if (scopes.isEmpty()) {
                Timber
                    .d("All tasks finished. Service is now idle. Scheduling shutdown in $IDLE_TIMEOUT_MS ms.")
                timeoutJob = lifecycleScope.launch {
                    delay(IDLE_TIMEOUT_MS)
                    Timber.w("Idle timeout reached. Destroying service now.")
                    destroy()
                }
            } else {
                Timber.d("Tasks still running: ${scopes.keys}. Updating foreground state.")
                autoForeground()
            }
        }
    }

    private fun destroy() {
        Timber.w("destroy() called. Calling stopSelf().")
        stopSelf()
    }
}