package com.rosan.installer.framework.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.rosan.installer.R
import com.rosan.installer.data.session.handler.BroadcastHandler
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.sortedBest
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.Authorizer

class NotificationHelper(
    private val context: Context,
    private val session: InstallerSessionRepository,
    private val getAppIcon: GetAppIconUseCase
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

    val openIntent: PendingIntent = BroadcastHandler.Companion.openIntent(context, session)
    val analyseIntent: PendingIntent = BroadcastHandler.Companion.namedIntent(context, session, BroadcastHandler.Name.Analyse)
    val installIntent: PendingIntent = BroadcastHandler.Companion.namedIntent(context, session, BroadcastHandler.Name.Install)
    val cancelIntent: PendingIntent = BroadcastHandler.Companion.namedIntent(context, session, BroadcastHandler.Name.Cancel)
    val finishIntent: PendingIntent = BroadcastHandler.Companion.namedIntent(context, session, BroadcastHandler.Name.Finish)

    // Resolve specific launch intent considering privileged access
    fun getLaunchPendingIntent(packageName: String?): PendingIntent? {
        val launchIntent = packageName?.let {
            context.packageManager.getLaunchIntentForPackage(it)
        } ?: return null

        val supportsPrivileged = session.config.authorizer in listOf(
            Authorizer.Root,
            Authorizer.Shizuku,
            Authorizer.Customize
        )

        return if (supportsPrivileged) {
            BroadcastHandler.Companion.privilegedLaunchAndFinishIntent(context, session)
        } else {
            BroadcastHandler.Companion.launchIntent(context, session, launchIntent)
        }
    }

    /**
     * Retrieves the large icon for notifications.
     * Supports both installation and uninstallation tasks.
     */
    suspend fun getLargeIconBitmap(preferSystemIcon: Boolean, currentBatchIndex: Int? = null): Bitmap? {
        // Priority 1: Check if this is an uninstallation task
        val uninstallPkg = session.uninstallInfo.value?.packageName

        // Priority 2: Check if this is a multi-install batch
        val entityFromQueue = if (currentBatchIndex != null && session.multiInstallQueue.isNotEmpty()) {
            session.multiInstallQueue.getOrNull(currentBatchIndex)?.app
        } else null

        // Priority 3: Resolve current selected entity for single install
        val entityToInstall = entityFromQueue
            ?: if (uninstallPkg == null) {
                val entities = session.analysisResults
                    .flatMap { it.appEntities }
                    .filter { it.selected }
                    .map { it.app }
                entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
                    ?: entities.sortedBest().firstOrNull()
            } else null

        val iconSizePx = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        // Delegate to UseCase using the resolved package name
        return getAppIcon(
            sessionId = session.id,
            packageName = uninstallPkg ?: entityToInstall?.packageName ?: return null,
            entityToInstall = entityToInstall,
            iconSizePx = iconSizePx,
            preferSystemIcon = preferSystemIcon
        )
    }
}