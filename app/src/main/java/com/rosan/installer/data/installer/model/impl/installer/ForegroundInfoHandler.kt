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
import com.rosan.installer.build.Manufacturer
import com.rosan.installer.build.RsConfig
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
import kotlinx.coroutines.delay
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
    companion object {
        private const val MINIMUM_VISIBILITY_DURATION_MS = 400L
    }

    private var job: Job? = null

    private val context by inject<Context>()

    private val appDataStore by inject<AppDataStore>()

    private val notificationManager = NotificationManagerCompat.from(context)

    private val notificationId = installer.id.hashCode() and Int.MAX_VALUE

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to combine and collect flows.")
        job = scope.launch {
            installer.progress.combine(installer.background) { progress, background ->
                Timber.i(
                    "[id=${installer.id}] Combined Flow Update: progress=${progress::class.simpleName}, background=$background"
                )

                if (progress is ProgressEntity.AnalysedUnsupported) {
                    Timber.w("[id=${installer.id}] AnalysedUnsupported detected: ${progress.reason}")
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show()
                    }
                    installer.close()
                    return@combine
                }

                if (background) {
                    val startTime = System.currentTimeMillis()
                    Timber.d("[id=${installer.id}] Background mode. Generating and setting notification.")
                    setNotification(newNotification(progress, true))
                    // 计算已经花费的时间
                    val elapsedTime = System.currentTimeMillis() - startTime
                    // 如果花费的时间小于最小可见时间，则延迟剩余的时间
                    if (elapsedTime < MINIMUM_VISIBILITY_DURATION_MS) {
                        // 只对非终止状态的通知进行延迟，确保取消通知的即时性
                        if (progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess) {
                            delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                        }
                    }
                } else {
                    Timber
                        .d("[id=${installer.id}] Foreground mode (Activity visible). Cancelling notification.")
                    setNotification(null)
                }

            }.collect() // 启动流的收集
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cancelling notification and job.")
        setNotification(null)
        job?.cancel()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotification(notification: Notification? = null) {
        if (notification == null) {
            Timber.d("[id=${installer.id}] setNotification: Cancelling notification with id: $notificationId")
            notificationManager.cancel(notificationId)
            return
        }
        Timber
            .d("[id=${installer.id}] setNotification: Posting/Updating notification with id: $notificationId")
        notificationManager.notify(notificationId, notification)
    }

    enum class Channel(val value: String) {
        InstallerChannel("installer_channel"),
        InstallerProgressChannel("installer_progress_channel")
    }

    enum class Icon(@param:DrawableRes val resId: Int) {
        Working(R.drawable.round_hourglass_empty_black_24),
        Pausing(R.drawable.round_hourglass_disabled_black_24)
    }

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

    private fun getString(@StringRes resId: Int): String = context.getString(resId)
    private val openIntent = BroadcastHandler.openIntent(context, installer)

    private val analyseIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Analyse)

    private val installIntent: PendingIntent by lazy {
        // use when to handle different manufacturers
        when {
            RsConfig.currentManufacturer == Manufacturer.XIAOMI &&
                    installer.config.authorizer == ConfigEntity.Authorizer.Shizuku &&
                    installer.config.installMode == ConfigEntity.InstallMode.Notification -> {
                // Special logic for Xiaomi devices
                // Xiaomi will intercept usb installation requests, which caused ForegroundService not working.
                // So we use a transparent Activity to trigger the installation.
                val intent = Intent(context, InstallTriggerActivity::class.java).apply {
                    putExtra(InstallerActivity.KEY_ID, installer.id)
                }
                PendingIntent.getActivity(
                    context,
                    installer.id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            else -> {
                // Default logic for other manufacturers
                // Use a PendingIntent to trigger the installation directly
                BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Install)
            }
        }
    }

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