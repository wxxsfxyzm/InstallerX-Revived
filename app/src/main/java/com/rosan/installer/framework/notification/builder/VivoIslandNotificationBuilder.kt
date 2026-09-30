// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.framework.notification.NotificationHelper
import com.rosan.installer.framework.privileged.core.execution.dispatcher.useUserService
import com.rosan.installer.framework.privileged.core.execution.runtime.DefaultPrivilegedService

private const val OPERATION = "notification.superx.operation"
private const val SCENE = "notification.superx.scene"
private const val TEMPLATE = "notification.superx.template"
private const val CHANGED_RECORD = "notification.superx.changedRecord"
private const val NEW_NODE = "notification.superx.newNode"
private const val DISPLAYS = "notification.superx.displays"
private const val SHOW_NOTIFY = "notification.superx.showNotify"
private const val ISLAND_NOTIFY = "notification.superx.islandNotify"
private const val ISLAND_INFO = "notification.superx.island"
private const val SHORT_INFOS = "notification.superx.shortInfos"
private const val SHORT_CORE = "notification.superx.shortInfos.coreInfoShort"
private const val SHORT_DESCRIBE = "notification.superx.shortInfos.describeShort"
private const val SHORT_IMAGE = "notification.superx.shortInfos.image"
private const val SHORT_IMAGE_CLICK = "notification.superx.shortInfos.imageClickResp"
private const val CLICK_RESP = "notification.superx.clickResp"

private const val ISLAND_TEMPLATE = "island.superx.template"
private const val ISLAND_INFOS = "island.superx.infos"
private const val BASE_INFOS = "notification.superx.baseInfos"
private const val ISLAND_BASE_INFOS = "island.superx.baseInfos"
private const val BASE_TITLE = "notification.superx.baseInfos.title"
private const val BASE_CONTENT = "notification.superx.baseInfos.content"
private const val BASE_ICON = "notification.superx.baseInfos.icon"
private const val BASE_SUB_INFO = "notification.superx.baseInfos.subInfo"
private const val BASE_SUB_IMAGE = "notification.superx.baseInfos.subImage"
private const val CARD_BG_COLOR = "notification.superx.cardBgColor"
private const val DISABLE_INVERT_COLOR = "notification.superx.disableInvertColor"
private const val INFOS = "notification.superx.infos"
private const val INFO_NODE_ICON = "notification.superx.infos.nodeIcon"
private const val INFO_PROGRESS = "notification.superx.infos.progress"
private const val INFO_PROGRESS_COLOR = "notification.superx.infos.progressColor"
private const val INFO_BUTTON_TYPE = "notification.superx.infos.btnType"
private const val INFO_BUTTON_TEXT_COLOR_LIST = "notification.superx.infos.btnTextColorList"
private const val INFO_BUTTON_TEXT_LIST = "notification.superx.infos.btnTextList"
private const val INFO_BUTTON_ICON_LIST = "notification.superx.infos.btnIconList"
private const val INFO_BUTTON_COLOR_LIST = "notification.superx.infos.btnColorList"
private const val INFO_BUTTON_CLICK_RESP_LIST = "notification.superx.infos.btnClickRespList"
private const val ISLAND_LEFT_TEMPLATE = "island.superx.leftTemplate"
private const val ISLAND_RIGHT_TEMPLATE = "island.superx.rightTemplate"
private const val ISLAND_LEFT_INFO = "island.superx.leftInfo"
private const val ISLAND_RIGHT_INFO = "island.superx.rightInfo"
private const val ISLAND_LEFT_ICON = "island.superx.leftInfo.icon"
private const val ISLAND_LEFT_CONTENT = "island.superx.leftInfo.content"
private const val ISLAND_RIGHT_CONTENT = "island.superx.rightInfo.content"
private const val ISLAND_RIGHT_ICON = "island.superx.rightInfo.icon"
private const val ISLAND_RIGHT_CAPSULE_CONTENT = "island.superx.rightInfo.capsuleContent"
private const val ISLAND_CLICK = "island.superx.clickResp"
private const val ISLAND_LANDING_PACKAGE = "island.superx.landingPkg"
private const val ISLAND_LANDING_INFO = "island.superx.landingInfo"
private const val ISLAND_SCENE = "island.superx.scene"
private const val ISLAND_CLICK_TYPE = "island.superx.islandClick"
private const val ISLAND_SHOW_TIME = "island.superx.showTime"
private const val ISLAND_AFTER_SLIDE_CARD = "island.superx.islandAfterSlideCard"
private const val ISLAND_PRIORITY = "island.superx.priority"
private const val ISLAND_BUSINESS = "island.superx.business"
private const val ISLAND_TYPE = "island.superx.islandType"
private const val ISLAND_SHOW_TYPE = "island.superx.islandShowType"
private const val ISLAND_PERMANENT = "island.superx.permanent"
private const val ISLAND_CLIP_TO_OUTLINE = "island.superx.clipToOutline"
private const val ISLAND_CAPSULE_WIDTH = "island.superx.capsule.width"
private const val ISLAND_CUSTOM_TEMPLATE = "island.superx.customTemplate"
private const val NOTIFICATION_CUSTOM_TEMPLATE = "notification.superx.customTemplate"
private const val ISLAND_KEEP_SCREEN_ON = "notification.superx.keepScreenOn"

