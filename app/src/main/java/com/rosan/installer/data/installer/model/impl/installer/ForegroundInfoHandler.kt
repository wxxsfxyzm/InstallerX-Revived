package com.rosan.installer.data.installer.model.impl.installer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.rosan.installer.R
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.ui.activity.InstallTriggerActivity
import com.rosan.installer.ui.activity.InstallerActivity
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ForegroundInfoHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    enum class Channel(val value: String) {
        InstallerChannel("installer_channel"), InstallerProgressChannel("installer_progress_channel")
    }

    enum class Icon(@DrawableRes val resId: Int) {
        Working(R.drawable.round_hourglass_empty_black_24), Pausing(R.drawable.round_hourglass_disabled_black_24)
    }

    private var job: Job? = null

    private val context by inject<Context>()

    private val notificationManager = NotificationManagerCompat.from(context)

    private val notificationId = installer.id.hashCode() and Int.MAX_VALUE

    private val notificationChannels = mapOf(
        Channel.InstallerChannel to NotificationChannelCompat.Builder(
            Channel.InstallerChannel.value, NotificationManagerCompat.IMPORTANCE_MAX
        ).setName(getString(R.string.installer_channel_name)).build(),

        Channel.InstallerProgressChannel to NotificationChannelCompat.Builder(
            Channel.InstallerProgressChannel.value, NotificationManagerCompat.IMPORTANCE_MIN
        ).setName(getString(R.string.installer_progress_channel_name)).build()
    )

    private val workingProgresses = listOf(
        ProgressEntity.Ready,
        ProgressEntity.Resolving,
        ProgressEntity.ResolveSuccess,
        ProgressEntity.Analysing,
        ProgressEntity.AnalysedSuccess,
        ProgressEntity.Installing,
        ProgressEntity.InstallSuccess
    )

    private val importanceProgresses = listOf(
        ProgressEntity.ResolvedFailed,
        ProgressEntity.AnalysedFailed,
        ProgressEntity.AnalysedSuccess,
        ProgressEntity.InstallFailed,
        ProgressEntity.InstallSuccess
    )

    private val installProgresses = mapOf(
        ProgressEntity.Resolving to 0,
        ProgressEntity.Analysing to 40,
        ProgressEntity.Installing to 80
    )

    private fun newNotificationBuilder(
        progress: ProgressEntity, background: Boolean
    ): NotificationCompat.Builder {
        val isWorking = workingProgresses.contains(progress)
        val isImportance = importanceProgresses.contains(progress)

        val channel =
            notificationChannels[if (isImportance && background) Channel.InstallerChannel else Channel.InstallerProgressChannel]!!

        notificationManager.createNotificationChannel(channel)

        val icon = (if (isWorking) Icon.Working else Icon.Pausing).resId

        var builder = NotificationCompat.Builder(context, channel.id).setSmallIcon(icon)
            .setContentIntent(openIntent)
            .setDeleteIntent(finishIntent)

        installProgresses[progress]?.let {
            builder = builder.setProgress(100, it, false)
        }

        return builder
    }

    private fun newNotification(
        progress: ProgressEntity, background: Boolean
    ): Notification? {
        val builder = newNotificationBuilder(progress, background)
        return when (progress) {
            is ProgressEntity.Ready -> onReady(builder)
            is ProgressEntity.Resolving -> onResolving(builder)
            is ProgressEntity.ResolvedFailed -> onResolvedFailed(builder)
            is ProgressEntity.ResolveSuccess -> onResolveSuccess(builder)
            is ProgressEntity.Analysing -> onAnalysing(builder)
            is ProgressEntity.AnalysedFailed -> onAnalysedFailed(builder)
            is ProgressEntity.AnalysedSuccess -> onAnalysedSuccess(builder)
            is ProgressEntity.Installing -> onInstalling(builder)
            is ProgressEntity.InstallFailed -> onInstallFailed(builder)
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder)
            is ProgressEntity.Finish -> null
            else -> null
        }
    }

    override suspend fun onStart() {
        job = scope.launch {
            var progress: ProgressEntity = ProgressEntity.Ready
            var background = false
            fun refresh() {
                setNotification(newNotification(progress, background))
            }
            launch {
                installer.progress.collect {
                    progress = it
                    refresh()
                }
            }
            launch {
                installer.background.collect {
                    background = it
                    refresh()
                }
            }
        }
    }

    override suspend fun onFinish() {
        setNotification(null)
        job?.cancel()
    }

    private fun getString(@StringRes resId: Int): String = context.getString(resId)

    /*private fun setNotification(notification: Notification? = null) {
        *//*        // ======================= 在这里加上日志 =======================
                val title = notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)
                Log.d("NotificationIdDebug", "setNotification called. ID: $notificationId, Title: $title")
                // ==============================================================*//*
        if (notification == null) {
            notificationManager.cancel(notificationId)
            return
        }
        // 检查版本是否为 Android 13 (Tiramisu) 或更高
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 检查 POST_NOTIFICATIONS 权限是否已被授予
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 如果权限未被授予
                // 在这里不应该调用 notify()
                // TODO 应该在这里引导用户去开启权限
                // 权限未被授予，不再直接显示 Toast
                // 而是通过 Repo 发送一个事件
                scope.launch {
                    installer.postEvent(InstallerEvent.NOTIFICATION_PERMISSION_MISSING)
                }
                return
            }
        }
        // 如果权限已被授予，或者系统版本低于 Android 13，则正常显示通知
        notificationManager.notify(notificationId, notification)
    }*/

    // 简化 setNotification 方法，移除其中的权限检查逻辑
    private fun setNotification(notification: Notification? = null) {
        if (notification == null) {
            notificationManager.cancel(notificationId)
            return
        }

        // 此处的权限检查逻辑已被 ActionHandler 取代，可以直接移除
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                scope.launch {
                    installer.postEvent(InstallerEvent.NOTIFICATION_PERMISSION_MISSING)
                }
                return
            }
        }
        */

        // 直接显示通知
        notificationManager.notify(notificationId, notification)
    }

    private val openIntent = BroadcastHandler.openIntent(context, installer)

    private val analyseIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Analyse)

    // 将 installIntent 的目标从 InstallerActivity 改为 InstallTriggerActivity
    private val installIntent by lazy {
        // 创建一个指向我们新的透明Activity的 Intent
        val intent = Intent(context, InstallTriggerActivity::class.java).apply { // <--- 修改这里
            putExtra(InstallerActivity.KEY_ID, installer.id)
            // 这里不再需要 action，因为TriggerActivity只有一个功能
        }

        PendingIntent.getActivity(
            context,
            installer.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

//    private val installIntent =
//        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Install)

    private val finishIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Finish)

    private fun onReady(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_ready))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onResolving(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolving))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onResolvedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolve_failed))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onResolveSuccess(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolve_success))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onAnalysing(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_analysing))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onAnalysedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_analyse_failed))
            .addAction(0, getString(R.string.retry), analyseIntent)
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onAnalysedSuccess(builder: NotificationCompat.Builder): Notification {
        val selected = installer.entities.filter { it.selected }
        return (if (selected.groupBy { it.app.packageName }.size != 1) builder.setContentTitle(
            getString(R.string.installer_prepare_install)
        ).addAction(0, getString(R.string.cancel), finishIntent)
        else {
            val info = selected.map { it.app }.getInfo(context)
            builder.setContentTitle(info.title)
                .setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                .setLargeIcon(info.icon?.toBitmapOrNull())
                .addAction(0, getString(R.string.install), installIntent)
                .addAction(0, getString(R.string.cancel), finishIntent)
        }).build()
    }

    private fun onInstalling(builder: NotificationCompat.Builder): Notification {
        val info = installer.entities.filter { it.selected }.map { it.app }.getInfo(context)
        return builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_installing))
            .setLargeIcon(info.icon?.toBitmapOrNull())
            .addAction(0, getString(R.string.cancel), finishIntent).build()
    }

    private fun onInstallFailed(
        builder: NotificationCompat.Builder
    ): Notification {
        val info = installer.entities.filter { it.selected }.map { it.app }.getInfo(context)
        val reason = installer.error.getErrorMessage(context)
        val contentText = getString(R.string.installer_install_failed)
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(info.title)
            .bigText(
                "$contentText\n" +
                        getString(R.string.installer_error_reason) +
                        ": $reason"
            )
        return builder.setContentTitle(info.title)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setLargeIcon(info.icon?.toBitmapOrNull())
            .addAction(0, getString(R.string.retry), installIntent)
            .addAction(0, getString(R.string.cancel), finishIntent).build()
    }

    private fun onInstallSuccess(builder: NotificationCompat.Builder): Notification {
        val entities = installer.entities.filter { it.selected }.map { it.app }
        val info = entities.getInfo(context)
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(entities.first().packageName)
        val launchPendingIntent = launchIntent?.let {
            BroadcastHandler.launchIntent(context, installer, it)
        }

        var newBuilder = builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_install_success))
            .setLargeIcon(info.icon?.toBitmapOrNull())
        if (launchIntent != null) newBuilder =
            newBuilder.addAction(0, getString(R.string.open), launchPendingIntent)
        return newBuilder
            .addAction(0, getString(R.string.finish), finishIntent)
            .build()
    }
}