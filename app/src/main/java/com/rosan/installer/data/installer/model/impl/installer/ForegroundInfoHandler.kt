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
    private var sessionStartTime: Long = 0L
    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()
    private val appIconRepo by inject<AppIconRepo>()

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = installer.id.hashCode() and Int.MAX_VALUE

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to combine and collect flows.")
        sessionStartTime = System.currentTimeMillis()
        job = scope.launch {
            combine(
                installer.progress,
                installer.background,
                appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true),
                appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false),
                appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false)
            ) { progress, background, showDialog, showLiveActivity, preferSystemIcon ->

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
                    // --- VERSION CHECK & DISPATCH ---
                    // This is the main branching point. It decides which notification style to use.
                    val notification =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && showLiveActivity) {
                            buildModernNotification(progress, showDialog, preferSystemIcon)
                        } else {
                            buildLegacyNotification(progress, true, showDialog, preferSystemIcon)
                        }
                    setNotification(notification)

                    val elapsedTime = System.currentTimeMillis() - sessionStartTime
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

    // A data class to hold information about each installation stage.
    private data class InstallStageInfo(
        val progressClass: kotlin.reflect.KClass<out ProgressEntity>,
        val weight: Float // Represents the relative length of this stage's segment.
    )

    private val installStages = listOf(
        InstallStageInfo(ProgressEntity.InstallResolving::class, 1f),    // Short
        InstallStageInfo(ProgressEntity.InstallPreparing::class, 4f),    // Long, with sub-progress
        InstallStageInfo(ProgressEntity.InstallAnalysing::class, 1f),     // Short
        InstallStageInfo(ProgressEntity.Installing::class, 4f)         // Long
    )

    private val totalProgressWeight = installStages.sumOf { it.weight.toDouble() }.toFloat()

    /**
     * Builds and returns a Notification for Android 15+ devices.
     * This is the entry point for the modern notification logic.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun buildModernNotification(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported) {
            return null
        }
        val builder = newModernNotificationBuilder(progress, showDialog, preferSystemIcon)

        // Finalize notification content for terminal states, which might require async operations.
        return when (progress) {
            is ProgressEntity.InstallFailed -> onInstallFailed(builder).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder).build()
            else -> builder.build()
        }
    }

    /**
     * Creates and configures a NotificationCompat.Builder using rich ProgressStyle.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun newModernNotificationBuilder(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false
    ): NotificationCompat.Builder {
        val channel = NotificationChannelCompat.Builder(
            Channel.InstallerLiveChannel.value,
            NotificationManagerCompat.IMPORTANCE_MAX
        ).setName(getString(R.string.installer_live_channel_name)).build()
        notificationManager.createNotificationChannel(channel)
        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification,
            ConfigEntity.InstallMode.AutoNotification -> if (showDialog) openIntent else null

            else -> openIntent
        }
        val baseBuilder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(Icon.LOGO.resId)
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)

        // --- New Weighted & Dynamic Progress Logic ---
        // Find the current stage's information.
        val currentStageIndex = installStages.indexOfFirst { it.progressClass.isInstance(progress) }
        // Create the raw, uncolored segments with correct lengths.
        val segments = createInstallSegments(installStages)
        // Create the ProgressStyle.
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(segments)
            .setStyledByProgress(true)
        // Calculate the precise progress value.
        var contentTitle: String
        var shortText: String? = null
        // Calculate the total weight of all stages *before* the current one.
        val previousStagesWeight = if (currentStageIndex > 0) {
            installStages.subList(0, currentStageIndex).sumOf { it.weight.toDouble() }.toFloat()
        } else {
            0f
        }

        when (progress) {
            is ProgressEntity.InstallResolving, is ProgressEntity.InstallResolveSuccess -> {
                contentTitle = getString(R.string.installer_resolving)
                shortText = getString(R.string.installer_live_channel_short_text_resolving)
                // Progress is half of the current (first) segment's weight.
                val progressValue = previousStagesWeight + (installStages[0].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallPreparing -> {
                contentTitle = getString(R.string.installer_preparing)
                shortText = getString(R.string.installer_live_channel_short_text_preparing)
                // Real-time progress within this segment.
                val progressInCurrentSegment = installStages[1].weight * progress.progress
                val progressValue = previousStagesWeight + progressInCurrentSegment
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallResolvedFailed -> {
                contentTitle = getString(R.string.installer_resolve_failed)
                shortText = getString(R.string.installer_live_channel_short_text_resolve_failed)

                baseBuilder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false)
                    .addAction(0, getString(R.string.cancel), finishIntent)
            }

            is ProgressEntity.InstallAnalysing -> {
                contentTitle = getString(R.string.installer_analysing)
                shortText = getString(R.string.installer_live_channel_short_text_analysing)
                // Progress is the sum of previous segments + half of the current one.
                val progressValue = previousStagesWeight + (installStages[2].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                contentTitle = installer.analysisResults.flatMap { it.appEntities }
                    .filter { it.selected }.map { it.app }.getInfo(context).title
                shortText = getString(R.string.installer_live_channel_short_text_pending)
                // Progress is now at the end of the Analysing stage.
                val progressValue = installStages.subList(0, 3).sumOf { it.weight.toDouble() }.toFloat()
                progressStyle.setProgress(progressValue.toInt())
                baseBuilder.setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                    .addAction(0, getString(R.string.install), installIntent)
                    .addAction(0, getString(R.string.cancel), finishIntent)
                    .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            is ProgressEntity.Installing -> {
                contentTitle = installer.analysisResults.flatMap { it.appEntities }
                    .filter { it.selected }.map { it.app }.getInfo(context).title
                shortText = getString(R.string.installer_live_channel_short_text_installing)
                // Progress is the sum of previous segments + half of the current one.
                val progressValue = previousStagesWeight + (installStages[3].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
                baseBuilder.setContentText(getString(R.string.installer_installing))
            }

            is ProgressEntity.InstallSuccess -> {
                contentTitle = getString(R.string.installer_install_success)
                shortText = getString(R.string.installer_live_channel_short_text_success)
                progressStyle.setProgress(totalProgressWeight.toInt())
                baseBuilder.setOnlyAlertOnce(false).setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            is ProgressEntity.InstallFailed -> {
                contentTitle = getString(R.string.installer_install_failed)
                shortText = getString(R.string.installer_live_channel_short_text_install_failed)
                progressStyle.setProgress(previousStagesWeight.toInt())
                baseBuilder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false)
            }

            else -> {
                contentTitle = getString(R.string.installer_ready)
                progressStyle.setProgress(0)
            }
        }

        baseBuilder.setContentTitle(contentTitle)
        shortText?.let { baseBuilder.setShortCriticalText(it) }
        baseBuilder.setStyle(progressStyle)

        return baseBuilder
    }

    /**
     * Creates a list of styled segments for the notification progress bar based on the current install stage.
     * @param stages The ordered list of installation stages.
     * @return A list of NotificationCompat.ProgressStyle.Segment objects.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createInstallSegments(
        stages: List<InstallStageInfo>
    ): List<NotificationCompat.ProgressStyle.Segment> {
        return stages.map { stageInfo ->
            // The length of each segment is now its defined weight.
            NotificationCompat.ProgressStyle.Segment(stageInfo.weight.toInt())
        }
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
        LOGO(R.drawable.ic_notification_logo),
        Working(R.drawable.round_hourglass_empty_black_24),
        Pausing(R.drawable.round_hourglass_disabled_black_24)
    }

    private val notificationChannels = mapOf(
        Channel.InstallerChannel to NotificationChannelCompat.Builder(
            Channel.InstallerChannel.value,
            NotificationManagerCompat.IMPORTANCE_HIGH // Use HIGH for final pop-up
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
    private suspend fun getLargeIconBitmap(preferSystemIcon: Boolean): Bitmap? {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val entityToInstall = entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            ?: entities.sortedBest().firstOrNull()
            ?: return null // Return null if no suitable entity is found

        // Use standard notification large icon dimensions
        val iconSizePx =
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        // Get the drawable from the central repository
        val drawable = appIconRepo.getIcon(
            sessionId = installer.id,
            packageName = entityToInstall.packageName,
            entityToInstall = entityToInstall,
            iconSizePx = iconSizePx,
            preferSystemIcon = preferSystemIcon
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
        showDialog: Boolean,
        preferSystemIcon: Boolean = false
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
            is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess(builder, preferSystemIcon)
            is ProgressEntity.Installing -> onInstalling(builder, preferSystemIcon)
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
        builder.setContentTitle(getString(R.string.installer_resolving)).build()

    private fun onPreparing(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.InstallPreparing
    ) =
        builder.setContentTitle(getString(R.string.installer_prepare_install))
            .setProgress(100, (progress.progress * 100).toInt(), progress.progress < 0)
            .build()

    private fun onResolvedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolve_failed))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onResolveSuccess(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolve_success))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onAnalysing(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_analysing)).build()

    private fun onAnalysedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_analyse_failed))
            .addAction(0, getString(R.string.retry), analyseIntent)
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private suspend fun onAnalysedSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): Notification {
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
                .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                .addAction(0, getString(R.string.install), installIntent)
                .addAction(0, getString(R.string.cancel), finishIntent)
        }).build()
    }

    private suspend fun onInstalling(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): Notification {
        val info = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .getInfo(context)
        return builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_installing))
            .setLargeIcon(getLargeIconBitmap(preferSystemIcon)).build()
    }

    private fun onInstallFailed(
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
                "$contentText\n" + reason
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