private const val SCENE_PREFIX = "superx_scene_installerx_"
private const val ISLAND_TEMPLATE_NORMAL = 4
private const val ISLAND_TEMPLATE_PROGRESS = 2
private const val ISLAND_TEMPLATE_CUSTOM = 7
private const val ISLAND_TEMPLATE_BUTTONS = 8
private const val ISLAND_LEFT_TEMPLATE_IMAGE_ONLY = 4
private const val ISLAND_RIGHT_TEMPLATE_TEXT_IMAGE = 4
private const val ISLAND_DISPLAYS_STATUS_BAR = 256
private const val ISLAND_CLICK_SHOW_CARD = 0
private const val ISLAND_AFTER_SLIDE_CARD_COLLAPSE = 0
private val ISLAND_PROGRESS_COLOR = Color.rgb(0, 110, 255)
private val ISLAND_PROGRESS_BG_COLOR = Color.rgb(160, 160, 160)
private val ISLAND_ACTION_COLOR = Color.argb(51, 255, 255, 255)
private val ISLAND_ACTION_HIGHLIGHT_COLOR = Color.rgb(0, 110, 255)
private val ISLAND_ACTION_TEXT_COLOR = Color.WHITE
private const val COLLAPSED_PROGRESS_SIZE_DP = 20f
private const val COLLAPSED_PROGRESS_STROKE_DP = 2.3f
private const val COLLAPSED_LOGO_CONTAINER_DP = 24f
private const val COLLAPSED_LOGO_CONTENT_DP = 18f
private const val EXPANDED_LOGO_CONTAINER_DP = 40f
private const val EXPANDED_LOGO_CONTENT_DP = 20f
private const val TRANSPARENT_ICON_SIZE_DP = 24f
private const val NEVER_EXPIRE_SHOW_TIME_SECONDS = Int.MAX_VALUE

/**
 * Adapts the installer notification to OriginOS SuperX/Vivo Island.
 *
 * OriginOS requires a privileged caller to register the app scene before a non-system app
 * can submit a SuperX notification.
 */
