// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification

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
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.notification.SessionNotifier
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.framework.notification.builder.LegacyNotificationBuilder
import com.rosan.installer.framework.notification.builder.MiIslandNotificationBuilder
import com.rosan.installer.framework.notification.builder.ModernNotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.reflect.KClass

class SessionNotifierImpl(
    private val context: Context,
    private val session: InstallerSessionRepository,
    private val appSettingsRepo: AppSettingsRepository,
    private val appOps: AppOpsProvider,
    private val capabilityProvider: DeviceCapabilityProvider,
    getAppIcon: GetAppIconUseCase
) : SessionNotifier {

    companion object {
        private const val MINIMUM_VISIBILITY_DURATION_MS = 400L
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L
        private const val PROGRESS_UPDATE_THRESHOLD = 0.03f
        private const val XMSF_PACKAGE_NAME = "com.xiaomi.xmsf"
    }

    private data class NotificationSettings(
        val showDialog: Boolean,
        val showLiveActivity: Boolean,
        val showMiIsland: Boolean,
        val showMiIslandBlockingInterval: Int,
        val preferSystemIcon: Boolean,
        val preferDynamicColor: Boolean
    )

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = session.id.hashCode() and Int.MAX_VALUE
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Internal state flow to decouple data layer pushes from UI rendering ticks
    private val stateFlow = MutableStateFlow<Pair<ProgressEntity, Boolean>?>(null)
    private var isObserving = false
    private var sessionStartTime: Long = 0L

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
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    // Delegated Builders
    private val helper by lazy { NotificationHelper(context, session, getAppIcon) }
    private val miIslandBuilder by lazy { MiIslandNotificationBuilder(context, session, helper) }
    private val legacyBuilder by lazy { LegacyNotificationBuilder(context, session, helper) }
    private val modernBuilder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) ModernNotificationBuilder(context, session, helper) else null
    }

    override fun updateState(progress: ProgressEntity, background: Boolean) {
        stateFlow.value = Pair(progress, background)
        if (!isObserving) {
            isObserving = true
            startInternalObserver()
        }
    }

    override fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun cancel() {
        notificationManager.cancel(notificationId)
    }

    @SuppressLint("MissingPermission")
    private fun startInternalObserver() {
        createNotificationChannels()
        sessionStartTime = System.currentTimeMillis()

        scope.launch {
            globalAuthorizer = appSettingsRepo.preferencesFlow.first().authorizer

            val settings = NotificationSettings(
                showDialog = appSettingsRepo.getBoolean(BooleanSetting.ShowDialogWhenPressingNotification, true).first(),
                showLiveActivity = appSettingsRepo.getBoolean(BooleanSetting.ShowLiveActivity, false).first(),
                showMiIsland = appSettingsRepo.getBoolean(BooleanSetting.ShowMiIsland, false).first(),
                showMiIslandBlockingInterval = appSettingsRepo.getInt(IntSetting.ShowMiIslandBlockingInterval, 100).first(),
                preferSystemIcon = appSettingsRepo.getBoolean(BooleanSetting.PreferSystemIconForInstall, false).first(),
                preferDynamicColor = appSettingsRepo.getBoolean(BooleanSetting.LiveActivityDynColorFollowPkgIcon, false).first()
            )

            val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
            val canAnimate = isModernEligible && settings.showLiveActivity && !settings.showMiIsland

            val ticker = flow {
                while (true) {
                    emit(Unit)
                    delay(200)
                }
            }

            combine(stateFlow.filterNotNull(), ticker) { state, _ -> state }
                .distinctUntilChanged { old, new ->
                    if (old.first != new.first || old.second != new.second) return@distinctUntilChanged false
                    if (canAnimate) return@distinctUntilChanged !(new.first is ProgressEntity.Installing && new.second)
                    return@distinctUntilChanged true
                }.collect { (progress, background) ->

                    // UI Animation logic extracted from data layer
                    var fakeItemProgress = 0f
                    if (progress is ProgressEntity.Installing) {
                        val key = "${progress.current}|${progress.total}|${progress.appLabel}"
                        if (currentInstallKey != key) {
                            currentInstallKey = key
                            currentInstallStartTime = System.currentTimeMillis()
                        }
                        fakeItemProgress = (1f - 1f / (1f + (System.currentTimeMillis() - currentInstallStartTime) / 3000f)).coerceAtMost(0.95f)
                    } else currentInstallKey = null

                    if (background) {
                        val isSameState = lastNotifiedEntity?.let { it::class == progress::class } == true
                        val currentRequiresAnimation = canAnimate && progress is ProgressEntity.Installing

                        val notification = when {
                            settings.showMiIsland -> miIslandBuilder.build(
                                progress,
                                true,
                                settings.showDialog,
                                settings.preferSystemIcon,
                                fakeItemProgress
                            )

                            isModernEligible && settings.showLiveActivity -> modernBuilder?.build(
                                progress,
                                settings.showDialog,
                                settings.preferSystemIcon,
                                settings.preferDynamicColor,
                                fakeItemProgress,
                                isSameState
                            )

                            else -> legacyBuilder.build(progress, true, settings.showDialog, settings.preferSystemIcon)
                        }

                        setNotificationThrottled(
                            notification,
                            progress,
                            settings.showMiIsland,
                            settings.showMiIslandBlockingInterval,
                            currentRequiresAnimation
                        )

                        val elapsedTime = System.currentTimeMillis() - sessionStartTime
                        if (elapsedTime < MINIMUM_VISIBILITY_DURATION_MS && progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess && progress !is ProgressEntity.InstallCompleted) {
                            delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                        }
                    } else {
                        setNotificationThrottled(null, progress, false, settings.showMiIslandBlockingInterval)
                    }
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
        blockInterval: Int,
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
        val isDataChanged = progress != lastNotifiedEntity

        if (progress is ProgressEntity.InstallingModule) {
            val currentLine = progress.output.lastOrNull()
            if (currentLine != lastLogLine && timeSinceLastUpdate > NOTIFICATION_UPDATE_INTERVAL_MS) {
                lastLogLine = currentLine
                setNotificationImmediate(notification, isMiIsland, blockInterval)
                lastNotificationUpdateTime = currentTime
            }
            return
        }

        val currentProgress = (progress as? ProgressEntity.InstallPreparing)?.progress ?: -1f

        val shouldUpdate = when {
            isCriticalState -> isDataChanged
            isEnteringInstalling -> true
            progress is ProgressEntity.Installing && isDataChanged -> true
            timeSinceLastUpdate < NOTIFICATION_UPDATE_INTERVAL_MS -> false
            progress is ProgressEntity.Installing -> requiresAnimation
            currentProgress < 0 -> true
            else -> currentProgress > lastProgressValue && ((currentProgress - lastProgressValue) >= PROGRESS_UPDATE_THRESHOLD || currentProgress >= 0.99f)
        }

        if (shouldUpdate) {
            setNotificationImmediate(notification, isMiIsland, blockInterval)
            lastNotificationUpdateTime = currentTime
            if (currentProgress >= 0) lastProgressValue = currentProgress
            lastProgressClass = progress::class
            lastNotifiedEntity = progress
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotificationImmediate(notification: Notification?, isMiIsland: Boolean = false, blockInterval: Int = 100) {
        if (notification == null) {
            notificationManager.cancel(notificationId)
        } else {
            if (isMiIsland) notifyWithXiaomiMagic(notificationId, notification, blockInterval)
            else notificationManager.notify(notificationId, notification)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notifyWithXiaomiMagic(notificationId: Int, notification: Notification, blockInterval: Int) {
        val currentShizukuMode = capabilityProvider.shizukuModeFlow.value
        val isShizukuShell = currentShizukuMode == ShizukuMode.SHELL
        val shouldExecuteMagic = globalAuthorizer == Authorizer.Shizuku && isShizukuShell
        val targetUid = xmsfUid

        if (!shouldExecuteMagic || targetUid == null) {
            notificationManager.notify(notificationId, notification)
            return
        }

        scope.launch(Dispatchers.IO) {
            networkMutex.withLock {
                try {
                    appOps.setPackageNetworkingEnabled(authorizer = globalAuthorizer, uid = targetUid, enabled = false)
                    isXiaomiNetworkBlocked = true
                    notificationManager.notify(notificationId, notification)
                    delay(blockInterval.toLong())
                } catch (e: Exception) {
                    Timber.e(e, "Xiaomi magic execution failed")
                } finally {
                    withContext(NonCancellable) {
                        try {
                            appOps.setPackageNetworkingEnabled(authorizer = globalAuthorizer, uid = targetUid, enabled = true)
                        } finally {
                            isXiaomiNetworkBlocked = false
                        }
                    }
                }
            }
        }
    }

    override suspend fun cleanup() {
        cancel()
        scope.cancel() // Cancel the internal observer
        val targetUid = xmsfUid
        if (::globalAuthorizer.isInitialized && globalAuthorizer == Authorizer.Shizuku && capabilityProvider.shizukuModeFlow.value == ShizukuMode.SHELL && targetUid != null) {
            withContext(Dispatchers.IO + NonCancellable) {
                networkMutex.withLock {
                    if (isXiaomiNetworkBlocked) {
                        try {
                            appOps.setPackageNetworkingEnabled(authorizer = globalAuthorizer, uid = targetUid, enabled = true)
                        } catch (e: Exception) {
                            Timber.e(e, "FATAL: Failed to restore network in cleanup")
                        } finally {
                            isXiaomiNetworkBlocked = false
                        }
                    }
                }
            }
        }
    }
}