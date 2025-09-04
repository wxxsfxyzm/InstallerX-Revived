package com.rosan.installer.data.installer.model.impl.installer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
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

                    // --- VERSION CHECK & DISPATCH ---
                    // This is the main branching point. It decides which notification style to use.
                    val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        buildModernNotification(progress, showDialog)
                    } else {
                        buildLegacyNotification(progress, true, showDialog)
                    }
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

    /**
     * Builds and returns a Notification for Android 15+ devices.
     * This is the entry point for the modern notification logic.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun buildModernNotification(
        progress: ProgressEntity,
        showDialog: Boolean
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported) {
            return null
        }
        val builder = newModernNotificationBuilder(progress, showDialog)

        // Finalize notification content for terminal states, which might require async operations.
        return when (progress) {
            is ProgressEntity.InstallFailed -> onInstallFailed(builder).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder).build()
            else -> builder.build()
        }
    }

    /**
     * Creates and configures a NotificationCompat.Builder using Android 15's rich ProgressStyle.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun newModernNotificationBuilder(
        progress: ProgressEntity,
        showDialog: Boolean
    ): NotificationCompat.Builder {
        val channel = NotificationChannelCompat.Builder(
            Channel.InstallerLiveChannel.value,
            NotificationManagerCompat.IMPORTANCE_MAX
        )
            .setName(getString(R.string.installer_live_channel_name))
            .build()
        notificationManager.createNotificationChannel(channel)

        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification,
            ConfigEntity.InstallMode.AutoNotification -> if (showDialog) openIntent else null

            else -> openIntent
        }

        val baseBuilder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(Icon.Working.resId)
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)

        // === 定义进度点 ===
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressPoints(
                listOf(
                    NotificationCompat.ProgressStyle.Point(20),
                    NotificationCompat.ProgressStyle.Point(40),
                    NotificationCompat.ProgressStyle.Point(100)
                )
            )

        var progressValue = 0
        var contentTitle = getString(R.string.installer_ready)

        when (progress) {
            ProgressEntity.InstallResolving,
            ProgressEntity.InstallResolveSuccess -> {
                progressValue = 20
                contentTitle = getString(R.string.installer_resolving)
                progressStyle.setProgressTrackerIcon(
                    IconCompat.createWithResource(context, R.drawable.round_hourglass_empty_black_24)
                )
                baseBuilder.addAction(0, getString(R.string.cancel), finishIntent)
            }

            ProgressEntity.InstallAnalysing -> {
                progressValue = 40
                contentTitle = getString(R.string.installer_analysing)
                baseBuilder.addAction(0, getString(R.string.cancel), finishIntent)
            }

            ProgressEntity.InstallAnalysedSuccess -> {
                val selectedEntities = installer.analysisResults
                    .flatMap { it.appEntities }
                    .filter { it.selected }
                val selectedApps = selectedEntities.map { it.app }
                progressValue = 40
                contentTitle = selectedApps.getInfo(context).title
                baseBuilder.setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                baseBuilder.setShortCriticalText("Pending Install")
                baseBuilder.addAction(0, getString(R.string.install), installIntent)
                baseBuilder.addAction(0, getString(R.string.cancel), finishIntent)
                baseBuilder.setLargeIcon(getLargeIconBitmap())
            }

            ProgressEntity.Installing -> {
                progressValue = 80
                contentTitle = getString(R.string.installer_installing)
                baseBuilder.addAction(0, getString(R.string.cancel), finishIntent)
            }

            is ProgressEntity.InstallSuccess -> {
                progressValue = 100
                contentTitle = getString(R.string.installer_install_success)
                baseBuilder
                    .setOnlyAlertOnce(false)
                    .setLargeIcon(getLargeIconBitmap())
            }

            is ProgressEntity.InstallFailed -> {
                contentTitle = getString(R.string.installer_install_failed)
                baseBuilder
                    .setOnlyAlertOnce(false)
            }

            else -> { /* ignore */
            }
        }

        baseBuilder.setContentTitle(contentTitle)

        // 应用进度样式
        progressStyle.setProgress(progressValue)
        baseBuilder.setStyle(progressStyle)

        return baseBuilder
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
        InstallerProgressChannel("installer_progress_channel"),
        InstallerLiveChannel("installer_live_channel")
    }

    enum class Icon(@param:DrawableRes val resId: Int) {
        Working(R.drawable.round_hourglass_empty_black_24),
        Pausing(R.drawable.round_hourglass_disabled_black_24)
    }

    private val notificationChannels = mapOf(
        Channel.InstallerChannel to NotificationChannelCompat.Builder(
            Channel.InstallerChannel.value, NotificationManagerCompat.IMPORTANCE_HIGH // Use HIGH for final pop-up
        ).setName(getString(R.string.installer_channel_name)).build(),

        Channel.InstallerProgressChannel to NotificationChannelCompat.Builder(
            Channel.InstallerProgressChannel.value,
            NotificationManagerCompat.IMPORTANCE_LOW // Use LOW to be less intrusive
        ).setName(getString(R.string.installer_progress_channel_name)).build(),

        Channel.InstallerLiveChannel to NotificationChannelCompat.Builder(
            Channel.InstallerLiveChannel.value, NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName(getString(R.string.installer_live_channel_name)).build()
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

    /**
     * (Renamed from newNotification)
     * Builds and returns a Notification for legacy Android versions.
     */
    private suspend fun buildLegacyNotification(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): Notification? {
        val builder = newLegacyNotificationBuilder(progress, background, showDialog)
        return when (progress) {
            is ProgressEntity.Ready -> onReady(builder)
            is ProgressEntity.InstallResolving -> onResolving(builder)
            is ProgressEntity.InstallResolvedFailed -> onResolvedFailed(builder)
            is ProgressEntity.InstallResolveSuccess -> onResolveSuccess(builder)
            is ProgressEntity.InstallPreparing -> onPreparing(builder, progress)
            is ProgressEntity.InstallAnalysing -> onAnalysing(builder)
            is ProgressEntity.InstallAnalysedFailed -> onAnalysedFailed(builder)
            is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess(builder)
            is ProgressEntity.Installing -> onInstalling(builder)
            is ProgressEntity.InstallFailed -> onInstallFailed(builder).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder).build()
            is ProgressEntity.Finish, is ProgressEntity.Error, is ProgressEntity.InstallAnalysedUnsupported -> null
            else -> null // TODO temporarily disable uninstall notification
        }
    }

    /**
     * (Renamed from newNotificationBuilder)
     * Creates and configures a standard NotificationCompat.Builder for legacy Android versions.
     */
    private fun newLegacyNotificationBuilder(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): NotificationCompat.Builder {
        val isWorking = workingProgresses.contains(progress)
        val isImportance = importanceProgresses.contains(progress)

        // 1. Determine the correct enum constant directly. This is safe and reliable.
        val channelEnum = if (isImportance && background) {
            Channel.InstallerChannel
        } else {
            Channel.InstallerProgressChannel
        }

        // 2. Get the actual NotificationChannelCompat object from the map using the enum as the key.
        val channel = notificationChannels[channelEnum]!!
        notificationManager.createNotificationChannel(channel)

        val icon = (if (isWorking) Icon.Working else Icon.Pausing).resId

        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification, ConfigEntity.InstallMode.AutoNotification -> {
                if (showDialog) openIntent else null
            }

            else -> openIntent
        }

        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed) {
            builder.setOngoing(false)
                .setOnlyAlertOnce(false)
        }

        installProgresses[progress]?.let {
            builder.setProgress(100, it, false)
        }

        return builder
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

    private fun onPreparing(builder: NotificationCompat.Builder, progress: ProgressEntity.InstallPreparing) =
        builder.setContentTitle(getString(R.string.installer_prepare_install))
            .setProgress(100, (progress.progress * 100).toInt(), progress.progress < 0)
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
    ): NotificationCompat.Builder { // Changed return type
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
        return builder.setContentTitle(info.title) // Return builder directly
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .addAction(0, getString(R.string.retry), installIntent)
            .addAction(0, getString(R.string.cancel), finishIntent)
    }

    private fun onInstallSuccess(builder: NotificationCompat.Builder): NotificationCompat.Builder { // Changed return type
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
        if (launchIntent != null) newBuilder =
            newBuilder.addAction(0, getString(R.string.open), launchPendingIntent)
        return newBuilder // Return builder directly
            .addAction(0, getString(R.string.finish), finishIntent)
    }

}