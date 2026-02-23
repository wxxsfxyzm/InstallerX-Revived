// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.installer.model.impl.installer.helper

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmapOrNull
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.impl.installer.BroadcastHandler
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

class NotificationHelper(
    private val context: Context,
    private val installer: InstallerRepo,
    private val appIconRepo: AppIconRepo
) {
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

    val openIntent: PendingIntent? = BroadcastHandler.openIntent(context, installer)
    val analyseIntent: PendingIntent? = BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Analyse)
    val installIntent: PendingIntent? = BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Install)
    val cancelIntent: PendingIntent? = BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Cancel)
    val finishIntent: PendingIntent? = BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Finish)

    // Resolve specific launch intent considering privileged access
    fun getLaunchPendingIntent(packageName: String?): PendingIntent? {
        val launchIntent = packageName?.let {
            context.packageManager.getLaunchIntentForPackage(it)
        } ?: return null

        val supportsPrivileged = installer.config.authorizer in listOf(
            ConfigEntity.Authorizer.Root,
            ConfigEntity.Authorizer.Shizuku,
            ConfigEntity.Authorizer.Customize
        )

        return if (supportsPrivileged) {
            BroadcastHandler.privilegedLaunchAndFinishIntent(context, installer)
        } else {
            BroadcastHandler.launchIntent(context, installer, launchIntent)
        }
    }

    // Retrieve specific icon from multi-install queue or analysis results
    suspend fun getLargeIconBitmap(preferSystemIcon: Boolean, currentBatchIndex: Int? = null): Bitmap? {
        val entityFromQueue = if (currentBatchIndex != null && installer.multiInstallQueue.isNotEmpty()) {
            installer.multiInstallQueue.getOrNull(currentBatchIndex)?.app
        } else null

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

        val iconSizePx = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        val drawable = appIconRepo.getIcon(
            sessionId = installer.id,
            packageName = entityToInstall.packageName,
            entityToInstall = entityToInstall,
            iconSizePx = iconSizePx,
            preferSystemIcon = preferSystemIcon
        )

        return drawable?.toBitmapOrNull(width = iconSizePx, height = iconSizePx)
    }
}