class VivoIslandNotificationBuilder(
    private val context: Context,
    private val session: InstallerSessionRepository,
    private val helper: NotificationHelper,
    private val authorizer: Authorizer,
    private val bypassRestriction: Boolean,
) : InstallerNotificationBuilder {
    private val contentFactory = IslandNotificationContentFactory(context, session, helper)
    private val isSystemApp = context.applicationInfo.flags.hasFlag(ApplicationInfo.FLAG_SYSTEM)
    private val sceneName = SCENE_PREFIX + context.packageName.replace('.', '_')
    private var sceneRegistered = false
    private var islandCreated = false
    private var changedRecord = 0
    private var newNode = 0
    private var lastNodeKey: String? = null

    override suspend fun build(payload: NotificationPayload): Notification? {
        when (payload.state.progress) {
            is ProgressEntity.InstallAnalysedUnsupported,
            ProgressEntity.Error,
            ProgressEntity.Finish,
            -> return null

            else -> Unit
        }

        ensureSceneRegistered()

        val content = contentFactory.create(
            progress = payload.state.progress,
            cancelIntent = helper.cancelIntent,
            fakeItemProgress = payload.animation.fakeItemProgress,
        )
        val notification = createBaseNotification(payload, content)
        addIslandExtras(notification, payload, content)
        return notification
    }

    fun resetLifecycle() {
        islandCreated = false
    }

    fun markLifecycleStarted() {
        islandCreated = true
    }

    private suspend fun createBaseNotification(
        payload: NotificationPayload,
        content: IslandContent,
    ): Notification {
        val progress = payload.state.progress
        val icon = (if (isWorking(progress)) NotificationHelper.Icon.Working else NotificationHelper.Icon.Pausing).resId
        val contentIntent = notificationContentIntent(payload.settings.showDialog)

        val builder = NotificationCompat.Builder(context, NotificationHelper.Channel.InstallerLiveChannel.value)
            .setSmallIcon(icon)
            .setContentTitle(content.title)
            .setContentIntent(contentIntent)
            .setDeleteIntent(helper.finishIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setOngoing(false)

        if (content.contentText.isNotEmpty()) builder.setContentText(content.contentText)
        if (progress.isSuccessfulInstall() && payload.settings.successAutoClearSeconds > 0) {
            builder.setTimeoutAfter(payload.settings.successAutoClearSeconds * 1000L)
        }
        content.progressValue?.let { builder.setProgress(100, it, false) }
        content.actions.forEach { action ->
            builder.addAction(0, action.title, action.pendingIntent)
        }
        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed ||
            progress is ProgressEntity.InstallCompleted
        ) {
            builder.setOnlyAlertOnce(false)
        }

        if (content.showAppIcon) {
            helper.getLargeIconBitmap(
                payload.settings.preferSystemIcon,
                contentFactory.batchIndexFor(progress),
            )?.let { builder.setLargeIcon(it) }
        }
        return builder.build()
    }

    private suspend fun addIslandExtras(
        notification: Notification,
        payload: NotificationPayload,
        content: IslandContent,
    ) {
        val progress = payload.state.progress
        val logo = Icon.createWithBitmap(createLogoBitmap(COLLAPSED_LOGO_CONTAINER_DP, COLLAPSED_LOGO_CONTENT_DP))
        val expandedLogo = Icon.createWithBitmap(createLogoBitmap(EXPANDED_LOGO_CONTAINER_DP, EXPANDED_LOGO_CONTENT_DP))
        val contentIntent = notificationContentIntent(payload.settings.showDialog)
        val appIcon = if (content.showAppIcon) {
            helper.getLargeIconBitmap(
                payload.settings.preferSystemIcon,
                contentFactory.batchIndexFor(progress),
            )?.let { Icon.createWithBitmap(it) }
                ?: logo
        } else {
            logo
        }
        val changeRecord = ++changedRecord
        val node = nodeFor(progress, content)
        val cardTemplate = cardTemplate(progress, content)
        val rightContent = content.shortText.ifEmpty { content.title }
        val (displayTitle, displayContent) = displayInfo(progress, content)
        val shortInfos = Bundle().apply {
            putString(SHORT_CORE, content.title)
            putString(SHORT_DESCRIBE, rightContent)
            putParcelable(SHORT_IMAGE, appIcon)
            contentIntent?.let { putParcelable(SHORT_IMAGE_CLICK, it) }
        }
        val leftInfo = Bundle().apply {
            putParcelable(ISLAND_LEFT_ICON, appIcon)
            putCharSequence(ISLAND_LEFT_CONTENT, content.title)
        }
        val rightInfo = Bundle().apply {
            putCharSequence(ISLAND_RIGHT_CONTENT, rightContent)
            if (progress is ProgressEntity.InstallPreparing) {
                putParcelable(ISLAND_RIGHT_ICON, createCollapsedProgressIcon(content.progressValue ?: 0))
            }
            putCharSequence(ISLAND_RIGHT_CAPSULE_CONTENT, rightContent)
        }
        val cardBaseInfo = Bundle().apply {
            putCharSequence(BASE_TITLE, displayTitle)
            putCharSequence(BASE_CONTENT, displayContent.ifEmpty { " " })
            if (content.showAppIcon) {
                putParcelable(BASE_ICON, appIcon)
                putInt(BASE_SUB_INFO, 3)
                putParcelable(BASE_SUB_IMAGE, expandedLogo)
            }
        }
        val cardInfos = when (cardTemplate) {
            ISLAND_TEMPLATE_PROGRESS -> progressCardInfo(content)
            ISLAND_TEMPLATE_BUTTONS -> buttonCardInfo(content)
            else -> null
        }
        val customTemplate = if (cardTemplate == ISLAND_TEMPLATE_CUSTOM) {
            createReadingCardRemoteViews(content, displayTitle, displayContent)
        } else {
            null
        }
        val islandInfo = Bundle().apply {
            putInt(ISLAND_TEMPLATE, cardTemplate)
            putBundle(ISLAND_BASE_INFOS, cardBaseInfo)
            cardInfos?.let { putBundle(ISLAND_INFOS, it) }
            putInt(ISLAND_LEFT_TEMPLATE, ISLAND_LEFT_TEMPLATE_IMAGE_ONLY)
            putInt(ISLAND_RIGHT_TEMPLATE, ISLAND_RIGHT_TEMPLATE_TEXT_IMAGE)
            putBundle(ISLAND_LEFT_INFO, leftInfo)
            putBundle(ISLAND_RIGHT_INFO, rightInfo)
            contentIntent?.let { putParcelable(ISLAND_CLICK, it) }
            putString(ISLAND_LANDING_PACKAGE, context.packageName)
            putString(ISLAND_LANDING_INFO, "{\"mType\":\"app\",\"mPkgName\":\"${context.packageName}\"}")
            putString(ISLAND_SCENE, sceneName)
            putInt(ISLAND_CLICK_TYPE, ISLAND_CLICK_SHOW_CARD)
            putInt(ISLAND_AFTER_SLIDE_CARD, ISLAND_AFTER_SLIDE_CARD_COLLAPSE)
            putInt(ISLAND_SHOW_TIME, progress.islandShowTime(payload.settings.successAutoClearSeconds))
            putInt(ISLAND_PRIORITY, 3)
            putString(ISLAND_BUSINESS, "installerx")
            putInt(ISLAND_TYPE, 0)
            putInt(ISLAND_SHOW_TYPE, 0)
            putBoolean(ISLAND_PERMANENT, false)
            putBoolean(ISLAND_CLIP_TO_OUTLINE, true)
            putInt(ISLAND_CAPSULE_WIDTH, 0)
            customTemplate?.let { putParcelable(ISLAND_CUSTOM_TEMPLATE, it) }
        }

        val extras = notification.extras ?: Bundle().also { notification.extras = it }
        extras.putInt(OPERATION, if (islandCreated) 1 else 0)
        extras.putInt(TEMPLATE, cardTemplate)
        extras.putString(SCENE, sceneName)
        extras.putInt(CHANGED_RECORD, changeRecord)
        extras.putInt(NEW_NODE, node)
        extras.putInt(DISPLAYS, ISLAND_DISPLAYS_STATUS_BAR)
        extras.putBoolean(SHOW_NOTIFY, true)
        extras.putBoolean(ISLAND_NOTIFY, false)
        extras.putInt(CARD_BG_COLOR, Color.BLACK)
        extras.putBoolean(DISABLE_INVERT_COLOR, true)
        contentIntent?.let { extras.putParcelable(CLICK_RESP, it) }
        extras.putBoolean(ISLAND_KEEP_SCREEN_ON, false)
        extras.putBundle(BASE_INFOS, cardBaseInfo)
        cardInfos?.let { extras.putBundle(INFOS, it) }
        customTemplate?.let { extras.putParcelable(NOTIFICATION_CUSTOM_TEMPLATE, it) }
        extras.putBundle(SHORT_INFOS, shortInfos)
        extras.putBundle(ISLAND_INFO, islandInfo)
    }

    private fun notificationContentIntent(showDialog: Boolean): PendingIntent? = when (session.config.installMode) {
        InstallMode.Notification,
        InstallMode.AutoNotification,
        -> if (showDialog) helper.openIntent else null

        else -> helper.openIntent
    }

    private fun cardTemplate(progress: ProgressEntity, content: IslandContent): Int = when {
        content.actions.isNotEmpty() -> ISLAND_TEMPLATE_BUTTONS
        progress is ProgressEntity.InstallPreparing -> ISLAND_TEMPLATE_CUSTOM
        content.progressValue != null -> ISLAND_TEMPLATE_PROGRESS
        else -> ISLAND_TEMPLATE_NORMAL
    }

    private fun displayInfo(
        progress: ProgressEntity,
        content: IslandContent,
    ): Pair<CharSequence, CharSequence> = if (progress is ProgressEntity.InstallAnalysedSuccess && content.showAppIcon) {
        content.contentText to content.title
    } else {
        content.title to content.contentText
    }

    private fun progressCardInfo(content: IslandContent): Bundle = Bundle().apply {
        // Vivo validates this list and rejects progress cards with fewer than two icons.
        val placeholder = transparentIcon()
        putParcelableArrayList(
            INFO_NODE_ICON,
            ArrayList<Icon>(2).apply {
                add(placeholder)
                add(placeholder)
            },
        )
        putInt(INFO_PROGRESS, content.progressValue ?: 0)
        putInt(INFO_PROGRESS_COLOR, ISLAND_PROGRESS_COLOR)
    }

    private fun createReadingCardRemoteViews(
        content: IslandContent,
        title: CharSequence,
        body: CharSequence,
    ): android.widget.RemoteViews = android.widget.RemoteViews(
        context.packageName,
        R.layout.notification_vivo_island_reading,
    ).apply {
        setTextViewText(R.id.vivo_reading_title, title)
        setTextViewText(R.id.vivo_reading_content, body)
        setProgressBar(
            R.id.vivo_reading_progress,
            100,
            content.progressValue ?: 0,
            content.progressValue == null,
        )
        setImageViewBitmap(
            R.id.vivo_reading_logo,
            createLogoBitmap(EXPANDED_LOGO_CONTAINER_DP, EXPANDED_LOGO_CONTENT_DP),
        )
    }

    private fun transparentIcon(): Icon {
        val density = context.resources.displayMetrics.density
        val size = (TRANSPARENT_ICON_SIZE_DP * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            this.density = context.resources.displayMetrics.densityDpi
        }
        return Icon.createWithBitmap(bitmap)
    }

    private fun buttonCardInfo(content: IslandContent): Bundle = Bundle().apply {
        val actions = content.actions.take(3)
        putInt(INFO_BUTTON_TYPE, 1)
        putIntegerArrayList(INFO_BUTTON_TEXT_COLOR_LIST, ArrayList(actions.map { ISLAND_ACTION_TEXT_COLOR }))
        putStringArrayList(INFO_BUTTON_TEXT_LIST, ArrayList(actions.map { it.title }))
        putParcelableArrayList(
            INFO_BUTTON_ICON_LIST,
            ArrayList<Icon?>(actions.size).apply { repeat(actions.size) { add(null) } },
        )
        putIntegerArrayList(
            INFO_BUTTON_COLOR_LIST,
            ArrayList(
                actions.map { action ->
                    if (action.isHighlighted) ISLAND_ACTION_HIGHLIGHT_COLOR else ISLAND_ACTION_COLOR
                },
            ),
        )
        putParcelableArrayList(
            INFO_BUTTON_CLICK_RESP_LIST,
            ArrayList(actions.map { it.pendingIntent }),
        )
    }

    private fun ensureSceneRegistered() {
        if (sceneRegistered) return
        check(bypassRestriction || isSystemApp) {
            "Vivo Island requires system app access or bypass restriction"
        }

        val accepted = if (isSystemApp) {
            DefaultPrivilegedService.system().registerVivoIslandScene(sceneName, context.packageName)
        } else {
            var accepted = false
            useUserService(
                isSystemApp = false,
                authorizer = authorizer,
            ) { userService ->
                accepted = userService.privileged.registerVivoIslandScene(sceneName, context.packageName)
            }
            accepted
        }
        check(accepted) { "OriginOS rejected the SuperX scene registration" }
        sceneRegistered = true
    }

    private fun createCollapsedProgressIcon(progress: Int): Icon {
        val density = context.resources.displayMetrics.density
        val size = (COLLAPSED_PROGRESS_SIZE_DP * density).toInt().coerceAtLeast(1)
        val stroke = COLLAPSED_PROGRESS_STROKE_DP * density
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            this.density = context.resources.displayMetrics.densityDpi
        }
        val center = size / 2f
        val radius = center - stroke / 2f
        val bounds = RectF(center - radius, center - radius, center + radius, center + radius)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        val canvas = Canvas(bitmap)
        paint.color = ISLAND_PROGRESS_BG_COLOR
        canvas.drawCircle(center, center, radius, paint)
        paint.color = ISLAND_PROGRESS_COLOR
        canvas.drawArc(bounds, -90f, 360f * progress.coerceIn(0, 100) / 100f, false, paint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun createLogoBitmap(containerDp: Float, contentDp: Float): Bitmap {
        val density = context.resources.displayMetrics.density
        val canvasSize = (containerDp * density).toInt().coerceAtLeast(1)
        val logoSize = (contentDp * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888).apply {
            this.density = context.resources.displayMetrics.densityDpi
        }
        context.getDrawable(R.drawable.ic_notification_logo)?.let { drawable ->
            val inset = (canvasSize - logoSize) / 2
            drawable.setBounds(inset, inset, inset + logoSize, inset + logoSize)
            drawable.draw(Canvas(bitmap))
        }
        return bitmap
    }

    private fun ProgressEntity.islandShowTime(successAutoClearSeconds: Int): Int = if (isSuccessfulInstall()) {
        successAutoClearSeconds.takeIf { it > 0 } ?: NEVER_EXPIRE_SHOW_TIME_SECONDS
    } else {
        NEVER_EXPIRE_SHOW_TIME_SECONDS
    }

    private fun ProgressEntity.isSuccessfulInstall(): Boolean = this is ProgressEntity.InstallSuccess ||
        (this is ProgressEntity.InstallCompleted && this.results.all { it.success })

    private fun isWorking(progress: ProgressEntity): Boolean = progress is ProgressEntity.Ready ||
        progress is ProgressEntity.InstallResolving ||
        progress is ProgressEntity.InstallResolveSuccess ||
        progress is ProgressEntity.InstallAnalysing ||
        progress is ProgressEntity.InstallAnalysedSuccess ||
        progress is ProgressEntity.Installing ||
        progress is ProgressEntity.InstallingModule ||
        progress is ProgressEntity.InstallSuccess ||
        progress is ProgressEntity.InstallCompleted

    private fun nodeFor(progress: ProgressEntity, content: IslandContent): Int {
        val key = when (progress) {
            is ProgressEntity.Installing -> "installing:${progress.current}:${progress.total}:${progress.appLabel}"

            is ProgressEntity.InstallPreparing -> "preparing"

            else ->
                progress::class.qualifiedName + ":" + content.title + ":" + content.shortText + ":" +
                    content.contentText + ":" + content.actions.joinToString { it.title }
        }
        if (key != lastNodeKey) {
            lastNodeKey = key
            newNode++
        }
        return newNode
    }
}
