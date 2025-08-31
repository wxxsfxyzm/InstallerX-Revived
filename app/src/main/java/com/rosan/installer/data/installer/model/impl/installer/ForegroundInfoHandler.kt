package com.rosan.installer.data.installer.model.impl.installer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    private val appIconRepo by inject<AppIconRepo>()

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = installer.id.hashCode() and Int.MAX_VALUE

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to combine and collect flows.")
        job = scope.launch {
            combine(
                installer.progress,
                installer.background,
                appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true)
            ) { progress, background, showDialog ->

                Timber.i("[id=${installer.id}] Combined Flow: progress=${progress::class.simpleName}, background=$background, showDialog=$showDialog")

                if (progress is ProgressEntity.InstallAnalysedUnsupported) {
                    Timber.w("[id=${installer.id}] AnalysedUnsupported: ${progress.reason}")
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show()
                    }
                    installer.close()
                    return@combine
                }

                if (background) {
                    val startTime = System.currentTimeMillis()

                    // [FIXED] Call the new suspend function to build the notification
                    val notification = newNotification(progress, true, showDialog)
                    setNotification(notification)

                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < MINIMUM_VISIBILITY_DURATION_MS) {
                        if (progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess) {
                            delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                        }
                    }
                } else {
                    Timber.d("[id=${installer.id}] Foreground mode. Cancelling notification.")
                    setNotification(null)
                }

            }.collect()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cancelling notification and job.")
        // Clear cache for all involved packages to ensure freshness on next install
        installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app.packageName }
            .distinct()
            .forEach {
                appIconRepo.clearCacheForPackage(it)
            }
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
        ProgressEntity.InstallResolving,
        ProgressEntity.InstallResolveSuccess,
        ProgressEntity.InstallAnalysing,
        ProgressEntity.InstallAnalysedSuccess,
        ProgressEntity.Installing,
        ProgressEntity.InstallSuccess
    )

    private val importanceProgresses = listOf(
        ProgressEntity.InstallResolvedFailed,
        ProgressEntity.InstallAnalysedFailed,
        ProgressEntity.InstallAnalysedSuccess,
        ProgressEntity.InstallFailed,
        ProgressEntity.InstallSuccess
    )

    private val installProgresses = mapOf(
        ProgressEntity.InstallResolving to 0,
        ProgressEntity.InstallAnalysing to 40,
        ProgressEntity.Installing to 80
    )

    /**
     * Gets the large icon for notifications as a Bitmap by fetching it from the AppIconRepository.
     */
    private suspend fun getLargeIconBitmap(): Bitmap? {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val entityToInstall = entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            ?: entities.sortedBest().firstOrNull()
            ?: return null // Return null if no suitable entity is found

        // Use standard notification large icon dimensions
        val iconSizePx = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        // Get the drawable from the central repository
        val drawable = appIconRepo.getIcon(
            packageName = entityToInstall.packageName,
            entityToInstall = entityToInstall,
            iconSizePx = iconSizePx
        )

        // Convert the drawable to a bitmap of the correct size
        return drawable?.toBitmapOrNull(width = iconSizePx, height = iconSizePx)
    }

    private fun newNotificationBuilder(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): NotificationCompat.Builder {
        val isWorking = workingProgresses.contains(progress)
        val isImportance = importanceProgresses.contains(progress)

        val channel =
            notificationChannels[if (isImportance && background) Channel.InstallerChannel else Channel.InstallerProgressChannel]!!

        notificationManager.createNotificationChannel(channel)

        val icon = (if (isWorking) Icon.Working else Icon.Pausing).resId

        // Set content intent based on user setting
        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification, ConfigEntity.InstallMode.AutoNotification -> {
                if (showDialog) openIntent else null
            }

            else -> openIntent
        }

        var builder = NotificationCompat.Builder(context, channel.id).setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)

        installProgresses[progress]?.let {
            builder = builder.setProgress(100, it, false)
        }

        return builder
    }

    private suspend fun newNotification(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): Notification? {
        // No need to show notification for unsupported analysis since handled in collector
        // if (progress is ProgressEntity.AnalysedUnsupported) return null
        val builder = newNotificationBuilder(progress, background, showDialog)
        return when (progress) {
            is ProgressEntity.Ready -> onReady(builder)
            is ProgressEntity.InstallResolving -> onResolving(builder)
            is ProgressEntity.InstallResolvedFailed -> onResolvedFailed(builder)
            is ProgressEntity.InstallResolveSuccess -> onResolveSuccess(builder)
            is ProgressEntity.InstallAnalysing -> onAnalysing(builder)
            is ProgressEntity.InstallAnalysedFailed -> onAnalysedFailed(builder)
            is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess(builder)
            is ProgressEntity.Installing -> onInstalling(builder)
            is ProgressEntity.InstallFailed -> onInstallFailed(builder)
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder)
            is ProgressEntity.Finish -> null
            is ProgressEntity.Error -> null
            is ProgressEntity.InstallAnalysedUnsupported -> null
            // TODO temporarily disable uninstall notification
            else -> null
        }
    }

    private fun getString(@StringRes resId: Int): String = context.getString(resId)
    private val openIntent = BroadcastHandler.openIntent(context, installer)

    private val analyseIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Analyse)

    private val installIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Install)

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

    private suspend fun onAnalysedSuccess(builder: NotificationCompat.Builder): Notification {
        val selectedEntities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
        val selectedApps = selectedEntities.map { it.app }

        return (if (selectedApps.groupBy { it.packageName }.size != 1) builder.setContentTitle(
            getString(R.string.installer_prepare_install)
        ).addAction(0, getString(R.string.cancel), finishIntent)
        else {
            val info = selectedApps.getInfo(context)
            builder.setContentTitle(info.title)
                .setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                .setLargeIcon(getLargeIconBitmap())
                .addAction(0, getString(R.string.install), installIntent)
                .addAction(0, getString(R.string.cancel), finishIntent)
        }).build()
    }

    private suspend fun onInstalling(builder: NotificationCompat.Builder): Notification {
        val info = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .getInfo(context)
        return builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_installing))
            .setLargeIcon(getLargeIconBitmap())
            .addAction(0, getString(R.string.cancel), finishIntent).build()
    }

    private suspend fun onInstallFailed(
        builder: NotificationCompat.Builder
    ): Notification {
        val info = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .getInfo(context)
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
            .setLargeIcon(getLargeIconBitmap())
            .addAction(0, getString(R.string.retry), installIntent)
            .addAction(0, getString(R.string.cancel), finishIntent).build()
    }

    private suspend fun onInstallSuccess(builder: NotificationCompat.Builder): Notification {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val info = entities.getInfo(context)
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(entities.first().packageName)
        val launchPendingIntent = launchIntent?.let {
            BroadcastHandler.launchIntent(context, installer, it)
        }

        var newBuilder = builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_install_success))
            .setLargeIcon(getLargeIconBitmap())
        if (launchIntent != null) newBuilder =
            newBuilder.addAction(0, getString(R.string.open), launchPendingIntent)
        return newBuilder
            .addAction(0, getString(R.string.finish), finishIntent)
            .build()
    }
}