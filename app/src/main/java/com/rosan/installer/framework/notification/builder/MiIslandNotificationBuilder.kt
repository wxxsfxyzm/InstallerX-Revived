// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification.builder

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import com.rosan.installer.R
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.framework.notification.NotificationHelper
import com.xzakota.hyper.notification.focus.FocusNotification

class MiIslandNotificationBuilder(
    private val context: Context,
    private val session: InstallerSessionRepository,
    private val helper: NotificationHelper,
) : InstallerNotificationBuilder {

    private val highlightBgColor = "#006EFF"
    private val highlightTitleColor = "#FFFFFF"
    private val contentFactory = IslandNotificationContentFactory(context, session, helper)

    override suspend fun build(payload: NotificationPayload): Notification? {
        val progress = payload.state.progress

        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error ||
            progress is ProgressEntity.InstallAnalysedUnsupported
        ) {
            return null
        }

        val builder = createBaseBuilder(progress, payload.state.background, payload.settings.showDialog)

        val content = contentFactory.create(
            progress = progress,
            cancelIntent = helper.finishIntent,
        )
        val title = content.title
        val shortText = content.shortText
        val contentText = content.contentText
        val progressValue = content.progressValue ?: -1
        val isOngoing = content.isOngoing
        val showAppIcon = content.showAppIcon
        val actionsList = content.actions

        builder.setContentTitle(title)
        if (contentText.isNotEmpty()) builder.setContentText(contentText)
        if (progressValue >= 0) {
            builder.setProgress(100, progressValue, progress is ProgressEntity.InstallPreparing && progress.progress < 0)
        }

        val appIconBitmap = helper.getLargeIconBitmap(
            payload.settings.preferSystemIcon,
            if (progress is ProgressEntity.Installing && progress.total > 1) progress.current - 1 else null,
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
                                this.content = contentText.ifEmpty { " " }
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
                    this.content = displayContent.ifEmpty { " " }
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
                    this.content = displayContent.ifEmpty { " " }
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
                                actionItem.pendingIntent,
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

    private fun createBaseBuilder(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean,
    ): NotificationCompat.Builder {
        val isWorking =
            progress is ProgressEntity.Ready || progress is ProgressEntity.InstallResolving ||
                progress is ProgressEntity.InstallResolveSuccess ||
                progress is ProgressEntity.InstallAnalysing ||
                progress is ProgressEntity.InstallAnalysedSuccess ||
                progress is ProgressEntity.Installing ||
                progress is ProgressEntity.InstallingModule ||
                progress is ProgressEntity.InstallSuccess ||
                progress is ProgressEntity.InstallCompleted
        // Keep Xiaomi Island updates on one channel. Switching between the normal
        // installer and progress channels causes MIUI's island renderer to rebuild
        // the live notification between stages, which shows up as a flash or a
        // short black gap.
        val channelEnum = NotificationHelper.Channel.InstallerLiveChannel
        val icon = (if (isWorking) NotificationHelper.Icon.Working else NotificationHelper.Icon.Pausing).resId
        val contentIntent =
            if (session.config.installMode == InstallMode.Notification ||
                session.config.installMode == InstallMode.AutoNotification
            ) {
                if (showDialog) helper.openIntent else null
            } else {
                helper.openIntent
            }

        val builder = NotificationCompat.Builder(context, channelEnum.value)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setDeleteIntent(helper.finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed ||
            progress is ProgressEntity.InstallCompleted
        ) {
            builder.setOngoing(false).setOnlyAlertOnce(false)
        }

        return builder
    }
}
