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
import kotlinx.coroutines.launch
import okhttp3.internal.closeQuietly

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

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

        /*        scope.launch {
                    handlers.forEach { it.onStart() }
                    installer.progress.collect {
                        if (it is ProgressEntity.Finish) {
                            handlers.forEach { it.onFinish() }
                            scopes.remove(id)
                            finish(installer)
                        }
                    }
                }*/
        scope.launch {
            handlers.forEach { it.onStart() }
            installer.progress.collect { progress ->
                if (progress is ProgressEntity.Finish) {
                    handlers.forEach { it.onFinish() }
                    // 任务完成，直接调用 finish 方法处理后续清理和服务生命周期检查
                    // 不需要在这里再做 scopes.remove(id) 等操作，统一在 finish 方法中处理
                    finish(installer)
                }
            }
        }
        autoForeground()
    }

    private fun finish(installer: InstallerRepo) {
        /*        val id = installer.id

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
                }*/
        // --- Logic to finish the installer process ---
        val id = installer.id

        // 如果任务仍在 scopes 中，取消其协程并移除
        scopes.remove(id)?.cancel() // 好习惯：取消协程以释放资源

        // 清理与该 installer 相关的资源
        InstallerRepoImpl.remove(id)
        installer.closeQuietly()

        // 取消任何可能存在的旧的 timeoutJob
        timeoutJob?.cancel()

        // 立即检查服务是否应该停止，不再延迟
        synchronized(this) {
            if (scopes.isEmpty()) {
                // 没有正在运行的任务了，立即销毁服务
                destroy() // destroy() 会调用 stopSelf()
            } else {
                // 如果还有其他任务，仅更新前台状态
                autoForeground()
            }
        }
    }

    private fun destroy() {
        stopSelf()
    }
}