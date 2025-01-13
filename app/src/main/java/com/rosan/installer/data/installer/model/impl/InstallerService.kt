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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.closeQuietly
import kotlin.time.Duration.Companion.seconds

class InstallerService : Service() {
    companion object {
        const val EXTRA_ID = "id"
    }

    enum class Action(val value: String) {
        Ready("ready"), Finish("finish"), Destroy("destroy");

        companion object {
            fun revert(value: String): Action = entries.first { it.value == value }
        }
    }

    private val lifecycleScope = CoroutineScope(Dispatchers.IO)

    private val scopes = mutableMapOf<String, CoroutineScope>()

    private var timeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun setForeground(enable: Boolean) {
        if (!enable) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val id = this.hashCode()

        val channelId = "installer_background_channel"
        val channel =
            NotificationChannelCompat.Builder(
                channelId,
                NotificationManagerCompat.IMPORTANCE_MIN
            )
                .setName(getString(R.string.installer_background_channel_name)).build()
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(channel)

        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val cancelIntent = Intent(Action.Destroy.value)
        cancelIntent.component = ComponentName(this, InstallerService::class.java)
        val cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, flags)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.round_hourglass_empty_black_24)
            .setContentTitle(getString(R.string.installer_running))
            .addAction(0, getString(R.string.cancel), cancelPendingIntent)
            .setDeleteIntent(cancelPendingIntent).build()
        startForeground(id, notification)
    }

    private fun autoForeground() {
        synchronized(this) {
            setForeground(scopes.isNotEmpty())
        }
    }

    override fun onDestroy() {
        scopes.keys.forEach {
            (InstallerRepoImpl.get(it) ?: return@forEach).closeQuietly()
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { onStartCommand(it) }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onStartCommand(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID)
        val installer = id?.let { InstallerRepoImpl.get(it) }
        when (Action.revert(intent.action ?: return)) {
            Action.Ready -> ready(installer ?: return)
            Action.Finish -> finish(installer ?: return)
            Action.Destroy -> destroy()
        }
    }

    private fun ready(installer: InstallerRepo) {
        val id = installer.id
        if (scopes[id] != null) return
        val scope = CoroutineScope(Dispatchers.IO)
        scopes[id] = scope

        val handlers = listOf(
            ActionHandler(scope, installer),
            ProgressHandler(scope, installer),
            ForegroundInfoHandler(scope, installer),
            BroadcastHandler(scope, installer)
        )

        scope.launch {
            handlers.forEach { it.onStart() }
            installer.progress.collect {
                if (it is ProgressEntity.Finish) {
                    handlers.forEach { it.onFinish() }
                    scopes.remove(id)
                    finish(installer)
                }
            }
        }

        autoForeground()
    }

    private fun finish(installer: InstallerRepo) {
        val id = installer.id

        if (scopes[id] != null) {
            installer.closeQuietly()
            return
        }

        InstallerRepoImpl.remove(id)

        timeoutJob?.cancel()
        timeoutJob = lifecycleScope.launch {
            autoForeground()
            delay(15.seconds)
            if (scopes.isEmpty()) destroy()
        }
    }

    private fun destroy() {
        stopSelf()
    }
}