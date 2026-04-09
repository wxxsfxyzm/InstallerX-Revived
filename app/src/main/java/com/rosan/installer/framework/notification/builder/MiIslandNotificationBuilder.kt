// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.getInfo
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.framework.notification.NotificationHelper
import com.rosan.installer.util.getErrorMessage
import com.xzakota.hyper.notification.focus.FocusNotification

class MiIslandNotificationBuilder(
    private val context: Context,
    private val session: InstallerSessionRepository,
    private val helper: NotificationHelper
) : InstallerNotificationBuilder {

    private data class IslandAction(
        val key: String,
        val title: String,
        val pendingIntent: PendingIntent,
        val isHighlighted: Boolean = false
    )

    private val highlightBgColor = "#006EFF"
    private val highlightTitleColor = "#FFFFFF"

    override suspend fun build(payload: NotificationPayload): Notification? {
        val progress = payload.state.progress

        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported) {
            return null
        }

        val builder = createBaseBuilder(progress, payload.state.background, payload.settings.showDialog)

        var title = context.getString(R.string.installer_ready)
        var contentText = ""
        var shortText = context.getString(R.string.installer_ready)
        var progressValue = -1
        var isOngoing = false
        var showAppIcon = true // Flag to determine whether to display the app icon or the default logo
        val actionsList = mutableListOf<IslandAction>()

        when (progress) {
            is ProgressEntity.InstallResolving -> {
                title = context.getString(R.string.installer_resolving)
                shortText = context.getString(R.string.installer_live_channel_short_text_resolving)
                contentText = context.getString(R.string.installer_resolving_desc)
                isOngoing = true
                showAppIcon = false
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallResolveSuccess -> {
                title = context.getString(R.string.installer_resolve_success)
                shortText = context.getString(R.string.installer_live_channel_short_text_resolving)
                showAppIcon = false
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallPreparing -> {
                title = context.getString(R.string.installer_preparing)
                shortText = context.getString(R.string.installer_live_channel_short_text_preparing)
                contentText = context.getString(R.string.installer_preparing_desc)
                progressValue = (progress.progress * 100).toInt()
                isOngoing = true
                showAppIcon = false
            }

            is ProgressEntity.InstallResolvedFailed -> {
                title = context.getString(R.string.installer_resolve_failed)
                shortText = context.getString(R.string.installer_live_channel_short_text_resolve_failed)
                contentText = session.error.getErrorMessage(context)
                showAppIcon = false
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallAnalysing -> {
                title = context.getString(R.string.installer_analysing)
                shortText = context.getString(R.string.installer_live_channel_short_text_analysing)
                isOngoing = true
                showAppIcon = false
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                val allEntities = session.analysisResults.flatMap { it.appEntities }
                val selectedApps = allEntities.map { it.app }
                val hasComplexType =
                    allEntities.any { it.app.sourceType == DataType.MIXED_MODULE_APK || it.app.sourceType == DataType.MIXED_MODULE_ZIP }
                val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

                shortText = if (hasComplexType || isMultiPackage) {
                    context.getString(R.string.installer_live_channel_short_text_pending)
                } else {
                    context.getString(R.string.installer_live_channel_short_text_pending_install)
                }

                if (hasComplexType) {
                    title = context.getString(R.string.installer_prepare_install)
                    contentText = context.getString(R.string.installer_mixed_module_apk_description_notification)
                    showAppIcon = false
                    actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                } else if (isMultiPackage) {
                    title = context.getString(R.string.installer_prepare_install)
                    contentText = context.getString(R.string.installer_multi_apk_description_notification)
                    showAppIcon = false
                    actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                } else {
                    title = context.getString(R.string.installer_prepare_type_unknown_confirm)
                    contentText = selectedApps.getInfo(context).title
                    actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                    actionsList.add(IslandAction("miui_action_install", context.getString(R.string.install), helper.installIntent, true))
                }
            }

            is ProgressEntity.InstallAnalysedFailed -> {
                title = context.getString(R.string.installer_analyse_failed)
                shortText = context.getString(R.string.installer_live_channel_short_text_analyse_failed)
                contentText = session.error.getErrorMessage(context)
                showAppIcon = false
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                actionsList.add(IslandAction("miui_action_retry", context.getString(R.string.retry), helper.analyseIntent))
            }

            is ProgressEntity.Installing -> {
                val appLabel = progress.appLabel ?: context.getString(R.string.installer_installing)
                title = context.getString(R.string.installer_installing)
                shortText = context.getString(R.string.installer_live_channel_short_text_installing)
                contentText = if (progress.total > 1) "(${(progress.current)}/${progress.total}) $appLabel" else appLabel
                isOngoing = true
                val total = progress.total.coerceAtLeast(1).toFloat()
                val currentBase = (progress.current - 1).coerceAtLeast(0).toFloat()

                // Calculate actual static progress without fake animation ticks
                val batchFraction = currentBase / total
                progressValue = (100 * batchFraction).toInt()
            }

            is ProgressEntity.InstallingModule -> {
                title = context.getString(R.string.installer_installing)
                shortText = context.getString(R.string.installer_live_channel_short_text_installing)
                isOngoing = true
                contentText = progress.output.lastOrNull() ?: context.getString(R.string.installer_installing)
            }

            is ProgressEntity.InstallSuccess -> {
                title = context.getString(R.string.installer_install_success)
                shortText = context.getString(R.string.installer_live_channel_short_text_success)
                contentText = session.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context).title

                actionsList.add(IslandAction("miui_action_finish", context.getString(R.string.finish), helper.finishIntent))
                val openIntent =
                    helper.getLaunchPendingIntent(session.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }
                        .firstOrNull()?.packageName)
                if (openIntent != null) {
                    actionsList.add(IslandAction("miui_action_open", context.getString(R.string.open), openIntent, true))
                }
            }

            is ProgressEntity.InstallCompleted -> {
                val successCount = progress.results.count { it.success }
                val totalCount = progress.results.size
                title =
                    if (successCount == totalCount) context.getString(R.string.installer_install_success) else "${context.getString(R.string.installer_install_success)}: $successCount/$totalCount"
                shortText =
                    if (successCount == totalCount) context.getString(R.string.installer_live_channel_short_text_success) else "$successCount/$totalCount ${
                        context.getString(R.string.installer_live_channel_short_text_success)
                    }"
                contentText = context.getString(R.string.installer_live_channel_short_text_success)
                actionsList.add(IslandAction("miui_action_finish", context.getString(R.string.finish), helper.finishIntent))
            }

            is ProgressEntity.InstallFailed -> {
                title = context.getString(R.string.installer_install_failed)
                shortText = context.getString(R.string.installer_live_channel_short_text_install_failed)
                contentText = session.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context).title
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                actionsList.add(IslandAction("miui_action_retry", context.getString(R.string.retry), helper.installIntent))
            }

            else -> {}
        }

        builder.setContentTitle(title)
        if (contentText.isNotEmpty()) builder.setContentText(contentText)
        if (progressValue >= 0) {
            builder.setProgress(100, progressValue, progress is ProgressEntity.InstallPreparing && progress.progress < 0)
        }

        val appIconBitmap = helper.getLargeIconBitmap(
            payload.settings.preferSystemIcon,
            if (progress is ProgressEntity.Installing && progress.total > 1) progress.current - 1 else null
        )

        val isAutoMode = session.config.installMode == InstallMode.AutoNotification

        val lightLogoIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.BLACK)
        val darkLogoIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.WHITE)

        val islandExtras = FocusNotification.buildV3 {
            val lightLogoKey = createPicture("key_logo_light", lightLogoIcon)
            val darkLogoKey = createPicture("key_logo_dark", darkLogoIcon)
            val appIconKey = appIconBitmap?.let { createPicture("key_app_icon", Icon.createWithBitmap(it)) } ?: lightLogoKey

            // Use the dark logo for the black capsule if showAppIcon is false
            val displayIconKey = if (showAppIcon) appIconKey else darkLogoKey

            if (isAutoMode) {
                islandFirstFloat = false
                enableFloat = false
            } else {
                islandFirstFloat = true
                enableFloat = !isOngoing
            }
            updatable = true
            ticker = title
            tickerPic = lightLogoKey
            if (payload.settings.miIslandOuterGlow) { // Control the outer glow
                outEffectSrc = "outer_glow"
            }

            // 1. Xiaomi Island configuration (includes capsule summary state and large island expanded state)
            island {
                islandProperty = 1

                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = displayIconKey
                        }
                    }

                    if (progress is ProgressEntity.InstallPreparing) {
                        progressTextInfo {
                            progressInfo {
                                isCCW = true
                                this.progress = progressValue.coerceAtLeast(0)
                            }
                            textInfo {
                                this.title = shortText.ifEmpty { title }
                                content = contentText.ifEmpty { " " }
                            }
                        }
                    } else {
                        imageTextInfoRight {
                            type = 3
                            textInfo {
                                this.title = shortText.ifEmpty { title }
                            }
                        }
                    }
                }

                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = displayIconKey
                    }
                }
            }

            // 2. Focus notification dropdown expanded state configuration
            var displayTitle = title
            var displayContent = contentText

            if (progress is ProgressEntity.InstallAnalysedSuccess) {
                displayTitle = contentText
                displayContent = title
            }

            if (!showAppIcon) {
                // Apply official template [No. 19]: Text component 2 (baseInfo type=2) + Progress component 3 (multiProgressInfo)
                baseInfo {
                    type = 2
                    this.title = displayTitle
                    content = displayContent.ifEmpty { " " }
                }

                // Use multiProgressInfo during the Preparing stage to avoid progressInfo parsing bugs
                if (progress is ProgressEntity.InstallPreparing) {
                    multiProgressInfo {
                        this.progress = progressValue.coerceAtLeast(0)
                    }
                }
            } else {
                // Standard template with icon for other stages
                iconTextInfo {
                    this.title = displayTitle
                    content = displayContent.ifEmpty { " " }
                    animIconInfo {
                        type = 0
                        src = displayIconKey
                    }
                }
            }

            picInfo {
                type = 1
                pic = lightLogoKey
                picDark = darkLogoKey
            }

            if (actionsList.isNotEmpty()) {
                textButton {
                    actionsList.take(2).forEach { actionItem ->
                        addActionInfo {
                            val nativeAction = Notification.Action.Builder(
                                Icon.createWithResource(context, NotificationHelper.Icon.Pausing.resId),
                                actionItem.title,
                                actionItem.pendingIntent
                            ).build()

                            action = createAction(actionItem.key, nativeAction)
                            actionTitle = actionItem.title

                            if (actionItem.isHighlighted) {
                                actionBgColor = highlightBgColor
                                actionBgColorDark = highlightBgColor
                                actionTitleColor = highlightTitleColor
                                actionTitleColorDark = highlightTitleColor
                            }
                        }
                    }
                }
            }
        }

        builder.addExtras(islandExtras)
        return builder.build()
    }

    private fun createBaseBuilder(progress: ProgressEntity, background: Boolean, showDialog: Boolean): NotificationCompat.Builder {
        val isWorking =
            progress is ProgressEntity.Ready || progress is ProgressEntity.InstallResolving || progress is ProgressEntity.InstallResolveSuccess || progress is ProgressEntity.InstallAnalysing || progress is ProgressEntity.InstallAnalysedSuccess || progress is ProgressEntity.Installing || progress is ProgressEntity.InstallingModule || progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallCompleted
        val isImportance =
            progress is ProgressEntity.InstallResolvedFailed || progress is ProgressEntity.InstallAnalysedFailed || progress is ProgressEntity.InstallAnalysedSuccess || progress is ProgressEntity.InstallFailed || progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallCompleted

        val channelEnum =
            if (isImportance && background) NotificationHelper.Channel.InstallerChannel else NotificationHelper.Channel.InstallerProgressChannel
        val icon = (if (isWorking) NotificationHelper.Icon.Working else NotificationHelper.Icon.Pausing).resId
        val contentIntent =
            if (session.config.installMode == InstallMode.Notification || session.config.installMode == InstallMode.AutoNotification) {
                if (showDialog) helper.openIntent else null
            } else helper.openIntent

        val builder = NotificationCompat.Builder(context, channelEnum.value)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setDeleteIntent(helper.finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed || progress is ProgressEntity.InstallCompleted) {
            builder.setOngoing(false).setOnlyAlertOnce(false)
        }

        return builder
    }
}
