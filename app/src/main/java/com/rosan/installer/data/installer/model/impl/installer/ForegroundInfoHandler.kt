package com.rosan.installer.data.installer.model.impl.installer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.rosan.installer.R
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.impl.installer.helper.LegacyNotificationBuilder
import com.rosan.installer.data.installer.model.impl.installer.helper.ModernNotificationBuilder
import com.rosan.installer.data.installer.model.impl.installer.helper.NotificationHelper
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
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
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L
        private const val PROGRESS_UPDATE_THRESHOLD = 0.03f
    }

    private data class NotificationSettings(
        val showDialog: Boolean, val showLiveActivity: Boolean, val autoCloseNotification: Int,
        val preferSystemIcon: Boolean, val preferDynamicColor: Boolean
    )

    private data class NotificationState(val progress: ProgressEntity, val background: Boolean, val tick: Unit)

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
    private var lastNotifiedEntity: ProgressEntity? = null
    private var currentInstallKey: String? = null
    private var currentInstallStartTime: Long = 0L

    // Initialize Delegated Builders
    private val helper by lazy { NotificationHelper(context, installer, appIconRepo) }
    private val legacyBuilder by lazy { LegacyNotificationBuilder(context, installer, helper) }
    private val modernBuilder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) ModernNotificationBuilder(context, installer, helper) else null
    }

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

        val ticker = flow {
            while (true) {
                emit(Unit); delay(200)
            }
        }

        job = scope.launch {
            combine(installer.progress, installer.background, ticker) { progress, background, tick ->
                NotificationState(progress, background, tick)
            }.distinctUntilChanged { old, new ->
                if (old.progress != new.progress || old.background != new.background) return@distinctUntilChanged false
                return@distinctUntilChanged !(new.progress is ProgressEntity.Installing && new.background)
            }.collect { state ->
                val progress = state.progress
                val background = state.background

                var fakeItemProgress = 0f
                if (progress is ProgressEntity.Installing) {
                    val key = "${progress.current}|${progress.total}|${progress.appLabel}"
                    if (currentInstallKey != key) {
                        currentInstallKey = key; currentInstallStartTime = System.currentTimeMillis()
                    }
                    fakeItemProgress = (1f - 1f / (1f + (System.currentTimeMillis() - currentInstallStartTime) / 3000f)).coerceAtMost(0.95f)
                } else currentInstallKey = null

                if (progress is ProgressEntity.InstallAnalysedUnsupported) {
                    scope.launch(Dispatchers.Main) { Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show() }
                    installer.close()
                    return@collect
                }

                if (progress is ProgressEntity.InstallAnalysedSuccess && installer.config.installMode == ConfigEntity.InstallMode.AutoNotification) return@collect

                if (background) {
                    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                    val isSameState = lastNotifiedEntity?.let { it::class == progress::class } == true

                    val notification = if (isModernEligible && settings.showLiveActivity) {
                        modernBuilder?.build(
                            progress,
                            settings.showDialog,
                            settings.preferSystemIcon,
                            settings.preferDynamicColor,
                            fakeItemProgress,
                            isSameState
                        )
                    } else legacyBuilder.build(progress, true, settings.showDialog, settings.preferSystemIcon)

                    setNotificationThrottled(notification, progress)

                    if (progress is ProgressEntity.InstallSuccess || (progress is ProgressEntity.InstallCompleted && progress.results.all { it.success })) {
                        scope.launch {
                            if (settings.autoCloseNotification > 0) {
                                delay(settings.autoCloseNotification * 1000L)
                                notificationManager.cancel(notificationId)
                                installer.close()
                            }
                        }
                    }

                    val elapsedTime = System.currentTimeMillis() - sessionStartTime
                    if (elapsedTime < MINIMUM_VISIBILITY_DURATION_MS && progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess && progress !is ProgressEntity.InstallCompleted) {
                        delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                    }
                } else setNotificationThrottled(null, progress)
            }
        }
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannelCompat.Builder(NotificationHelper.Channel.InstallerChannel.value, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.installer_channel_name)).build(),
            NotificationChannelCompat.Builder(
                NotificationHelper.Channel.InstallerProgressChannel.value,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(context.getString(R.string.installer_progress_channel_name)).build(),
            NotificationChannelCompat.Builder(NotificationHelper.Channel.InstallerLiveChannel.value, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.installer_live_channel_name)).build()
        )
        channels.forEach { notificationManager.createNotificationChannel(it) }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotificationThrottled(notification: Notification?, progress: ProgressEntity) {
        if (notification == null) {
            setNotificationImmediate(null)
            lastProgressValue = -1f; lastProgressClass = null; lastLogLine = null; lastNotifiedEntity = null; lastNotificationUpdateTime = 0
            return
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastNotificationUpdateTime
        val isCriticalState =
            progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed || progress is ProgressEntity.InstallCompleted || progress is ProgressEntity.InstallAnalysedSuccess || progress is ProgressEntity.InstallResolvedFailed || progress is ProgressEntity.InstallAnalysedFailed
        val isEnteringInstalling = progress is ProgressEntity.Installing && lastProgressClass != ProgressEntity.Installing::class

        if (progress is ProgressEntity.InstallingModule) {
            val currentLine = progress.output.lastOrNull()
            if (currentLine != lastLogLine && timeSinceLastUpdate > NOTIFICATION_UPDATE_INTERVAL_MS) {
                lastLogLine = currentLine; setNotificationImmediate(notification); lastNotificationUpdateTime = currentTime
            }
            return
        }

        val currentProgress = (progress as? ProgressEntity.InstallPreparing)?.progress ?: -1f
        val shouldUpdate = when {
            isCriticalState -> progress != lastNotifiedEntity
            isEnteringInstalling -> true
            timeSinceLastUpdate < NOTIFICATION_UPDATE_INTERVAL_MS -> false
            progress is ProgressEntity.Installing -> true
            currentProgress < 0 -> true
            else -> currentProgress > lastProgressValue && ((currentProgress - lastProgressValue) >= PROGRESS_UPDATE_THRESHOLD || currentProgress >= 0.99f)
        }

        if (shouldUpdate) {
            setNotificationImmediate(notification)
            lastNotificationUpdateTime = currentTime
            if (currentProgress >= 0) lastProgressValue = currentProgress
            lastProgressClass = progress::class; lastNotifiedEntity = progress
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotificationImmediate(notification: Notification?) {
        if (notification == null) notificationManager.cancel(notificationId) else notificationManager.notify(notificationId, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app.packageName }.distinct()
            .forEach { appIconRepo.clearCacheForPackage(it) }
        setNotificationImmediate(null)
        job?.cancel()
    }
}