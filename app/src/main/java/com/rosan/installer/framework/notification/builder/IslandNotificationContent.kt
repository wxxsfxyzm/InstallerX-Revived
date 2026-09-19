// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification.builder

import android.app.PendingIntent
import android.content.Context
import com.rosan.installer.R
import com.rosan.installer.domain.engine.model.packageinfo.getInfo
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.framework.notification.NotificationHelper
import com.rosan.installer.util.getErrorMessage

internal data class IslandAction(
    val key: String,
    val title: String,
    val pendingIntent: PendingIntent,
    val isHighlighted: Boolean = false,
)

internal data class IslandContent(
    val title: String,
    val shortText: String,
    val contentText: String = "",
    val progressValue: Int? = null,
    val isOngoing: Boolean = false,
    val showAppIcon: Boolean = true,
    val actions: List<IslandAction> = emptyList(),
)

/** Shared installer states used by both the Xiaomi and vivo island adapters. */
internal class IslandNotificationContentFactory(
    private val context: Context,
    private val session: InstallerSessionRepository,
    private val helper: NotificationHelper,
) {
    fun create(
        progress: ProgressEntity,
        cancelIntent: PendingIntent,
        fakeItemProgress: Float = 0f,
    ): IslandContent = when (progress) {
        ProgressEntity.Ready -> IslandContent(
            title = context.getString(R.string.installer_ready),
            shortText = context.getString(R.string.installer_ready),
        )

        ProgressEntity.InstallResolving -> IslandContent(
            title = context.getString(R.string.installer_resolving),
            shortText = context.getString(R.string.installer_live_channel_short_text_resolving),
            contentText = context.getString(R.string.installer_resolving_desc),
            isOngoing = true,
            showAppIcon = false,
            actions = listOf(cancelAction(cancelIntent)),
        )

        ProgressEntity.InstallResolveSuccess -> IslandContent(
            title = context.getString(R.string.installer_resolve_success),
            shortText = context.getString(R.string.installer_live_channel_short_text_resolving),
            showAppIcon = false,
            actions = listOf(cancelAction(cancelIntent)),
        )

        is ProgressEntity.InstallPreparing -> IslandContent(
            title = context.getString(R.string.installer_preparing),
            shortText = context.getString(R.string.installer_live_channel_short_text_preparing),
            contentText = context.getString(R.string.installer_preparing_desc),
            progressValue = progress.progress.takeIf { it >= 0f }?.toIslandProgress(),
            isOngoing = true,
            showAppIcon = false,
        )

        ProgressEntity.InstallResolvedFailed -> IslandContent(
            title = context.getString(R.string.installer_resolve_failed),
            shortText = context.getString(R.string.installer_live_channel_short_text_resolve_failed),
            contentText = session.error.getErrorMessage(context),
            showAppIcon = false,
            actions = listOf(cancelAction(cancelIntent)),
        )

        ProgressEntity.InstallAnalysing -> IslandContent(
            title = context.getString(R.string.installer_analysing),
            shortText = context.getString(R.string.installer_live_channel_short_text_analysing),
            isOngoing = true,
            showAppIcon = false,
            actions = listOf(cancelAction(cancelIntent)),
        )

        ProgressEntity.InstallAnalysedSuccess -> createAnalysedSuccessContent(cancelIntent)

        ProgressEntity.InstallAnalysedFailed -> IslandContent(
            title = context.getString(R.string.installer_analyse_failed),
            shortText = context.getString(R.string.installer_live_channel_short_text_analyse_failed),
            contentText = session.error.getErrorMessage(context),
            showAppIcon = false,
            actions = listOf(
                cancelAction(cancelIntent),
                IslandAction(
                    key = "miui_action_retry",
                    title = context.getString(R.string.retry),
                    pendingIntent = helper.analyseIntent,
                ),
            ),
        )

        is ProgressEntity.Installing -> {
            val appLabel = progress.appLabel ?: context.getString(R.string.installer_installing)
            val contentText = if (progress.total > 1) {
                "(${progress.current}/${progress.total}) $appLabel"
            } else {
                appLabel
            }
            val total = progress.total.coerceAtLeast(1).toFloat()
            val currentBase = (progress.current - 1).coerceAtLeast(0).toFloat()
            val itemProgress = progress.writeProgress ?: fakeItemProgress
            val batchFraction = (currentBase + itemProgress) / total
            IslandContent(
                title = context.getString(R.string.installer_installing),
                shortText = context.getString(R.string.installer_live_channel_short_text_installing),
                contentText = contentText,
                progressValue = (100 * batchFraction).toInt(),
                isOngoing = true,
            )
        }

        is ProgressEntity.InstallingModule -> IslandContent(
            title = context.getString(R.string.installer_installing),
            shortText = context.getString(R.string.installer_live_channel_short_text_installing),
            contentText = progress.output.lastOrNull() ?: context.getString(R.string.installer_installing),
            isOngoing = true,
        )

        ProgressEntity.InstallSuccess -> IslandContent(
            title = context.getString(R.string.installer_install_success),
            shortText = context.getString(R.string.installer_live_channel_short_text_success),
            contentText = selectedAppsTitle(),
            actions = buildList {
                add(IslandAction("miui_action_finish", context.getString(R.string.finish), helper.finishIntent))
                helper.getLaunchPendingIntent(selectedPackageName())?.let {
                    add(IslandAction("miui_action_open", context.getString(R.string.open), it, isHighlighted = true))
                }
            },
        )

        is ProgressEntity.InstallCompleted -> {
            val successCount = progress.results.count { it.success }
            val totalCount = progress.results.size
            val allSucceeded = successCount == totalCount
            IslandContent(
                title = if (allSucceeded) {
                    context.getString(R.string.installer_install_success)
                } else {
                    "${context.getString(R.string.installer_install_success)}: $successCount/$totalCount"
                },
                shortText = if (allSucceeded) {
                    context.getString(R.string.installer_live_channel_short_text_success)
                } else {
                    "$successCount/$totalCount ${context.getString(R.string.installer_live_channel_short_text_success)}"
                },
                contentText = context.getString(R.string.installer_live_channel_short_text_success),
                actions = listOf(IslandAction("miui_action_finish", context.getString(R.string.finish), helper.finishIntent)),
            )
        }

        ProgressEntity.InstallWaitingUnknownSource -> IslandContent(
            title = context.getString(R.string.installer_waiting_unknown_source),
            shortText = context.getString(R.string.installer_waiting_unknown_source),
            contentText = helper.unknownSourceDescription(),
            showAppIcon = false,
            actions = listOf(
                IslandAction(
                    key = "miui_action_unknown_source",
                    title = context.getString(R.string.suggestion_allow_unknown_source),
                    pendingIntent = helper.unknownSourceIntent,
                    isHighlighted = true,
                ),
                cancelAction(cancelIntent),
            ),
        )

        ProgressEntity.InstallFailed -> IslandContent(
            title = context.getString(R.string.installer_install_failed),
            shortText = context.getString(R.string.installer_live_channel_short_text_install_failed),
            contentText = selectedAppsTitle(),
            actions = listOf(
                cancelAction(cancelIntent),
                IslandAction("miui_action_retry", context.getString(R.string.retry), helper.installIntent),
            ),
        )

        is ProgressEntity.InstallAnalysedUnsupported,
        ProgressEntity.Error,
        ProgressEntity.Finish,
        -> error("Unsupported notification state: $progress")

        else -> IslandContent(
            title = context.getString(R.string.installer_ready),
            shortText = context.getString(R.string.installer_ready),
        )
    }

    fun batchIndexFor(progress: ProgressEntity): Int? = (progress as? ProgressEntity.Installing)
        ?.takeIf { it.total > 1 }
        ?.current
        ?.minus(1)

    private fun createAnalysedSuccessContent(cancelIntent: PendingIntent): IslandContent {
        val allEntities = session.analysisResults.flatMap { it.appEntities }
        val selectedApps = allEntities.map { it.app }
        val hasComplexType = allEntities.any {
            it.app.sourceType == DataType.MIXED_MODULE_APK ||
                it.app.sourceType == DataType.MIXED_MODULE_ZIP
        }
        val isMultiPackage = selectedApps.groupBy { it.packageName }.size > 1

        if (hasComplexType || isMultiPackage) {
            return IslandContent(
                title = context.getString(R.string.installer_prepare_install),
                shortText = context.getString(R.string.installer_live_channel_short_text_pending),
                contentText = if (hasComplexType) {
                    context.getString(R.string.installer_mixed_module_apk_description_notification)
                } else {
                    context.getString(R.string.installer_multi_apk_description_notification)
                },
                showAppIcon = false,
                actions = listOf(cancelAction(cancelIntent)),
            )
        }

        return IslandContent(
            title = context.getString(R.string.installer_prepare_type_unknown_confirm),
            shortText = context.getString(R.string.installer_live_channel_short_text_pending_install),
            contentText = selectedApps.getInfo(context).title,
            actions = listOf(
                cancelAction(cancelIntent),
                IslandAction(
                    key = "miui_action_install",
                    title = context.getString(R.string.install),
                    pendingIntent = helper.installIntent,
                    isHighlighted = true,
                ),
            ),
        )
    }

    private fun cancelAction(pendingIntent: PendingIntent) = IslandAction(
        key = "miui_action_cancel",
        title = context.getString(R.string.cancel),
        pendingIntent = pendingIntent,
    )

    private fun selectedApps() = session.analysisResults
        .flatMap { it.appEntities }
        .filter { it.selected }
        .map { it.app }

    private fun selectedAppsTitle(): String = selectedApps()
        .getInfo(context)
        .title

    private fun selectedPackageName(): String? = selectedApps()
        .firstOrNull()
        ?.packageName

    private fun Float.toIslandProgress(): Int = (this * 100f).toInt()
}
