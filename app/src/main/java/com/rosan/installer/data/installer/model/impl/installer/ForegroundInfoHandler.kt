package com.rosan.installer.data.installer.model.impl.installer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.rosan.installer.R
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.activity.InstallTriggerActivity
import com.rosan.installer.ui.activity.InstallerActivity
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ForegroundInfoHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {

    enum class Channel(val value: String) {
        InstallerChannel("installer_channel"),
        InstallerProgressChannel("installer_progress_channel")
    }

    enum class Icon(@param:DrawableRes val resId: Int) {
        Working(R.drawable.round_hourglass_empty_black_24),
        Pausing(R.drawable.round_hourglass_disabled_black_24)
    }

    private var job: Job? = null

    private val context by inject<Context>()

    private val appDataStore by inject<AppDataStore>()

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

        val showDialog = runBlocking {
            appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, false).first()
        }

        var builder = NotificationCompat.Builder(context, channel.id).setSmallIcon(icon)
            .setContentIntent(

                when (installer.config.installMode) {
                    ConfigEntity.InstallMode.Notification,
                    ConfigEntity.InstallMode.AutoNotification -> {
                        Timber.tag("ForegroundInfoHandler").d("Using openIntent for notification")
                        Timber.tag("ForegroundInfoHandler")
                            .d("isShowDialogWhenPressingNotificationEnabled: $showDialog")
                        if (showDialog) openIntent else null
                    }

                    else -> openIntent
                }
            )
            .setDeleteIntent(finishIntent)

        installProgresses[progress]?.let {
            builder = builder.setProgress(100, it, false)
        }

        return builder
    }

    private fun newNotification(
        progress: ProgressEntity, background: Boolean
    ): Notification? {
        if (progress is ProgressEntity.AnalysedUnsupported) return null
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
            is ProgressEntity.Error -> null
            is ProgressEntity.AnalysedUnsupported -> null
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        job = scope.launch {
            // --- 核心修改：使用 combine 将 progress 和 background 流合并 ---
            // 只要 progress 或 background 中任何一个有新值，这个代码块就会以最新的组合执行
            installer.progress.combine(installer.background) { progress, background ->
                // 特殊逻辑优先处理
                if (progress is ProgressEntity.AnalysedUnsupported) {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show()
                    }
                    installer.close()
                    return@combine // 提前返回，不再执行后续通知逻辑
                }

                // 根据是否在后台决定通知行为
                if (background) {
                    // 在后台模式，创建并显示通知
                    setNotification(newNotification(progress, true)) // 始终传递 true
                } else {
                    // 在前台模式（Activity可见），取消任何可能存在的通知
                    setNotification(null)
                }

            }.collect() // 启动流的收集
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        setNotification(null)
        job?.cancel()
    }

    private fun getString(@StringRes resId: Int): String = context.getString(resId)

    // 简化 setNotification 方法，移除其中的权限检查逻辑
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotification(notification: Notification? = null) {
        if (notification == null) {
            notificationManager.cancel(notificationId)
            return
        }

        // 直接显示通知
        notificationManager.notify(notificationId, notification)
    }

    private val openIntent = BroadcastHandler.openIntent(context, installer)

    private val analyseIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Analyse)

    // 将 installIntent 的目标从 InstallerActivity 改为 InstallTriggerActivity
    private val installIntent by lazy {
        // 创建一个指向我们新的透明Activity的 Intent
        val intent = Intent(context, InstallTriggerActivity::class.java).apply {
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