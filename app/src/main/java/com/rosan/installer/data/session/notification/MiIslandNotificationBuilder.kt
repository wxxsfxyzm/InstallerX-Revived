// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.notification

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
import com.rosan.installer.util.getErrorMessage
import com.xzakota.hyper.notification.focus.FocusNotification

class MiIslandNotificationBuilder(
    private val context: Context,
    private val installer: InstallerSessionRepository,
    private val helper: NotificationHelper
) {

    private data class IslandAction(
        val key: String,
        val title: String,
        val pendingIntent: PendingIntent,
        val isHighlighted: Boolean = false
    )

    private val highlightBgColor = "#006EFF"
    private val highlightTitleColor = "#FFFFFF"

    suspend fun build(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean,
        preferSystemIcon: Boolean,
        fakeItemProgress: Float = 0f
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported) {
            return null
        }

        val builder = createBaseBuilder(progress, background, showDialog)

        var title = context.getString(R.string.installer_ready)
        var contentText = ""
        var progressValue = -1
        var isError = false
        var isSuccess = false
        var isOngoing = false
        val actionsList = mutableListOf<IslandAction>()

        when (progress) {
            is ProgressEntity.InstallResolving -> {
                title = context.getString(R.string.installer_resolving)
                isOngoing = true
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallResolveSuccess -> {
                title = context.getString(R.string.installer_resolve_success)
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallPreparing -> {
                title = context.getString(R.string.installer_preparing)
                contentText = context.getString(R.string.installer_preparing_desc)
                progressValue = (progress.progress * 100).toInt()
                isOngoing = true
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.cancelIntent))
            }

            is ProgressEntity.InstallResolvedFailed -> {
                title = context.getString(R.string.installer_resolve_failed)
                contentText = installer.error.getErrorMessage(context)
                isError = true
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallAnalysing -> {
                title = context.getString(R.string.installer_analysing)
                isOngoing = true
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                val allEntities = installer.analysisResults.flatMap { it.appEntities }
                val selectedApps = allEntities.map { it.app }
                val hasComplexType =
                    allEntities.any { it.app.sourceType == DataType.MIXED_MODULE_APK || it.app.sourceType == DataType.MIXED_MODULE_ZIP }
                val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

                if (hasComplexType) {
                    title = context.getString(R.string.installer_prepare_install)
                    contentText = context.getString(R.string.installer_mixed_module_apk_description_notification)
                    actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                } else if (isMultiPackage) {
                    title = context.getString(R.string.installer_prepare_install)
                    contentText = context.getString(R.string.installer_multi_apk_description_notification)
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
                contentText = installer.error.getErrorMessage(context)
                isError = true
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.finishIntent))
                actionsList.add(IslandAction("miui_action_retry", context.getString(R.string.retry), helper.analyseIntent))
            }

            is ProgressEntity.Installing -> {
                val appLabel = progress.appLabel ?: context.getString(R.string.installer_installing)
                title = context.getString(R.string.installer_installing)
                contentText = if (progress.total > 1) "(${(progress.current)}/${progress.total}) $appLabel" else appLabel
                isOngoing = true
                val total = progress.total.coerceAtLeast(1).toFloat()
                val currentBase = (progress.current - 1).coerceAtLeast(0).toFloat()
                val batchFraction = (currentBase + fakeItemProgress).coerceIn(0f, total) / total
                progressValue = (100 * batchFraction).toInt()

                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.cancelIntent))
            }

            is ProgressEntity.InstallingModule -> {
                title = context.getString(R.string.installer_installing)
                isOngoing = true
                contentText = progress.output.lastOrNull() ?: context.getString(R.string.installer_installing)
                actionsList.add(IslandAction("miui_action_cancel", context.getString(R.string.cancel), helper.cancelIntent))
            }

            is ProgressEntity.InstallSuccess -> {
                title = context.getString(R.string.installer_install_success)
                contentText = installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context).title
                isSuccess = true

                actionsList.add(IslandAction("miui_action_finish", context.getString(R.string.finish), helper.finishIntent))
                val openIntent =
                    helper.getLaunchPendingIntent(installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }
                        .firstOrNull()?.packageName)
                if (openIntent != null) {
                    actionsList.add(IslandAction("miui_action_open", context.getString(R.string.open), openIntent, true))
                }
            }

            is ProgressEntity.InstallCompleted -> {
                val successCount = progress.results.count { it.success }
                title =
                    if (successCount == progress.results.size) context.getString(R.string.installer_install_success) else "${context.getString(R.string.installer_install_success)}: $successCount/${progress.results.size}"
                contentText = context.getString(R.string.installer_live_channel_short_text_success)
                isSuccess = true
                actionsList.add(IslandAction("miui_action_finish", context.getString(R.string.finish), helper.finishIntent))
            }

            is ProgressEntity.InstallFailed -> {
                title = context.getString(R.string.installer_install_failed)
                contentText = installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context).title
                isError = true
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
            preferSystemIcon,
            if (progress is ProgressEntity.Installing && progress.total > 1) progress.current - 1 else null
        )

        val isAutoMode = installer.config.installMode == InstallMode.AutoNotification

        val lightLogoIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.BLACK)
        val darkLogoIcon = Icon.createWithResource(context, R.drawable.ic_notification_logo).setTint(Color.WHITE)

        val islandExtras = FocusNotification.buildV3 {
            val lightLogoKey = createPicture("key_logo_light", lightLogoIcon)
            val darkLogoKey = createPicture("key_logo_dark", darkLogoIcon)

            val appIconKey = appIconBitmap?.let { createPicture("key_app_icon", Icon.createWithBitmap(it)) } ?: lightLogoKey

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

            // 1. 小米岛 摘要态 (组合4：左侧 App 图标 + App 名称，右侧纯文本状态)
            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = appIconKey
                        }
                    }
                    imageTextInfoRight {
                        type = 3 // 官方文档中用于展示: 正文大字 + 图标 的合法组件
                        textInfo {
                            this.title = title
                        }
                    }
                }
                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = appIconKey
                    }
                }
            }

            // 2. 焦点通知 展开态 (模板17)
            iconTextInfo {
                this.title = title
                content = contentText.ifEmpty { " " }
                animIconInfo {
                    type = 0
                    src = appIconKey
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
            if (installer.config.installMode == InstallMode.Notification || installer.config.installMode == InstallMode.AutoNotification) {
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