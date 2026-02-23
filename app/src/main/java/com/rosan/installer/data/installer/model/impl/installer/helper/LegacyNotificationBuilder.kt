// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.impl.installer.helper

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.rosan.installer.R
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.util.getErrorMessage

class LegacyNotificationBuilder(
    private val context: Context,
    private val installer: InstallerRepo,
    private val helper: NotificationHelper
) {

    suspend fun build(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean,
        preferSystemIcon: Boolean
    ): Notification? {
        val builder = createBaseBuilder(progress, background, showDialog)
        return when (progress) {
            is ProgressEntity.Ready -> onReady(builder)
            is ProgressEntity.InstallResolving -> onResolving(builder)
            is ProgressEntity.InstallResolvedFailed -> onResolvedFailed(builder)
            is ProgressEntity.InstallResolveSuccess -> onResolveSuccess(builder)
            is ProgressEntity.InstallPreparing -> onPreparing(builder, progress)
            is ProgressEntity.InstallAnalysing -> onAnalysing(builder)
            is ProgressEntity.InstallAnalysedFailed -> onAnalysedFailed(builder)
            is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess(builder, preferSystemIcon)
            is ProgressEntity.Installing -> onInstalling(builder, progress, preferSystemIcon)
            is ProgressEntity.InstallingModule -> onInstallingModule(builder, progress, preferSystemIcon)
            is ProgressEntity.InstallFailed -> onInstallFailed(builder, preferSystemIcon).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder, preferSystemIcon).build()
            is ProgressEntity.InstallCompleted -> onInstallCompleted(builder, progress).build()
            is ProgressEntity.Finish, is ProgressEntity.Error, is ProgressEntity.InstallAnalysedUnsupported -> null
            else -> null
        }
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
            if (installer.config.installMode == ConfigEntity.InstallMode.Notification || installer.config.installMode == ConfigEntity.InstallMode.AutoNotification) {
                if (showDialog) helper.openIntent else null
            } else helper.openIntent

        val builder = NotificationCompat.Builder(context, channelEnum.value)
            .setSmallIcon(icon).setContentIntent(contentIntent).setDeleteIntent(helper.finishIntent).setOnlyAlertOnce(true).setOngoing(true)

        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed || progress is ProgressEntity.InstallCompleted) {
            builder.setOngoing(false).setOnlyAlertOnce(false)
        }

        val legacyProgressValue = when (progress) {
            is ProgressEntity.InstallResolving -> 0
            is ProgressEntity.InstallAnalysing -> 40
            is ProgressEntity.Installing -> 50 + (40 * (if (progress.total > 0) progress.current.toFloat() / progress.total.toFloat() else 0.5f)).toInt()
            is ProgressEntity.InstallingModule -> 70
            else -> null
        }
        legacyProgressValue?.let { builder.setProgress(100, it, false) }
        return builder
    }

    private fun onReady(builder: NotificationCompat.Builder) = builder.setContentTitle(context.getString(R.string.installer_ready))
        .addAction(0, context.getString(R.string.cancel), helper.finishIntent).build()

    private fun onResolving(builder: NotificationCompat.Builder) =
        builder.setContentTitle(context.getString(R.string.installer_resolving)).build()

    private fun onPreparing(builder: NotificationCompat.Builder, progress: ProgressEntity.InstallPreparing) =
        builder.setContentTitle(context.getString(R.string.installer_preparing))
            .setContentText(context.getString(R.string.installer_preparing_desc))
            .setProgress(100, (progress.progress * 100).toInt(), progress.progress < 0)
            .addAction(0, context.getString(R.string.cancel), helper.cancelIntent).build()

    private fun onResolvedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(context.getString(R.string.installer_resolve_failed))
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent).build()

    private fun onResolveSuccess(builder: NotificationCompat.Builder) =
        builder.setContentTitle(context.getString(R.string.installer_resolve_success))
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent).build()

    private fun onAnalysing(builder: NotificationCompat.Builder) =
        builder.setContentTitle(context.getString(R.string.installer_analysing)).build()

    private fun onAnalysedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(context.getString(R.string.installer_analyse_failed))
            .addAction(0, context.getString(R.string.retry), helper.analyseIntent)
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent).build()

    private suspend fun onAnalysedSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): Notification {
        val allEntities = installer.analysisResults.flatMap { it.appEntities }
        val selectedApps = allEntities.map { it.app }
        val hasComplexType = allEntities.any { it.app.sourceType == DataType.MIXED_MODULE_APK || it.app.sourceType == DataType.MIXED_MODULE_ZIP }
        val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

        if (hasComplexType) return builder.setContentTitle(context.getString(R.string.installer_prepare_install))
            .setContentText(context.getString(R.string.installer_mixed_module_apk_description_notification))
            .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon)).addAction(0, context.getString(R.string.cancel), helper.finishIntent)
            .build()
        return if (isMultiPackage) builder.setContentTitle(context.getString(R.string.installer_prepare_install))
            .setContentText(context.getString(R.string.installer_multi_apk_description_notification))
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent).build()
        else builder.setContentTitle(selectedApps.getInfo(context).title)
            .setContentText(context.getString(R.string.installer_prepare_type_unknown_confirm))
            .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon)).addAction(0, context.getString(R.string.install), helper.installIntent)
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent).build()
    }

    private suspend fun onInstalling(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.Installing,
        preferSystemIcon: Boolean
    ): Notification {
        val appLabel = progress.appLabel ?: context.getString(R.string.installer_installing)
        val title =
            if (progress.total > 1) "${context.getString(R.string.installer_installing)} (${progress.current}/${progress.total})" else appLabel
        val content = if (progress.total > 1) appLabel else context.getString(R.string.installer_installing)
        return builder.setContentTitle(title).setContentText(content)
            .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon, if (progress.total > 1) progress.current - 1 else null)).build()
    }

    private suspend fun onInstallingModule(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.InstallingModule,
        preferSystemIcon: Boolean
    ): Notification = builder.setContentTitle(context.getString(R.string.installer_installing))
        .setContentText(progress.output.lastOrNull() ?: context.getString(R.string.installer_installing)).setProgress(100, 50, true)
        .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon)).build()

    private suspend fun onInstallFailed(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): NotificationCompat.Builder {
        val info = installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }.getInfo(context)
        val contentText = context.getString(R.string.installer_install_failed)
        return builder.setContentTitle(info.title).setContentText(contentText).setStyle(
            NotificationCompat.BigTextStyle().setBigContentTitle(info.title).bigText("$contentText\n${installer.error.getErrorMessage(context)}")
        ).setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon)).addAction(0, context.getString(R.string.retry), helper.installIntent)
            .addAction(0, context.getString(R.string.cancel), helper.finishIntent)
    }

    private suspend fun onInstallSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): NotificationCompat.Builder {
        val entities = installer.analysisResults.flatMap { it.appEntities }.filter { it.selected }.map { it.app }
        var newBuilder =
            builder.setContentTitle(entities.getInfo(context).title).setContentText(context.getString(R.string.installer_install_success))
                .setLargeIcon(helper.getLargeIconBitmap(preferSystemIcon))
        val openPendingIntent = helper.getLaunchPendingIntent(entities.firstOrNull()?.packageName)
        if (openPendingIntent != null) newBuilder = newBuilder.addAction(0, context.getString(R.string.open), openPendingIntent)
        return newBuilder.addAction(0, context.getString(R.string.finish), helper.finishIntent)
    }

    private fun onInstallCompleted(builder: NotificationCompat.Builder, progress: ProgressEntity.InstallCompleted): NotificationCompat.Builder {
        val successCount = progress.results.count { it.success }
        val title =
            if (successCount == progress.results.size) context.getString(R.string.installer_install_success) else "${context.getString(R.string.installer_install_success)}: $successCount/${progress.results.size}"
        return builder.setContentTitle(title).setContentText(context.getString(R.string.installer_live_channel_short_text_success))
            .addAction(0, context.getString(R.string.finish), helper.finishIntent)
    }
}