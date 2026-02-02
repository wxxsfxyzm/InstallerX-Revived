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
import com.rosan.installer.data.app.model.enums.DataType
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
import com.rosan.installer.util.hasFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.reflect.KClass

class ForegroundInfoHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    companion object {
        private const val MINIMUM_VISIBILITY_DURATION_MS = 400L

        // Throttle notification updates to prevent system rate limiting
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L

        // For progress updates, use a slightly higher threshold
        private const val PROGRESS_UPDATE_THRESHOLD = 0.03f // Update every 3% change
    }

    private data class NotificationSettings(
        val showDialog: Boolean,
        val showLiveActivity: Boolean,
        val autoCloseNotification: Int,
        val preferSystemIcon: Boolean,
        val preferDynamicColor: Boolean
    )

    private data class NotificationState(
        val progress: ProgressEntity,
        val background: Boolean,
        val tick: Unit // Only used to trigger a notification update
    )

    private val baseNotificationBuilder by lazy {
        NotificationCompat.Builder(context, Channel.InstallerLiveChannel.value)
            .setSmallIcon(Icon.LOGO.resId)
            .setSilent(true)
            .setOnlyAlertOnce(true) // 默认闭嘴，静默更新
            .setOngoing(true)       // 默认常驻
            .setRequestPromotedOngoing(true)
    }

    private var job: Job? = null
    private var sessionStartTime: Long = 0L
    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()
    private val appIconRepo by inject<AppIconRepo>()

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = installer.id.hashCode() and Int.MAX_VALUE

    // Throttling state
    private var lastNotificationUpdateTime = 0L
    private var lastProgressValue = -1f
    private var lastProgressClass: KClass<out ProgressEntity>? = null
    private var lastLogLine: String? = null

    // State for comparing last notified entity
    private var lastNotifiedEntity: ProgressEntity? = null

    // State for fake progress animation
    private var currentInstallKey: String? = null
    private var currentInstallStartTime: Long = 0L

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to combine and collect flows.")

        createNotificationChannels()

        sessionStartTime = System.currentTimeMillis()

        val settings = NotificationSettings(
            showDialog = appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true).first(),
            showLiveActivity = appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false).first(),
            autoCloseNotification = appDataStore.getInt(AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS, 0).first(),
            preferSystemIcon = appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false).first(),
            preferDynamicColor = appDataStore.getBoolean(AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
        )

        // Ticker flow for animation (approx 30fps cap for calculation, throttled by setNotificationThrottled)
        // Actual UI update rate is limited by NOTIFICATION_UPDATE_INTERVAL_MS (500ms)
        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(200) // Trigger check every 200ms
            }
        }

        job = scope.launch {
            combine(
                installer.progress,
                installer.background,
                ticker
            ) { progress, background, tick ->
                NotificationState(progress, background, tick)
            }.distinctUntilChanged { old, new ->
                if (old.progress != new.progress || old.background != new.background) {
                    return@distinctUntilChanged false
                }
                val isAnimating = new.progress is ProgressEntity.Installing && new.background
                return@distinctUntilChanged !isAnimating
            }.collect { state ->
                val progress = state.progress
                val background = state.background

                if (progress !is ProgressEntity.InstallingModule) {
                    Timber.i("[id=${installer.id}] Combined Flow: progress=${progress::class.simpleName}, background=$background")
                }

                var fakeItemProgress = 0f
                if (progress is ProgressEntity.Installing) {
                    val key = "${progress.current}|${progress.total}|${progress.appLabel}"
                    if (currentInstallKey != key) {
                        currentInstallKey = key
                        currentInstallStartTime = System.currentTimeMillis()
                    }
                    val elapsed = System.currentTimeMillis() - currentInstallStartTime
                    // Asymptotic curve: Starts fast, slows down, caps at 0.95
                    // Formula: 1 - 1 / (1 + t/3000) roughly maps 0s->0, 3s->0.5, 9s->0.75...
                    fakeItemProgress = (1f - 1f / (1f + elapsed / 3000f)).coerceAtMost(0.95f)
                } else {
                    currentInstallKey = null
                }

                if (progress is ProgressEntity.InstallAnalysedUnsupported) {
                    Timber.w("[id=${installer.id}] AnalysedUnsupported: ${progress.reason}")
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show()
                    }
                    installer.close()
                    return@collect
                }

                // --- Skip the "Confirm Install" notification in AutoNotification mode ---
                // Instead of cancelling it (which causes a blink), we simply return@combine.
                // This keeps the previous notification (e.g. "Analysing") visible for a split second
                // until the "Installing" state arrives and overwrites it.
                if (progress is ProgressEntity.InstallAnalysedSuccess &&
                    installer.config.installMode == ConfigEntity.InstallMode.AutoNotification
                ) {
                    Timber.d("[id=${installer.id}] AutoNotification mode detected. Skipping interactive InstallAnalysedSuccess notification.")
                    return@collect
                }

                if (background) {
                    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                    val isSameState = lastNotifiedEntity?.let { it::class == progress::class } == true
                    val notification =
                        if (isModernEligible && settings.showLiveActivity) {
                            buildModernNotification(
                                progress = progress,
                                showDialog = settings.showDialog,
                                preferSystemIcon = settings.preferSystemIcon,
                                preferDynamicColor = settings.preferDynamicColor,
                                fakeItemProgress = fakeItemProgress,
                                isSameState = isSameState
                            )
                        } else {
                            buildLegacyNotification(progress, true, settings.showDialog, settings.preferSystemIcon)
                        }

                    // Use throttled update for notification
                    setNotificationThrottled(notification, progress)

                    if (progress is ProgressEntity.InstallSuccess || (progress is ProgressEntity.InstallCompleted && progress.results.all { it.success })) {
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
                        if (progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess && progress !is ProgressEntity.InstallCompleted) {
                            delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                        }
                    }
                } else {
                    Timber.d("[id=${installer.id}] Foreground mode. Cancelling notification.")
                    setNotificationThrottled(null, progress)
                }
            }
        }
    }

    private fun createNotificationChannels() {
        // Batch create channels
        val channels = listOf(
            NotificationChannelCompat.Builder(
                Channel.InstallerChannel.value,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName(getString(R.string.installer_channel_name))
                .setDescription(getString(R.string.installer_channel_name_desc))
                .build(),

            NotificationChannelCompat.Builder(
                Channel.InstallerProgressChannel.value,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName(getString(R.string.installer_progress_channel_name))
                .setDescription(getString(R.string.installer_progress_channel_name_desc))
                .build(),

            NotificationChannelCompat.Builder(
                Channel.InstallerLiveChannel.value,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName(getString(R.string.installer_live_channel_name))
                .setDescription(getString(R.string.installer_live_channel_name_desc))
                .build()
        )
        channels.forEach { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Throttled notification update that respects system rate limits
     * - For critical states (success, failure): update immediately
     * - For progress updates: throttle to avoid rate limiting
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotificationThrottled(notification: Notification?, progress: ProgressEntity) {
        if (notification == null) {
            setNotificationImmediate(null)
            lastProgressValue = -1f  // Reset state
            lastProgressClass = null // Reset state class
            lastLogLine = null       // Reset module log
            lastNotifiedEntity = null
            lastNotificationUpdateTime = 0
            return
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastNotificationUpdateTime

        // Critical states that should update immediately
        val isCriticalState = progress is ProgressEntity.InstallSuccess ||
                progress is ProgressEntity.InstallFailed ||
                progress is ProgressEntity.InstallCompleted || // Added for batch
                progress is ProgressEntity.InstallAnalysedSuccess ||
                progress is ProgressEntity.InstallResolvedFailed ||
                progress is ProgressEntity.InstallAnalysedFailed

        // Transition Detection (Always Update)
        // If we are switching from any other state to Installing, this is the "Start" of installation.
        // We must show this immediately to let the user know installation has begun.
        // If we are already in Installing (e.g. batch item 1 -> batch item 2), this will be false, allowing throttling.
        val isEnteringInstalling = progress is ProgressEntity.Installing && lastProgressClass != ProgressEntity.Installing::class

        // Specific logic for Module Install (Log based throttling)
        if (progress is ProgressEntity.InstallingModule) {
            val currentLine = progress.output.lastOrNull()
            if (currentLine != lastLogLine && timeSinceLastUpdate > NOTIFICATION_UPDATE_INTERVAL_MS) {
                lastLogLine = currentLine
                setNotificationImmediate(notification)
                lastNotificationUpdateTime = currentTime
            }
            return
        }

        val currentProgress = (progress as? ProgressEntity.InstallPreparing)?.progress ?: -1f

        val shouldUpdate = when {
            // For critical states, updates must only occur when the state entity actually changes.
            // This prevents redundant refreshes triggered by the ticker.
            isCriticalState -> progress != lastNotifiedEntity
            isEnteringInstalling -> true
            timeSinceLastUpdate < NOTIFICATION_UPDATE_INTERVAL_MS -> false
            progress is ProgressEntity.Installing -> true // Installing state (batch or single) should update per item or progress
            currentProgress < 0 -> true  // Not progress
            else -> {
                val progressIncreased = currentProgress > lastProgressValue
                val significantChange = (currentProgress - lastProgressValue) >= PROGRESS_UPDATE_THRESHOLD
                progressIncreased && (significantChange || currentProgress >= 0.99f)
            }
        }

        if (shouldUpdate) {
            setNotificationImmediate(notification)
            lastNotificationUpdateTime = currentTime
            if (currentProgress >= 0) lastProgressValue = currentProgress
            // Update the tracked class only when we actually notify
            lastProgressClass = progress::class
            lastNotifiedEntity = progress
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotificationImmediate(notification: Notification?) {
        if (notification == null) {
            Timber.d("[id=${installer.id}] setNotification: Cancelling notification with id: $notificationId")
            notificationManager.cancel(notificationId)
            return
        }
        notificationManager.notify(notificationId, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cancelling notification and job.")
        installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app.packageName }
            .distinct()
            .forEach { appIconRepo.clearCacheForPackage(it) }
        setNotificationImmediate(null)
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
        preferDynamicColor: Boolean = false,
        fakeItemProgress: Float = 0f,
        isSameState: Boolean = false
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported)
            return null

        val builder = newModernNotificationBuilder(progress, showDialog, preferSystemIcon, preferDynamicColor, fakeItemProgress, isSameState)

        return when (progress) {
            is ProgressEntity.InstallFailed -> onInstallFailed(builder, preferSystemIcon).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder, preferSystemIcon).build()
            is ProgressEntity.InstallCompleted -> onInstallCompleted(builder, progress).build()
            else -> builder.build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun newModernNotificationBuilder(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false,
        preferDynamicColor: Boolean = false,
        fakeItemProgress: Float = 0f,
        isSameState: Boolean = false
    ): NotificationCompat.Builder {
        val baseBuilder = baseNotificationBuilder
        baseBuilder.clearActions()
        baseBuilder.setOnlyAlertOnce(true)
        baseBuilder.setSilent(true)
        baseBuilder.setOngoing(true)
        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification,
            ConfigEntity.InstallMode.AutoNotification -> if (showDialog) openIntent else null

            else -> openIntent
        }
        val isDarkTheme = context.resources.configuration.uiMode.hasFlag(Configuration.UI_MODE_NIGHT_YES)
        val brandColor = if (isDarkTheme) primaryDark else primaryLight

        baseBuilder.setColor(brandColor.toArgb())
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)

        val seedColorInt = getCurrentSeedColor(progress)

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

                baseBuilder.addAction(0, getString(R.string.cancel), cancelIntent)
            }

            is ProgressEntity.InstallResolvedFailed -> {
                contentTitle = getString(R.string.installer_resolve_failed)
                shortText = getString(R.string.installer_live_channel_short_text_resolve_failed)
                baseBuilder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false)
                    .setOnlyAlertOnce(false)
                    .setSilent(false)
                    .addAction(0, getString(R.string.cancel), finishIntent)
            }

            is ProgressEntity.InstallAnalysing -> {
                contentTitle = getString(R.string.installer_analysing)
                shortText = getString(R.string.installer_live_channel_short_text_analysing)
                val progressValue = previousStagesWeight + (installStages[2].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                baseBuilder.setOnlyAlertOnce(isSameState).setSilent(false)
                val allEntities = installer.analysisResults.flatMap { it.appEntities }
                val selectedApps = allEntities.map { it.app }
                val hasComplexType = allEntities.any {
                    it.app.sourceType == DataType.MIXED_MODULE_APK ||
                            it.app.sourceType == DataType.MIXED_MODULE_ZIP
                }
                val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

                shortText = if (hasComplexType || isMultiPackage) getString(R.string.installer_live_channel_short_text_pending)
                else getString(R.string.installer_live_channel_short_text_pending_install)
                val progressValue = installStages.subList(0, 3).sumOf { it.weight.toDouble() }.toFloat()
                progressStyle.setProgress(progressValue.toInt())

                if (hasComplexType) {
                    contentTitle = getString(R.string.installer_prepare_install)
                    baseBuilder.setContentText(getString(R.string.installer_mixed_module_apk_description_notification))
                        .addAction(0, getString(R.string.cancel), finishIntent)
                } else if (isMultiPackage) {
                    // Multiple packages: Show generic title and require user interaction to select.
                    // Consistent with legacy onAnalysedSuccess logic.
                    contentTitle = getString(R.string.installer_prepare_install)
                    // Do not add 'Install' action here. User must click the notification content to open the dialog.
                    baseBuilder
                        .setContentText(getString(R.string.installer_multi_apk_description_notification))
                        .addAction(0, getString(R.string.cancel), finishIntent)
                } else {
                    // Single package: Show specific app info and install action.
                    contentTitle = selectedApps.getInfo(context).title
                    baseBuilder.setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                        .addAction(0, getString(R.string.install), installIntent)
                        .addAction(0, getString(R.string.cancel), finishIntent)
                    baseBuilder.setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                }
            }

            // Handle Batch and Detailed Installing
            is ProgressEntity.Installing -> {
                val isBatch = progress.total > 1
                val appLabel = progress.appLabel ?: getString(R.string.installer_installing)

                contentTitle = if (isBatch) {
                    getString(R.string.installer_installing) + " (${progress.current}/${progress.total}): $appLabel"
                } else {
                    appLabel
                }

                shortText = getString(R.string.installer_live_channel_short_text_installing)

                // Calculate progress: Base + (Segment * Fraction)
                val segmentWeight = installStages[3].weight

                // Logic:
                // If Single: fraction = fakeItemProgress (0 -> 0.95)
                // If Batch: fraction = ( (current - 1) + fakeItemProgress ) / total
                val total = progress.total.coerceAtLeast(1).toFloat()
                val currentBase = (progress.current - 1).coerceAtLeast(0).toFloat()
                val batchFraction = (currentBase + fakeItemProgress) / total

                val progressValue = previousStagesWeight + (segmentWeight * batchFraction)

                progressStyle.setProgress(progressValue.toInt())
                baseBuilder.setContentText(getString(R.string.installer_installing))

                val currentBatchIndex = if (isBatch) progress.current - 1 else null
                baseBuilder.setLargeIcon(getLargeIconBitmap(preferSystemIcon, currentBatchIndex))
            }

            // Handle Module Installation
            is ProgressEntity.InstallingModule -> {
                contentTitle = getString(R.string.installer_installing)
                shortText = getString(R.string.installer_live_channel_short_text_installing)

                // Module installs don't have percentage, so we set it to the start of the "Installing" segment
                // to show we are in the active installation phase.
                val progressValue = previousStagesWeight + (installStages[3].weight * 0.1f)
                progressStyle.setProgress(progressValue.toInt())

                val lastLog = progress.output.lastOrNull() ?: getString(R.string.installer_installing)
                baseBuilder.setContentText(lastLog)

                baseBuilder.setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            is ProgressEntity.InstallSuccess -> {
                contentTitle = getString(R.string.installer_install_success)
                shortText = getString(R.string.installer_live_channel_short_text_success)
                progressStyle.setProgress(totalProgressWeight.toInt())

                // Show icon for single install
                baseBuilder.setOnlyAlertOnce(false)
                    .setSilent(false)
                    .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            // Case 2: Batch Install Completed
            is ProgressEntity.InstallCompleted -> {
                val successCount = progress.results.count { it.success }
                val totalCount = progress.results.size

                // Calculate title with ratio
                contentTitle = if (successCount == totalCount) {
                    getString(R.string.installer_install_success)
                } else {
                    "${getString(R.string.installer_install_success)}: $successCount/$totalCount"
                }

                // Calculate short text with ratio for Live Activity / Capsule view
                shortText = if (successCount == totalCount) {
                    getString(R.string.installer_live_channel_short_text_success)
                } else {
                    "$successCount/$totalCount ${getString(R.string.installer_live_channel_short_text_success)}"
                }

                progressStyle.setProgress(totalProgressWeight.toInt())

                // Do not set large icon for batch completion
                baseBuilder.setOnlyAlertOnce(false).setSilent(false)
            }

            is ProgressEntity.InstallFailed -> {
                contentTitle = getString(R.string.installer_install_failed)
                shortText = getString(R.string.installer_live_channel_short_text_install_failed)
                progressStyle.setProgress(previousStagesWeight.toInt())
                baseBuilder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false).setSilent(false)
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

    enum class Channel(val value: String) {
        InstallerChannel("installer_channel"),
        InstallerProgressChannel("installer_progress_channel"),
        InstallerLiveChannel("installer_live_channel")
    }

    enum class Icon(@param:DrawableRes val resId: Int) {
        LOGO(R.drawable.ic_notification_logo),
        Working(R.drawable.round_hourglass_empty_24),
        Pausing(R.drawable.round_hourglass_disabled_24)
    }

    // Support fetching specific icon from Multi-Install Queue
    private suspend fun getLargeIconBitmap(preferSystemIcon: Boolean, currentBatchIndex: Int? = null): Bitmap? {
        // Priority 1: If explicit batch index provided and queue is valid
        val entityFromQueue = if (currentBatchIndex != null && installer.multiInstallQueue.isNotEmpty()) {
            installer.multiInstallQueue.getOrNull(currentBatchIndex)?.app
        } else null

        // Priority 2: Standard selection (Single install or fallback)
        val entityToInstall = if (entityFromQueue != null) {
            entityFromQueue
        } else {
            val entities = installer.analysisResults
                .flatMap { it.appEntities }
                .filter { it.selected }
                .map { it.app }
            entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                ?: entities.sortedBest().firstOrNull()
        } ?: return null

        val iconSizePx =
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        // Note: For multi-install, the packageName might not be in analysisResults map keys effectively if handled via temp results,
        // but AppIconRepo should handle it via the AppEntity.
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
            // Modified: Pass progress to onInstalling
            is ProgressEntity.Installing -> onInstalling(builder, progress, preferSystemIcon)
            // Added: Module support
            is ProgressEntity.InstallingModule -> onInstallingModule(builder, progress, preferSystemIcon)
            is ProgressEntity.InstallFailed -> onInstallFailed(builder, preferSystemIcon).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder, preferSystemIcon).build()
            // Added: Batch Completed
            is ProgressEntity.InstallCompleted -> onInstallCompleted(builder, progress).build()

            is ProgressEntity.Finish, is ProgressEntity.Error, is ProgressEntity.InstallAnalysedUnsupported -> null
            else -> null
        }
    }

    private fun newLegacyNotificationBuilder(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): NotificationCompat.Builder {
        val isWorking = when (progress) {
            is ProgressEntity.Ready,
            is ProgressEntity.InstallResolving,
            is ProgressEntity.InstallResolveSuccess,
            is ProgressEntity.InstallAnalysing,
            is ProgressEntity.InstallAnalysedSuccess,
            is ProgressEntity.Installing,
            is ProgressEntity.InstallingModule, // Added
            is ProgressEntity.InstallSuccess,
            is ProgressEntity.InstallCompleted -> true // Added

            else -> false
        }

        val isImportance = when (progress) {
            is ProgressEntity.InstallResolvedFailed,
            is ProgressEntity.InstallAnalysedFailed,
            is ProgressEntity.InstallAnalysedSuccess,
            is ProgressEntity.InstallFailed,
            is ProgressEntity.InstallSuccess,
            is ProgressEntity.InstallCompleted -> true

            else -> false
        }

        val channelEnum = if (isImportance && background) {
            Channel.InstallerChannel
        } else {
            Channel.InstallerProgressChannel
        }

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

        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed || progress is ProgressEntity.InstallCompleted) {
            builder.setOngoing(false)
                .setOnlyAlertOnce(false)
        }

        val legacyProgressValue = when (progress) {
            is ProgressEntity.InstallResolving -> 0
            is ProgressEntity.InstallAnalysing -> 40
            is ProgressEntity.Installing -> {
                // Map Installing phase (single or batch) to 50-90 range
                val base = 50
                val range = 40
                val fraction = if (progress.total > 0) progress.current.toFloat() / progress.total.toFloat() else 0.5f
                base + (range * fraction).toInt()
            }

            is ProgressEntity.InstallingModule -> 70 // Arbitrary working state
            else -> null
        }

        legacyProgressValue?.let {
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

    private val cancelIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Cancel)

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
        builder.setContentTitle(getString(R.string.installer_preparing))
            .setContentText(getString(R.string.installer_preparing_desc))
            .setProgress(100, (progress.progress * 100).toInt(), progress.progress < 0)
            .addAction(0, getString(R.string.cancel), cancelIntent)
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
        val allEntities = installer.analysisResults.flatMap { it.appEntities }
        val selectedApps = allEntities.map { it.app }
        val hasComplexType = allEntities.any {
            it.app.sourceType == DataType.MIXED_MODULE_APK ||
                    it.app.sourceType == DataType.MIXED_MODULE_ZIP
        }
        val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

        if (hasComplexType) {
            return builder.setContentTitle(getString(R.string.installer_prepare_install))
                .setContentText(getString(R.string.installer_mixed_module_apk_description_notification))
                .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                .addAction(0, getString(R.string.cancel), finishIntent)
                .build()
        }

        return (if (isMultiPackage) {
            builder.setContentTitle(getString(R.string.installer_prepare_install))
                .setContentText(getString(R.string.installer_multi_apk_description_notification))
                .addAction(0, getString(R.string.cancel), finishIntent)
        } else {
            builder.setContentTitle(selectedApps.getInfo(context).title)
                .setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                .addAction(0, getString(R.string.install), installIntent)
                .addAction(0, getString(R.string.cancel), finishIntent)
        }).build()
    }

    // === Modified: Handle detailed batch install info ===
    private suspend fun onInstalling(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.Installing,
        preferSystemIcon: Boolean
    ): Notification {
        val isBatch = progress.total > 1
        val appLabel = progress.appLabel ?: getString(R.string.installer_installing)

        val title = if (isBatch) {
            "${getString(R.string.installer_installing)} (${progress.current}/${progress.total})"
        } else {
            appLabel
        }

        val content = if (isBatch) appLabel else getString(R.string.installer_installing)

        val currentBatchIndex = if (isBatch) progress.current - 1 else null

        return builder.setContentTitle(title)
            .setContentText(content)
            .setLargeIcon(getLargeIconBitmap(preferSystemIcon, currentBatchIndex))
            .build()
    }

    // === Added: Handle Module install ===
    private suspend fun onInstallingModule(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.InstallingModule,
        preferSystemIcon: Boolean
    ): Notification {
        val lastLog = progress.output.lastOrNull() ?: getString(R.string.installer_installing)

        return builder.setContentTitle(getString(R.string.installer_installing))
            .setContentText(lastLog)
            .setProgress(100, 50, true) // Indeterminate progress for modules usually
            .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            .build()
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

    // === Added: Handle Batch Completion ===
    private fun onInstallCompleted(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.InstallCompleted
    ): NotificationCompat.Builder {
        val successCount = progress.results.count { it.success }
        val totalCount = progress.results.size

        val title = if (successCount == totalCount) {
            getString(R.string.installer_install_success)
        } else {
            // E.g. "Completed: 3 success, 1 failed"
            // You might want to add a specific resource string for this
            "${getString(R.string.installer_install_success)}: $successCount/$totalCount"
        }

        return builder.setContentTitle(title)
            .setContentText(getString(R.string.installer_live_channel_short_text_success))
            .addAction(0, getString(R.string.finish), finishIntent)
    }

    /**
     * Helper to retrieve the seed color dynamically.
     * If in batch install mode, it fetches the color of the app currently being installed.
     * Otherwise, it falls back to the first available color in the results.
     */
    private fun getCurrentSeedColor(progress: ProgressEntity): Int? {
        if (progress is ProgressEntity.Installing && progress.total > 1) {
            // 0-based index for the queue
            val index = progress.current - 1
            val currentEntity = installer.multiInstallQueue.getOrNull(index)

            if (currentEntity != null) {
                // Find the analysis result for the current package to get its specific color
                val result = installer.analysisResults.find { it.packageName == currentEntity.app.packageName }
                if (result?.seedColor != null) {
                    return result.seedColor
                }
            }
        }

        // Fallback: Use the first available color (default behavior for single install or unknown state)
        return installer.analysisResults.firstNotNullOfOrNull { it.seedColor }
    }
}