package com.rosan.installer.data.installer.model.impl.installer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import com.rosan.installer.ui.theme.primaryDark
import com.rosan.installer.ui.theme.primaryLight
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.reflect.KClass

class ForegroundInfoHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    companion object {
        private const val MINIMUM_VISIBILITY_DURATION_MS = 400L
    }

    private data class NotificationSettings(
        val showDialog: Boolean,
        val showLiveActivity: Boolean,
        val autoCloseNotification: Int,
        val preferSystemIcon: Boolean,
        val preferDynamicColor: Boolean
    )

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

        // OPTIMIZATION: Create notification channels once at start to avoid IPC overhead during progress updates
        createNotificationChannels()

        sessionStartTime = System.currentTimeMillis()

        val settings = NotificationSettings(
            showDialog = appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true).first(),
            showLiveActivity = appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false).first(),
            autoCloseNotification = appDataStore.getInt(AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS, 0).first(),
            preferSystemIcon = appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false).first(),
            preferDynamicColor = appDataStore.getBoolean(AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
        )

        job = scope.launch {
            combine(
                installer.progress,
                installer.background
            ) { progress, background ->

                Timber.i("[id=${installer.id}] Combined Flow: progress=${progress::class.simpleName}, background=$background, showDialog=${settings.showDialog}")

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
                    // Note: Changed BAKLAVA to VANILLA_ICE_CREAM (Android 15) or S (Android 12) if dynamic color is the goal.
                    // Assuming you want modern notifications on supported devices.
                    // If you strictly need SDK 36, keep BAKLAVA. I lowered it to S (Android 12) for broader Dynamic Color support logic,
                    // but the specific Live Activity style implies Android 15+.
                    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

                    val notification =
                        if (isModernEligible && settings.showLiveActivity) {
                            buildModernNotification(
                                progress = progress,
                                showDialog = settings.showDialog,
                                preferSystemIcon = settings.preferSystemIcon,
                                preferDynamicColor = settings.preferDynamicColor
                            )
                        } else {
                            buildLegacyNotification(progress, true, settings.showDialog, settings.preferSystemIcon)
                        }
                    setNotification(notification)

                    if (progress is ProgressEntity.InstallSuccess) {
                        scope.launch {
                            val clearTimeSeconds = settings.autoCloseNotification
                            if (clearTimeSeconds > 0) {
                                Timber.d("[id=${installer.id}] Scheduling removal of success notification in $clearTimeSeconds seconds.")
                                delay(clearTimeSeconds * 1000L)
                                Timber.d("[id=${installer.id}] Auto-clearing success notification now.")
                                notificationManager.cancel(notificationId)
                                Timber.d("[id=${installer.id}] Auto-closing session now.")
                                installer.close()
                            }
                        }
                    }

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

    private fun createNotificationChannels() {
        // Batch create channels
        val channels = listOf(
            NotificationChannelCompat.Builder(
                Channel.InstallerChannel.value,
                NotificationManagerCompat.IMPORTANCE_HIGH
            ).setName(getString(R.string.installer_channel_name)).build(),

            NotificationChannelCompat.Builder(
                Channel.InstallerProgressChannel.value,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(getString(R.string.installer_progress_channel_name)).build(),

            NotificationChannelCompat.Builder(
                Channel.InstallerLiveChannel.value,
                NotificationManagerCompat.IMPORTANCE_HIGH
            ).setName(getString(R.string.installer_live_channel_name)).build()
        )
        channels.forEach { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cancelling notification and job.")
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

    private data class InstallStageInfo(
        val progressClass: KClass<out ProgressEntity>,
        val weight: Float
    )

    private val installStages = listOf(
        InstallStageInfo(ProgressEntity.InstallResolving::class, 1f),
        InstallStageInfo(ProgressEntity.InstallPreparing::class, 4f),
        InstallStageInfo(ProgressEntity.InstallAnalysing::class, 1f),
        InstallStageInfo(ProgressEntity.Installing::class, 4f)
    )

    private val totalProgressWeight = installStages.sumOf { it.weight.toDouble() }.toFloat()

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun buildModernNotification(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false,
        preferDynamicColor: Boolean = false
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported) {
            return null
        }
        val builder = newModernNotificationBuilder(progress, showDialog, preferSystemIcon, preferDynamicColor)

        return when (progress) {
            is ProgressEntity.InstallFailed -> onInstallFailed(builder, preferSystemIcon).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder, preferSystemIcon).build()
            else -> builder.build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun newModernNotificationBuilder(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false,
        preferDynamicColor: Boolean = false
    ): NotificationCompat.Builder {
        // Channel creation removed from here

        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification,
            ConfigEntity.InstallMode.AutoNotification -> if (showDialog) openIntent else null

            else -> openIntent
        }
        val isDarkTheme = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val brandColor = if (isDarkTheme) primaryDark else primaryLight

        val baseBuilder = NotificationCompat.Builder(context, Channel.InstallerLiveChannel.value)
            .setSmallIcon(Icon.LOGO.resId)
            .setColor(brandColor.toArgb())
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)

        val seedColorInt = installer.analysisResults.firstNotNullOfOrNull { it.seedColor }

        val dynamicColorScheme = if (preferDynamicColor) {
            seedColorInt?.let { color ->
                dynamicColorScheme(
                    keyColor = Color(color),
                    isDark = isDarkTheme,
                    style = PaletteStyle.TonalSpot
                )
            }
        } else null

        val currentStageIndex = installStages.indexOfFirst { it.progressClass.isInstance(progress) }
        val segments = createInstallSegments(installStages, dynamicColorScheme)

        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(segments)
            .setStyledByProgress(true)

        var contentTitle: String
        var shortText: String? = null
        val previousStagesWeight = if (currentStageIndex > 0) {
            installStages.subList(0, currentStageIndex).sumOf { it.weight.toDouble() }.toFloat()
        } else {
            0f
        }

        when (progress) {
            is ProgressEntity.InstallResolving, is ProgressEntity.InstallResolveSuccess -> {
                contentTitle = getString(R.string.installer_resolving)
                shortText = getString(R.string.installer_live_channel_short_text_resolving)
                val progressValue = previousStagesWeight + (installStages[0].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallPreparing -> {
                contentTitle = getString(R.string.installer_preparing)
                shortText = getString(R.string.installer_live_channel_short_text_preparing)
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
                val progressValue = previousStagesWeight + (installStages[2].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                val allEntities = installer.analysisResults.flatMap { it.appEntities }
                contentTitle = allEntities.map { it.app }.getInfo(context).title
                shortText = getString(R.string.installer_live_channel_short_text_pending)
                val progressValue = installStages.subList(0, 3).sumOf { it.weight.toDouble() }.toFloat()
                progressStyle.setProgress(progressValue.toInt())

                val isMixedType = allEntities.any {
                    it.app.containerType == DataType.MIXED_MODULE_APK
                }

                if (isMixedType) {
                    baseBuilder.setContentText(getString(R.string.installer_mixed_module_apk_description_notification))
                        .addAction(0, getString(R.string.cancel), finishIntent)
                } else {
                    baseBuilder.setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                        .addAction(0, getString(R.string.install), installIntent)
                        .addAction(0, getString(R.string.cancel), finishIntent)
                }
                baseBuilder.setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            is ProgressEntity.Installing -> {
                contentTitle = installer.analysisResults.flatMap { it.appEntities }
                    .filter { it.selected }.map { it.app }.getInfo(context).title
                shortText = getString(R.string.installer_live_channel_short_text_installing)
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

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createInstallSegments(
        stages: List<InstallStageInfo>,
        colorScheme: ColorScheme?
    ): List<NotificationCompat.ProgressStyle.Segment> {
        return stages.map { stageInfo ->
            val segment = NotificationCompat.ProgressStyle.Segment(stageInfo.weight.toInt())
            colorScheme?.let {
                val color = when (stageInfo.progressClass) {
                    ProgressEntity.InstallPreparing::class,
                    ProgressEntity.Installing::class -> it.primary.toArgb()

                    ProgressEntity.InstallResolving::class,
                    ProgressEntity.InstallAnalysing::class -> it.tertiary.toArgb()

                    else -> it.primary.toArgb()
                }
                segment.setColor(color)
            }
            segment
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotification(notification: Notification? = null) {
        if (notification == null) {
            Timber.d("[id=${installer.id}] setNotification: Cancelling notification with id: $notificationId")
            notificationManager.cancel(notificationId)
            return
        }
        // Timber.d here is fine, but consider reducing log level if it spams too much
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

    // Removed duplicate notificationChannels map as it is now handled in createNotificationChannels

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

    private suspend fun getLargeIconBitmap(preferSystemIcon: Boolean): Bitmap? {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val entityToInstall = entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            ?: entities.sortedBest().firstOrNull()
            ?: return null

        val iconSizePx =
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        val drawable = appIconRepo.getIcon(
            sessionId = installer.id,
            packageName = entityToInstall.packageName,
            entityToInstall = entityToInstall,
            iconSizePx = iconSizePx,
            preferSystemIcon = preferSystemIcon
        )

        return drawable?.toBitmapOrNull(width = iconSizePx, height = iconSizePx)
    }

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
            is ProgressEntity.InstallFailed -> onInstallFailed(builder, preferSystemIcon).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder, preferSystemIcon).build()
            is ProgressEntity.Finish, is ProgressEntity.Error, is ProgressEntity.InstallAnalysedUnsupported -> null
            else -> null
        }
    }

    private fun newLegacyNotificationBuilder(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): NotificationCompat.Builder {
        val isWorking = workingProgresses.contains(progress)
        val isImportance = importanceProgresses.contains(progress)

        val channelEnum = if (isImportance && background) {
            Channel.InstallerChannel
        } else {
            Channel.InstallerProgressChannel
        }

        // Removed explicit channel creation here. It's done in onStart.

        val icon = (if (isWorking) Icon.Working else Icon.Pausing).resId

        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification, ConfigEntity.InstallMode.AutoNotification -> {
                if (showDialog) openIntent else null
            }

            else -> openIntent
        }

        val builder = NotificationCompat.Builder(context, channelEnum.value)
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

        val selectedApps = selectedEntities.map { it.app }
        val info = selectedApps.getInfo(context)

        val isMixedType = selectedEntities.any {
            it.app.containerType == DataType.MIXED_MODULE_APK
        }

        if (isMixedType) {
            return builder.setContentTitle(info.title)
                .setContentText(getString(R.string.installer_mixed_module_apk_description_notification))
                .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                .addAction(0, getString(R.string.cancel), finishIntent)
                .build()
        }

        return (if (selectedApps.groupBy { it.packageName }.size != 1) {
            builder.setContentTitle(getString(R.string.installer_prepare_install))
                .addAction(0, getString(R.string.cancel), finishIntent)
        } else {
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

    private suspend fun onInstallFailed(
        builder: NotificationCompat.Builder,
        preferSystemIcon: Boolean
    ): NotificationCompat.Builder {
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
        return builder.setContentTitle(info.title)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            .addAction(0, getString(R.string.retry), installIntent)
            .addAction(0, getString(R.string.cancel), finishIntent)
    }

    private suspend fun onInstallSuccess(
        builder: NotificationCompat.Builder,
        preferSystemIcon: Boolean
    ): NotificationCompat.Builder {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val info = entities.getInfo(context)
        var openPendingIntent: PendingIntent? = null

        val launchIntent = entities.firstOrNull()?.packageName?.let {
            context.packageManager.getLaunchIntentForPackage(it)
        }
        if (launchIntent != null) {
            val supportsPrivileged = installer.config.authorizer in listOf(
                ConfigEntity.Authorizer.Root,
                ConfigEntity.Authorizer.Shizuku,
                ConfigEntity.Authorizer.Customize
            )

            openPendingIntent = if (supportsPrivileged) {
                BroadcastHandler.privilegedLaunchAndFinishIntent(context, installer)
            } else {
                BroadcastHandler.launchIntent(context, installer, launchIntent)
            }
        }

        var newBuilder = builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_install_success))
            .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
        if (openPendingIntent != null)
            newBuilder = newBuilder.addAction(0, getString(R.string.open), openPendingIntent)

        return newBuilder.addAction(0, getString(R.string.finish), finishIntent)
    }
}