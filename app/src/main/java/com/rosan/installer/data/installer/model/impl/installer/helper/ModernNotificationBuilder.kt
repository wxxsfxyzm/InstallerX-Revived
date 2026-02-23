// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.impl.installer.helper

import android.app.Notification
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.rosan.installer.R
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.dynamicColorScheme
import com.rosan.installer.ui.theme.primaryDark
import com.rosan.installer.ui.theme.primaryLight
import com.rosan.installer.util.getErrorMessage
import com.rosan.installer.util.hasFlag
import kotlin.reflect.KClass

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ModernNotificationBuilder(
    private val context: Context,
    private val installer: InstallerRepo,
    private val helper: NotificationHelper
) {
    companion object {
        private const val M3_ERROR_COLOR_LIGHT = 0xFFB3261E
        private const val M3_ERROR_COLOR_DARK = 0xFFF2B8B5
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

    private val baseNotificationBuilder by lazy {
        NotificationCompat.Builder(context, NotificationHelper.Channel.InstallerLiveChannel.value)
            .setSmallIcon(NotificationHelper.Icon.LOGO.resId)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
    }

    suspend fun build(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean,
        preferDynamicColor: Boolean,
        fakeItemProgress: Float,
        isSameState: Boolean
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported)
            return null

        val builder = createBaseBuilder(progress, showDialog, preferDynamicColor, fakeItemProgress)

        return when (progress) {
            is ProgressEntity.InstallPreparing -> builder.addAction(0, context.getString(R.string.cancel), helper.cancelIntent).build()
            is ProgressEntity.InstallResolvedFailed -> onResolvedFailed(builder).build()
            is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess(builder, preferSystemIcon, isSameState).build()
            is ProgressEntity.InstallAnalysedFailed -> onAnalysedFailed(builder).build()
            is ProgressEntity.Installing -> onInstalling(builder, progress, preferSystemIcon).build()
            is ProgressEntity.InstallingModule -> onInstallingModule(builder, progress, preferSystemIcon).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder, preferSystemIcon).build()
            is ProgressEntity.InstallCompleted -> onInstallCompleted(builder).build()
            is ProgressEntity.InstallFailed -> onInstallFailed(builder, preferSystemIcon).build()
            else -> builder.build()
        }
    }

    private fun createBaseBuilder(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferDynamicColor: Boolean,
        fakeItemProgress: Float
    ): NotificationCompat.Builder {
        val baseBuilder = baseNotificationBuilder
        baseBuilder
            .clearActions()
            // Prevent state leakage from previous progress stages
            .setContentText(null)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)

        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification,
            ConfigEntity.InstallMode.AutoNotification -> if (showDialog) helper.openIntent else null

            else -> helper.openIntent
        }
        val isDarkTheme = context.resources.configuration.uiMode.hasFlag(Configuration.UI_MODE_NIGHT_YES)
        val brandColor = if (isDarkTheme) primaryDark else primaryLight

        baseBuilder
            .setColor(brandColor.toArgb())
            .setContentIntent(contentIntent)
            .setDeleteIntent(helper.finishIntent)

        val seedColorInt = getCurrentSeedColor(progress)
        val dynamicColorScheme = if (preferDynamicColor) {
            seedColorInt?.let { color ->
                dynamicColorScheme(keyColor = Color(color), isDark = isDarkTheme, style = PaletteStyle.TonalSpot)
            }
        } else null

        val failedStageIndex = when (progress) {
            is ProgressEntity.InstallResolvedFailed -> 0
            is ProgressEntity.InstallAnalysedFailed -> 2
            is ProgressEntity.InstallFailed -> 3
            else -> null
        }

        val currentStageIndex = installStages.indexOfFirst { it.progressClass.isInstance(progress) }
        val segments = createInstallSegments(installStages, dynamicColorScheme, isDarkTheme, failedStageIndex)
        val progressStyle = NotificationCompat.ProgressStyle().setProgressSegments(segments).setStyledByProgress(true)

        var contentTitle: String
        var shortText: String? = null
        val previousStagesWeight =
            if (currentStageIndex > 0) installStages.subList(0, currentStageIndex).sumOf { it.weight.toDouble() }.toFloat() else 0f

        when (progress) {
            is ProgressEntity.InstallResolving, is ProgressEntity.InstallResolveSuccess -> {
                contentTitle = context.getString(R.string.installer_resolving)
                shortText = context.getString(R.string.installer_live_channel_short_text_resolving)
                progressStyle.setProgress((previousStagesWeight + (installStages[0].weight / 2f)).toInt())
            }

            is ProgressEntity.InstallPreparing -> {
                contentTitle = context.getString(R.string.installer_preparing)
                shortText = context.getString(R.string.installer_live_channel_short_text_preparing)
                progressStyle.setProgress((previousStagesWeight + installStages[1].weight * progress.progress).toInt())
            }

            is ProgressEntity.InstallResolvedFailed -> {
                contentTitle = context.getString(R.string.installer_resolve_failed)
                shortText = context.getString(R.string.installer_live_channel_short_text_resolve_failed)
                progressStyle.setProgress(installStages[0].weight.toInt())
            }

            is ProgressEntity.InstallAnalysing -> {
                contentTitle = context.getString(R.string.installer_analysing)
                shortText = context.getString(R.string.installer_live_channel_short_text_analysing)
                progressStyle.setProgress((previousStagesWeight + (installStages[2].weight / 2f)).toInt())
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                val selectedApps = installer.analysisResults.flatMap { it.appEntities }.map { it.app }
                val hasComplexType = installer.analysisResults.flatMap { it.appEntities }
                    .any { it.app.sourceType == DataType.MIXED_MODULE_APK || it.app.sourceType == DataType.MIXED_MODULE_ZIP }
                val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

                shortText = if (hasComplexType || isMultiPackage) context.getString(R.string.installer_live_channel_short_text_pending)
                else context.getString(R.string.installer_live_channel_short_text_pending_install)

                contentTitle = if (hasComplexType || isMultiPackage) context.getString(R.string.installer_prepare_install)
                else selectedApps.getInfo(context).title

                progressStyle.setProgress(installStages.subList(0, 3).sumOf { it.weight.toDouble() }.toInt())
            }

            is ProgressEntity.InstallAnalysedFailed -> {
                contentTitle = context.getString(R.string.installer_analyse_failed)
                shortText = context.getString(R.string.installer_live_channel_short_text_analyse_failed)
                progressStyle.setProgress(installStages.subList(0, 3).sumOf { it.weight.toDouble() }.toInt())
            }

            is ProgressEntity.Installing -> {
                val isBatch = progress.total > 1
                val appLabel = progress.appLabel ?: context.getString(R.string.installer_installing)
                contentTitle =
                    if (isBatch) "${context.getString(R.string.installer_installing)} (${progress.current}/${progress.total}): $appLabel" else appLabel
                shortText = context.getString(R.string.installer_live_channel_short_text_installing)

                val total = progress.total.coerceAtLeast(1).toFloat()
                val currentBase = (progress.current - 1).coerceAtLeast(0).toFloat()
                val batchFraction = (currentBase + fakeItemProgress) / total
                progressStyle.setProgress((previousStagesWeight + (installStages[3].weight * batchFraction)).toInt())
            }

            is ProgressEntity.InstallingModule -> {
                contentTitle = context.getString(R.string.installer_installing)
                shortText = context.getString(R.string.installer_live_channel_short_text_installing)
                progressStyle.setProgress((previousStagesWeight + (installStages[3].weight * 0.1f)).toInt())
            }

            is ProgressEntity.InstallSuccess -> {
                contentTitle = installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context).title
                shortText = context.getString(R.string.installer_live_channel_short_text_success)
                progressStyle.setProgress(totalProgressWeight.toInt())
            }

            is ProgressEntity.InstallCompleted -> {
                val successCount = progress.results.count { it.success }
                val totalCount = progress.results.size
                contentTitle =
                    if (successCount == totalCount) context.getString(R.string.installer_install_success) else "${context.getString(R.string.installer_install_success)}: $successCount/$totalCount"
                shortText =
                    if (successCount == totalCount) context.getString(R.string.installer_live_channel_short_text_success) else "$successCount/$totalCount ${
                        context.getString(R.string.installer_live_channel_short_text_success)
                    }"
                progressStyle.setProgress(totalProgressWeight.toInt())
            }

            is ProgressEntity.InstallFailed -> {
                contentTitle = installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context).title
                shortText = context.getString(R.string.installer_live_channel_short_text_install_failed)
                progressStyle.setProgress(totalProgressWeight.toInt())
            }

            else -> {
                contentTitle = context.getString(R.string.installer_ready)
                progressStyle.setProgress(0)
            }
        }

        baseBuilder.setContentTitle(contentTitle)
        shortText?.let { baseBuilder.setShortCriticalText(it) }
        baseBuilder.setStyle(progressStyle)
        return baseBuilder
    }

    private fun onResolvedFailed(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        builder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false).setSilent(false)
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent)

    private suspend fun onAnalysedSuccess(
        builder: NotificationCompat.Builder,
        preferSystemIcon: Boolean,
        isSameState: Boolean
    ): NotificationCompat.Builder {
        builder.setOnlyAlertOnce(isSameState).setSilent(false)
        val allEntities = installer.analysisResults.flatMap { it.appEntities }
        val hasComplexType = allEntities.any { it.app.sourceType == DataType.MIXED_MODULE_APK || it.app.sourceType == DataType.MIXED_MODULE_ZIP }
        val isMultiPackage = allEntities.map { it.app }.groupBy { it.packageName }.size > 1

        if (hasComplexType) {
            builder.setContentText(context.getString(R.string.installer_mixed_module_apk_description_notification))
                .addAction(0, context.getString(R.string.cancel), helper.finishIntent)
        } else if (isMultiPackage) {
            builder.setContentText(context.getString(R.string.installer_multi_apk_description_notification))
                .addAction(0, context.getString(R.string.cancel), helper.finishIntent)
        } else {
            builder.setContentText(context.getString(R.string.installer_prepare_type_unknown_confirm))
                .addAction(0, context.getString(R.string.install), helper.installIntent)
                .addAction(0, context.getString(R.string.cancel), helper.finishIntent)
                .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon))
        }
        return builder
    }

    private fun onAnalysedFailed(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        builder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false).setSilent(false)
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent)

    private suspend fun onInstalling(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.Installing,
        preferSystemIcon: Boolean
    ): NotificationCompat.Builder {
        val currentBatchIndex = if (progress.total > 1) progress.current - 1 else null
        return builder.setContentText(context.getString(R.string.installer_installing))
            .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon, currentBatchIndex))
    }

    private suspend fun onInstallingModule(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.InstallingModule,
        preferSystemIcon: Boolean
    ): NotificationCompat.Builder =
        builder.setContentText(progress.output.lastOrNull() ?: context.getString(R.string.installer_installing))
            .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon))

    private suspend fun onInstallSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): NotificationCompat.Builder {
        builder.setContentText(context.getString(R.string.installer_install_success))
            .setOnlyAlertOnce(false).setSilent(false).setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon))
        val openIntent = helper.getLaunchPendingIntent(installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }
            .firstOrNull()?.packageName)
        if (openIntent != null) builder.addAction(0, context.getString(R.string.open), openIntent)
        return builder.addAction(0, context.getString(R.string.finish), helper.finishIntent)
    }

    private fun onInstallCompleted(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        builder.setContentText(context.getString(R.string.installer_install_complete))
            .setOnlyAlertOnce(false).setSilent(false).addAction(0, context.getString(R.string.finish), helper.finishIntent)

    private suspend fun onInstallFailed(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): NotificationCompat.Builder =
        builder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false).setSilent(false)
            .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon))
            .addAction(0, context.getString(R.string.retry), helper.installIntent)
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent)

    private fun createInstallSegments(
        stages: List<InstallStageInfo>,
        colorScheme: ColorScheme?,
        isDarkTheme: Boolean,
        failedStageIndex: Int?
    ): List<NotificationCompat.ProgressStyle.Segment> {
        return stages.mapIndexed { index, stageInfo ->
            val segment = NotificationCompat.ProgressStyle.Segment(stageInfo.weight.toInt())
            if (index == failedStageIndex) {
                segment.setColor(colorScheme?.error?.toArgb() ?: if (isDarkTheme) M3_ERROR_COLOR_DARK.toInt() else M3_ERROR_COLOR_LIGHT.toInt())
            } else {
                colorScheme?.let {
                    segment.setColor(
                        when (stageInfo.progressClass) {
                            ProgressEntity.InstallPreparing::class, ProgressEntity.Installing::class -> it.primary.toArgb()
                            ProgressEntity.InstallResolving::class, ProgressEntity.InstallAnalysing::class -> it.tertiary.toArgb()
                            else -> it.primary.toArgb()
                        }
                    )
                }
            }
            segment
        }
    }

    private fun getCurrentSeedColor(progress: ProgressEntity): Int? {
        if (progress is ProgressEntity.Installing && progress.total > 1) {
            val currentEntity = installer.multiInstallQueue.getOrNull(progress.current - 1)
            if (currentEntity != null) {
                return installer.analysisResults.find { it.packageName == currentEntity.app.packageName }?.seedColor
            }
        }
        return installer.analysisResults.firstNotNullOfOrNull { it.seedColor }
    }
}
