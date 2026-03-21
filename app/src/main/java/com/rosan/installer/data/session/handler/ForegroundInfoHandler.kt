// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.rosan.installer.R
import com.rosan.installer.data.session.notification.LegacyNotificationBuilder
import com.rosan.installer.data.session.notification.MiIslandNotificationBuilder
import com.rosan.installer.data.session.notification.ModernNotificationBuilder
import com.rosan.installer.data.session.notification.NotificationHelper
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.usecase.config.GetResolvedConfigUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.reflect.KClass

class ForegroundInfoHandler(scope: CoroutineScope, session: InstallerSessionRepository) :
    Handler(scope, session), KoinComponent {

    companion object {
        private const val MINIMUM_VISIBILITY_DURATION_MS = 400L
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L
        private const val PROGRESS_UPDATE_THRESHOLD = 0.03f
        private const val XMSF_PACKAGE_NAME = "com.xiaomi.xmsf"

        // The duration to keep the network blocked to bypass Xiaomi's notification scanner
        private const val XIAOMI_MAGIC_BLIND_WINDOW_MS = 100L
    }

    private data class NotificationSettings(
        val showDialog: Boolean,
        val showLiveActivity: Boolean,
        val showMiIsland: Boolean,
        val autoCloseNotification: Int,
        val preferSystemIcon: Boolean,
        val preferDynamicColor: Boolean
    )

    private data class NotificationState(val progress: ProgressEntity, val background: Boolean, val tick: Unit)

    private var job: Job? = null
    private var sessionStartTime: Long = 0L
    private val context by inject<Context>()
    private val appSettingsRepo by inject<AppSettingsRepo>()
    private val appOpsProvider by inject<AppOpsProvider>()
    private val getResolvedConfig by inject<GetResolvedConfigUseCase>()
    private val getAppIcon by inject<GetAppIconUseCase>()

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = session.id.hashCode() and Int.MAX_VALUE

    // Throttling state
    private var lastNotificationUpdateTime = 0L
    private var lastProgressValue = -1f
    private var lastProgressClass: KClass<out ProgressEntity>? = null
    private var lastLogLine: String? = null
    private var lastNotifiedEntity: ProgressEntity? = null
    private var currentInstallKey: String? = null
    private var currentInstallStartTime: Long = 0L

    // Mi island magic
    private lateinit var globalAuthorizer: Authorizer
    private val networkMutex = Mutex()
    private var isXiaomiNetworkBlocked = false
    private val xmsfUid: Int? by lazy {
        try {
            context.packageManager.getPackageUid(XMSF_PACKAGE_NAME, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Target package not found, UID magic will be skipped.")
            null
        }
    }

    // Initialize Delegated Builders
    private val helper by lazy { NotificationHelper(context, session, getAppIcon) }
    private val miIslandBuilder by lazy { MiIslandNotificationBuilder(context, session, helper) }
    private val legacyBuilder by lazy { LegacyNotificationBuilder(context, session, helper) }
    private val modernBuilder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) ModernNotificationBuilder(context, session, helper) else null
    }

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        Timber.d("[id=${session.id}] onStart: Starting to combine and collect flows.")
        createNotificationChannels()
        sessionStartTime = System.currentTimeMillis()
        isXiaomiNetworkBlocked = false

        // Initialize synchronously in the suspend function to avoid race conditions with onFinish
        globalAuthorizer = getResolvedConfig(null).authorizer

        val settings = NotificationSettings(
            showDialog = appSettingsRepo.getBoolean(BooleanSetting.ShowDialogWhenPressingNotification, true).first(),
            showLiveActivity = appSettingsRepo.getBoolean(BooleanSetting.ShowLiveActivity, false).first(),
            showMiIsland = appSettingsRepo.getBoolean(BooleanSetting.ShowMiIsland, false).first(),
            autoCloseNotification = appSettingsRepo.getInt(IntSetting.NotificationSuccessAutoClearSeconds, 0).first(),
            preferSystemIcon = appSettingsRepo.getBoolean(BooleanSetting.PreferSystemIconForInstall, false).first(),
            preferDynamicColor = appSettingsRepo.getBoolean(BooleanSetting.LiveActivityDynColorFollowPkgIcon, false).first()
        )

        val ticker = flow {
            while (true) {
                emit(Unit)
                delay(200)
            }
        }

        // Determine if fake progress animation is actually required (only for Modern Live Activity)
        val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
        val requiresAnimation = isModernEligible && settings.showLiveActivity

        job = scope.launch {
            combine(session.progress, session.background, ticker) { progress, background, tick ->
                NotificationState(progress, background, tick)
            }.distinctUntilChanged { old, new ->
                if (old.progress != new.progress || old.background != new.background) return@distinctUntilChanged false

                // Penetrate the distinct barrier only if Live Activity animation is actively required
                if (requiresAnimation) {
                    return@distinctUntilChanged !(new.progress is ProgressEntity.Installing && new.background)
                }

                // Block ticker emissions if progress and background haven't changed
                return@distinctUntilChanged true
            }.collect { state ->
                val progress = state.progress
                val background = state.background

                var fakeItemProgress = 0f
                if (progress is ProgressEntity.Installing) {
                    val key = "${progress.current}|${progress.total}|${progress.appLabel}"
                    if (currentInstallKey != key) {
                        currentInstallKey = key
                        currentInstallStartTime = System.currentTimeMillis()
                    }
                    fakeItemProgress = (1f - 1f / (1f + (System.currentTimeMillis() - currentInstallStartTime) / 3000f)).coerceAtMost(0.95f)
                } else currentInstallKey = null

                if (progress is ProgressEntity.InstallAnalysedUnsupported) {
                    scope.launch(Dispatchers.Main) { Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show() }
                    session.close()
                    return@collect
                }

                if (progress is ProgressEntity.InstallAnalysedSuccess && session.config.installMode == InstallMode.AutoNotification) return@collect

                if (background) {
                    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                    val isSameState = lastNotifiedEntity?.let { it::class == progress::class } == true

                    val notification = if (settings.showMiIsland) {
                        miIslandBuilder.build(
                            progress,
                            true,
                            settings.showDialog,
                            settings.preferSystemIcon,
                            fakeItemProgress
                        )
                    } else if (isModernEligible && settings.showLiveActivity) {
                        modernBuilder?.build(
                            progress,
                            settings.showDialog,
                            settings.preferSystemIcon,
                            settings.preferDynamicColor,
                            fakeItemProgress,
                            isSameState
                        )
                    } else {
                        legacyBuilder.build(
                            progress,
                            true,
                            settings.showDialog,
                            settings.preferSystemIcon
                        )
                    }

                    setNotificationThrottled(notification, progress, settings.showMiIsland)

                    if (progress is ProgressEntity.InstallSuccess || (progress is ProgressEntity.InstallCompleted && progress.results.all { it.success })) {
                        scope.launch {
                            if (settings.autoCloseNotification > 0) {
                                delay(settings.autoCloseNotification * 1000L)
                                notificationManager.cancel(notificationId)
                                session.close()
                            }
                        }
                    }

                    val elapsedTime = System.currentTimeMillis() - sessionStartTime
                    if (elapsedTime < MINIMUM_VISIBILITY_DURATION_MS && progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess && progress !is ProgressEntity.InstallCompleted) {
                        delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                    }
                } else setNotificationThrottled(null, progress, false)
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
    private fun setNotificationThrottled(
        notification: Notification?,
        progress: ProgressEntity,
        isMiIsland: Boolean,
        requiresAnimation: Boolean = false
    ) {
        if (notification == null) {
            setNotificationImmediate(null)
            lastProgressValue = -1f
            lastProgressClass = null
            lastLogLine = null
            lastNotifiedEntity = null
            lastNotificationUpdateTime = 0
            return
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastNotificationUpdateTime

        val isCriticalState =
            progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed || progress is ProgressEntity.InstallCompleted || progress is ProgressEntity.InstallAnalysedSuccess || progress is ProgressEntity.InstallResolvedFailed || progress is ProgressEntity.InstallAnalysedFailed
        val isEnteringInstalling = progress is ProgressEntity.Installing && lastProgressClass != ProgressEntity.Installing::class

        // Check if the actual data inside the entity has changed (e.g., current, total, appLabel)
        val isDataChanged = progress != lastNotifiedEntity

        if (progress is ProgressEntity.InstallingModule) {
            val currentLine = progress.output.lastOrNull()
            if (currentLine != lastLogLine && timeSinceLastUpdate > NOTIFICATION_UPDATE_INTERVAL_MS) {
                lastLogLine = currentLine
                setNotificationImmediate(notification, isMiIsland)
                lastNotificationUpdateTime = currentTime
            }
            return
        }

        val currentProgress = (progress as? ProgressEntity.InstallPreparing)?.progress ?: -1f

        val shouldUpdate = when {
            isCriticalState -> isDataChanged
            isEnteringInstalling -> true

            // Unconditionally update if Installing data (e.g., batch index or label) changes
            progress is ProgressEntity.Installing && isDataChanged -> true

            // Throttle updates faster than 500ms
            timeSinceLastUpdate < NOTIFICATION_UPDATE_INTERVAL_MS -> false

            // After 500ms: only allow loop-refresh if fake animation is required by Modern Live Activity
            progress is ProgressEntity.Installing -> requiresAnimation

            currentProgress < 0 -> true
            else -> currentProgress > lastProgressValue && ((currentProgress - lastProgressValue) >= PROGRESS_UPDATE_THRESHOLD || currentProgress >= 0.99f)
        }

        if (shouldUpdate) {
            setNotificationImmediate(notification, isMiIsland)
            lastNotificationUpdateTime = currentTime
            if (currentProgress >= 0) lastProgressValue = currentProgress
            lastProgressClass = progress::class
            lastNotifiedEntity = progress
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotificationImmediate(notification: Notification?, isMiIsland: Boolean = false) {
        if (notification == null) {
            notificationManager.cancel(notificationId)
        } else {
            if (isMiIsland) {
                notifyWithXiaomiMagic(notificationId, notification)
            } else {
                notificationManager.notify(notificationId, notification)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notifyWithXiaomiMagic(notificationId: Int, notification: Notification) {
        val hasPrivilege = globalAuthorizer == Authorizer.Shizuku
        val targetUid = xmsfUid

        // Abort magic if no privilege or if the target package UID cannot be resolved
        if (!hasPrivilege || targetUid == null) {
            notificationManager.notify(notificationId, notification)
            return
        }

        // Launch asynchronously so it does not block the Flow's collect mechanism
        scope.launch(Dispatchers.IO) {
            networkMutex.withLock {
                try {
                    // 1. Block the network and update state
                    appOpsProvider.setPackageNetworkingEnabled(
                        authorizer = globalAuthorizer,
                        uid = targetUid,
                        enabled = false
                    )
                    isXiaomiNetworkBlocked = true

                    // 2. Dispatch the notification while the network is blocked
                    notificationManager.notify(notificationId, notification)

                    // 3. Maintain the blind spot long enough to outlast the asynchronous scan
                    delay(XIAOMI_MAGIC_BLIND_WINDOW_MS)

                } catch (e: Exception) {
                    Timber.e(e, "Xiaomi magic execution failed")
                } finally {
                    // Use NonCancellable to ensure network is restored even if the coroutine is cancelled
                    withContext(NonCancellable) {
                        try {
                            appOpsProvider.setPackageNetworkingEnabled(
                                authorizer = globalAuthorizer,
                                uid = targetUid,
                                enabled = true
                            )
                        } catch (restoreEx: Exception) {
                            Timber.e(restoreEx, "Failed to restore network natively")
                        } finally {
                            isXiaomiNetworkBlocked = false
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        setNotificationImmediate(null)
        job?.cancel()

        val targetUid = xmsfUid

        // Double-check initialization to be absolutely safe, though onStart now guarantees it
        if (::globalAuthorizer.isInitialized && globalAuthorizer == Authorizer.Shizuku && targetUid != null) {
            withContext(Dispatchers.IO + NonCancellable) {
                networkMutex.withLock {
                    if (isXiaomiNetworkBlocked) {
                        try {
                            appOpsProvider.setPackageNetworkingEnabled(
                                authorizer = globalAuthorizer,
                                uid = targetUid,
                                enabled = true
                            )
                            Timber.i("Restored $XMSF_PACKAGE_NAME network via onFinish fallback")
                        } catch (e: Exception) {
                            Timber.e(e, "FATAL: Failed to restore $XMSF_PACKAGE_NAME networking in onFinish")
                        } finally {
                            isXiaomiNetworkBlocked = false
                        }
                    }
                }
            }
        }
    }
}
