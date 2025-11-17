package com.rosan.installer.data.installer.model.impl.installer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.getInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.installer.util.MiuiIslandHelper
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.theme.primaryDark
import com.rosan.installer.ui.theme.primaryLight
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.reflect.KClass

class ForegroundInfoHandler(scope: CoroutineScope, installer: InstallerRepo) :
    Handler(scope, installer), KoinComponent {
    companion object {
        private const val MINIMUM_VISIBILITY_DURATION_MS = 400L
    }

    private var job: Job? = null
    private var sessionStartTime: Long = 0L
    private val context by inject<Context>()
    private val appDataStore by inject<AppDataStore>()
    private val appIconRepo by inject<AppIconRepo>()

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationId = installer.id.hashCode() and Int.MAX_VALUE

    @SuppressLint("MissingPermission")
    override suspend fun onStart() {
        Timber.d("[id=${installer.id}] onStart: Starting to combine and collect flows.")
        sessionStartTime = System.currentTimeMillis()
        job = scope.launch {
            combine(
                installer.progress,
                installer.background,
                appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true),
                appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false),
                appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false)
            ) { progress, background, showDialog, showLiveActivity, preferSystemIcon ->

                Timber.i("[id=${installer.id}] Combined Flow: progress=${progress::class.simpleName}, background=$background, showDialog=$showDialog")

                if (progress is ProgressEntity.InstallAnalysedUnsupported) {
                    Timber.w("[id=${installer.id}] AnalysedUnsupported: ${progress.reason}")
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, progress.reason, Toast.LENGTH_LONG).show()
                    }
                    installer.close()
                    return@combine
                }

                if (background) {
                    // --- VERSION CHECK & DISPATCH ---
                // This is the main branching point. It decides which notification style to use.
                val notification = 
                    if (MiuiIslandHelper.isMiuiIslandSupported(context)) {
                        buildMiuiIslandNotification(progress, showDialog, preferSystemIcon)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && showLiveActivity) {
                        buildModernNotification(progress, showDialog, preferSystemIcon)
                    } else {
                        buildLegacyNotification(progress, true, showDialog, preferSystemIcon)
                    }
                setNotification(notification)

                    val elapsedTime = System.currentTimeMillis() - sessionStartTime
                    if (elapsedTime < MINIMUM_VISIBILITY_DURATION_MS) {
                        if (progress !is ProgressEntity.Finish && progress !is ProgressEntity.InstallSuccess) {
                            delay(MINIMUM_VISIBILITY_DURATION_MS - elapsedTime)
                        }
                    }
                } else {
                    Timber.d("[id=${installer.id}] Foreground mode. Cancelling notification.")
                    setNotification(null)
                }

            }.collect()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFinish() {
        Timber.d("[id=${installer.id}] onFinish: Cancelling notification and job.")
        // Clear cache for all involved packages to ensure freshness on next install
        installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app.packageName }
            .distinct()
            .forEach {
                appIconRepo.clearCacheForPackage(it)
            }
        setNotification(null)
        job?.cancel()
    }

    /**
     * 构建小米超级岛通知
     */
    private suspend fun buildMiuiIslandNotification(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = true
    ): Notification? {
        Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Starting to build MIUI Island notification, notificationId=$notificationId")
        Timber.d("[id=${installer.id}] buildMiuiIslandNotification: progress=${progress::class.simpleName}, showDialog=$showDialog, preferSystemIcon=$preferSystemIcon")
        
        // 检查是否支持超级岛通知
        if (!MiuiIslandHelper.isMiuiIslandSupported(context)) {
            Timber.d("[id=${installer.id}] buildMiuiIslandNotification: MIUI Island not supported")
            return null
        }
        Timber.d("[id=${installer.id}] buildMiuiIslandNotification: MIUI Island is supported")

        // 对于终止状态不显示通知
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error) {
            Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Terminal state, not showing notification")
            return null
        }

        // 创建基础通知
        val builder = NotificationCompat.Builder(context, Channel.InstallerLiveChannel.value)
            .setSmallIcon(Icon.LOGO.resId)
            .setContentIntent(if (showDialog) openIntent else null)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        
        // 获取应用图标（如果可能的话）
        val appIcon = try {
            // 尝试获取待安装应用的图标
            when (progress) {
                is ProgressEntity.InstallAnalysedSuccess, 
                is ProgressEntity.Installing, 
                is ProgressEntity.InstallSuccess, 
                is ProgressEntity.InstallFailed -> {
                    getInstallAppIcon(preferSystemIcon)
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] 获取应用图标时出错")
            null
        }

        // 定义主Bundle
        val extrasBundle = Bundle()

        // 添加自定义图片到通知，传入应用图标
        val picsBundle = MiuiIslandHelper.createPicsBundle(context, Icon.LOGO.resId, appIcon)

        extrasBundle.putBundle("miui.focus.pics", picsBundle)


        Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Added custom image support with resId=${Icon.LOGO.resId}, 是否使用应用图标: ${appIcon != null}")

        // 根据进度状态构建通知内容和岛参数
        val notification = when (progress) {
            is ProgressEntity.InstallResolving, is ProgressEntity.InstallResolveSuccess -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Resolving state")
                buildIslandNotificationForResolving(builder, extrasBundle)
            }
            is ProgressEntity.InstallPreparing -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Preparing state, progress=${progress.progress}")
                buildIslandNotificationForPreparing(builder, progress, extrasBundle)
            }
            is ProgressEntity.InstallAnalysing -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Analysing state")
                buildIslandNotificationForAnalysing(builder, extrasBundle)
            }
            is ProgressEntity.InstallAnalysedSuccess -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for AnalysedSuccess state")
                buildIslandNotificationForAnalysedSuccess(builder, preferSystemIcon, extrasBundle)
            }
            is ProgressEntity.Installing -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Installing state")
                buildIslandNotificationForInstalling(builder, progress, extrasBundle)
            }
            is ProgressEntity.InstallSuccess -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Success state")
                buildIslandNotificationForSuccess(builder, preferSystemIcon, extrasBundle)
            }
            is ProgressEntity.InstallFailed -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Failed state, error=${installer.error?.message}")
                buildIslandNotificationForFailed(builder, preferSystemIcon, extrasBundle)
            }
            is ProgressEntity.Ready -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for Ready state")
                buildIslandNotificationForReady(builder,extrasBundle)
            }
            is ProgressEntity.InstallResolvedFailed -> {
                Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Building for ResolvedFailed state")
                buildIslandNotificationForResolvedFailed(builder, extrasBundle)
            }
            else -> {
                Timber.w("[id=${installer.id}] buildMiuiIslandNotification: Unsupported progress state: ${progress::class.simpleName}")
                null
            }
        }

        Timber.d("[id=${installer.id}] buildMiuiIslandNotification: Final notification: ${notification != null}")
        return notification
    }
    
    /**
     * 构建解析状态的超级岛通知
     */
    private fun buildIslandNotificationForResolving(builder: NotificationCompat.Builder, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForResolving: Building notification for resolving state")
        val title = getString(R.string.installer_resolving)
        // 解析状态没有操作按钮
        val params = MiuiIslandHelper.generateIslandParams(title, "...", 0, emptyList())
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForResolving: Title=$title, Progress=0")
        
        // 在builder阶段设置超级岛参数

        extras.putString("miui.focus.param", params)
        val notification = builder.setContentTitle(title)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForResolving: Notification created with MIUI island params")
        return notification
    }
    
    /**
     * 构建准备状态的超级岛通知
     */
    private fun buildIslandNotificationForPreparing(builder: NotificationCompat.Builder, progress: ProgressEntity.InstallPreparing, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForPreparing: Building notification for preparing state")
        val title = getString(R.string.installer_prepare_install)
        val progressValue = (progress.progress * 100).toInt()
        // 准备状态没有操作按钮
        val params = MiuiIslandHelper.generateIslandParams(title, "...", progressValue, emptyList())
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForPreparing: Title=$title, Progress=$progressValue, Indeterminate=${progress.progress < 0}")
        
        // 在builder阶段设置超级岛参数

        extras.putString("miui.focus.param", params)
        val notification = builder.setContentTitle(title)
            .setProgress(100, progressValue, progress.progress < 0)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForPreparing: Notification created with progress")
        return notification
    }
    
    /**
     * 构建分析状态的超级岛通知
     */
    private fun buildIslandNotificationForAnalysing(builder: NotificationCompat.Builder, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysing: Building notification for analysing state")
        val title = getString(R.string.installer_analysing)
        // 分析状态没有操作按钮
        val params = MiuiIslandHelper.generateIslandParams(title, "...", 0, emptyList())
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysing: Title=$title, Progress=0")
        
        // 在builder阶段设置超级岛参数

        extras.putString("miui.focus.param", params)
        val notification = builder.setContentTitle(title)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysing: Notification created with MIUI island params")
        return notification
    }
    
    /**
     * 构建分析完成状态的超级岛通知
     */
    private suspend fun buildIslandNotificationForAnalysedSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysedSuccess: Building notification for analysed success state")
        
        val selectedEntities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
        val selectedApps = selectedEntities.map { it.app }
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysedSuccess: Selected apps count: ${selectedApps.size}")
        
        val title = if (selectedApps.groupBy { it.packageName }.size != 1) {
            getString(R.string.installer_prepare_install)
        } else {
            selectedApps.getInfo(context).title
        }
        val content = getString(R.string.installer_prepare_type_unknown_confirm)
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysedSuccess: Title=$title, Content=$content")
        
        // 创建按钮参数列表
        val actions = mutableListOf<Map<String, Any>>()
        actions.add(MiuiIslandHelper.createCancelActionParams())
        actions.add(MiuiIslandHelper.createInstallActionParams())
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysedSuccess: Added Install and Cancel actions")
        
        // 使用统一的超级岛参数生成方法（始终包含大岛和小岛配置）
        val params = MiuiIslandHelper.generateIslandParams(title, content, 0, actions)
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysedSuccess: Generated island params")
        
        // 在builder阶段设置所有参数

        extras.putString("miui.focus.param", params)
        
        // 创建操作按钮
        val actionsBundle = Bundle()
        // 将finishIntent包装在Notification.Action对象中
        val finishAction = Notification.Action.Builder(
            R.drawable.ic_action_cancel,
            getString(R.string.cancel),
            finishIntent
        ).build()
        actionsBundle.putParcelable("miui.focus.action_finish", finishAction)
        // 将installIntent包装在Notification.Action对象中
        val installAction = Notification.Action.Builder(
            R.drawable.ic_action_install, // 使用MIUI风格图标
            getString(R.string.install),
             installIntent
        ).build()

        actionsBundle.putParcelable("miui.focus.action_install", installAction)

        extras.putBundle("miui.focus.actions", actionsBundle)
        
        val notification = builder.setContentTitle(title)
            .setContentText(content)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForAnalysedSuccess: Notification created with actions")
        return notification
    }
    
    /**
     * 构建安装中状态的超级岛通知
     */
    private suspend fun buildIslandNotificationForInstalling(builder: NotificationCompat.Builder, progress: ProgressEntity.Installing, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForInstalling: Building notification for installing state")
        
        val info = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .getInfo(context)
        val title = info.title
        val content = getString(R.string.installer_installing)
        
        // 安装中状态的进度值固定为80
        val progressValue = 80
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForInstalling: Title=$title, Content=$content, Progress=$progressValue")
        
        // 创建进度按钮参数
        val actions = listOf(MiuiIslandHelper.createProgressActionParams(progressValue))
        Timber.d("[id=${installer.id}] buildIslandNotificationForInstalling: Added Progress action")
        
        val params = MiuiIslandHelper.generateIslandParams(title, content, progressValue, actions)
        Timber.d("[id=${installer.id}] buildIslandNotificationForInstalling: Generated big island params")
        
        // 在builder阶段设置超级岛参数
        extras.putString("miui.focus.param", params)
        val notification = builder.setContentTitle(title)
            .setContentText(content)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForInstalling: Notification created with progress")
        return notification
    }
    
    /**
     * 构建安装成功状态的超级岛通知
     */
    private suspend fun buildIslandNotificationForSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: Building notification for success state")
        
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val info = entities.getInfo(context)
        val title = info.title
        val content = getString(R.string.installer_install_success)
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: Title=$title, Content=$content, Progress=100")
        
        // 创建按钮参数列表
        val actions = mutableListOf<Map<String, Any>>()
        actions.add(MiuiIslandHelper.createCancelActionParams())
        actions.add(MiuiIslandHelper.createOpenActionParams())
        Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: Added Open and Cancel actions")
        
        val params = MiuiIslandHelper.generateIslandParams(title, content, 100, actions)
        Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: Generated big island params")
        
        // 在builder阶段设置所有参数
        extras.putString("miui.focus.param", params)
        
        // 创建操作按钮
        val actionsBundle = android.os.Bundle()
        // 将finishIntent包装在Notification.Action对象中
        val finishAction = Notification.Action.Builder(
            R.drawable.ic_action_cancel, // 使用MIUI风格图标
            getString(R.string.finish),
            finishIntent
        ).build()
        actionsBundle.putParcelable("miui.focus.action_finish", finishAction)
        
        val launchIntent = context.packageManager.getLaunchIntentForPackage(entities.first().packageName)
        val launchPendingIntent = launchIntent?.let {
            BroadcastHandler.launchIntent(context, installer, it)
        }
        if (launchPendingIntent != null) {
            // 将PendingIntent包装在Notification.Action对象中
            val openAction = Notification.Action.Builder(
            R.drawable.ic_notification_logo, // 使用应用图标作为备选
            getString(R.string.open),
            launchPendingIntent
        ).build()
            actionsBundle.putParcelable("miui.focus.action_open", openAction)
            Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: Added launch action for ${entities.first().packageName}")
        } else {
            Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: No launch intent found for ${entities.first().packageName}")
        }
        
        extras.putBundle("miui.focus.actions", actionsBundle)
        
        val notification = builder.setContentTitle(title)
            .setContentText(content)
            .setOngoing(false)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForSuccess: Success notification created with actions")
        return notification
    }
    
    /**
     * 构建安装失败状态的超级岛通知
     */
    private fun buildIslandNotificationForFailed(builder: NotificationCompat.Builder, preferSystemIcon: Boolean, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForFailed: Building notification for failed state")
        
        val title = getString(R.string.installer_install_failed)
        val content = installer.error?.message ?: getString(R.string.installer_install_failed)
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForFailed: Title=$title, Content=$content, Error=${installer.error?.javaClass?.simpleName}")
        
        // 创建按钮参数列表
        val actions = mutableListOf<Map<String, Any>>()
        actions.add(MiuiIslandHelper.createCancelActionParams())
        actions.add(MiuiIslandHelper.createRetryActionParams())
        Timber.d("[id=${installer.id}] buildIslandNotificationForFailed: Added Retry and Cancel actions")
        
        val params = MiuiIslandHelper.generateIslandParams(title, content, 0, actions)
        Timber.d("[id=${installer.id}] buildIslandNotificationForFailed: Generated big island params")
        
        // 在builder阶段设置所有参数
        extras.putString("miui.focus.param", params)
        
        // 创建操作按钮
        val actionsBundle = android.os.Bundle()
        // 将finishIntent包装在Notification.Action对象中
        val finishAction = Notification.Action.Builder(
            R.drawable.ic_action_cancel, // 使用MIUI风格图标
            getString(R.string.finish),
            finishIntent
        ).build()
        actionsBundle.putParcelable("miui.focus.action_finish", finishAction)
        // 将installIntent包装在Notification.Action对象中
        val retryAction = Notification.Action.Builder(
            R.drawable.ic_action_install, // 使用MIUI风格安装图标作为重试按钮
            getString(R.string.retry),
            installIntent
        ).build()
        actionsBundle.putParcelable("miui.focus.action_retry", retryAction)
        
        extras.putBundle("miui.focus.actions", actionsBundle)
        
        val notification = builder.setContentTitle(title)
            .setContentText(content)
            .setOngoing(false)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForFailed: Failed notification created with actions")
        return notification
    }
    
    /**
     * 构建Ready状态的超级岛通知
     */
    private fun buildIslandNotificationForReady(builder: NotificationCompat.Builder, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForReady: Building notification for ready state")
        
        val title = getString(R.string.installer_ready)
        
        // Ready状态：显示取消按钮
        val actions = listOf(MiuiIslandHelper.createCancelActionParams())
        // 使用统一的超级岛参数生成方法（始终包含大岛和小岛配置）
        val params = MiuiIslandHelper.generateIslandParams(title, "", 0, actions)
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForReady: Title=$title, Added Cancel action")
        
        // 在builder阶段设置所有参数
        extras.putString("miui.focus.param", params)
        
        // 创建操作按钮
        val actionsBundle = Bundle()
        // 将finishIntent包装在Notification.Action对象中
        val finishAction = Notification.Action.Builder(
            R.drawable.ic_action_cancel, // 使用MIUI风格图标
            getString(R.string.cancel),
            finishIntent
        ).build()
        actionsBundle.putParcelable("miui.focus.action_finish", finishAction)
        
        extras.putBundle("miui.focus.actions", actionsBundle)
        
        val notification = builder.setContentTitle(title)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForReady: Ready notification created with action")
        return notification
    }
    
    /**
     * 构建解析失败状态的超级岛通知
     */
    private fun buildIslandNotificationForResolvedFailed(builder: NotificationCompat.Builder, extras: Bundle): Notification {
        Timber.d("[id=${installer.id}] buildIslandNotificationForResolvedFailed: Building notification for resolved failed state")
        
        val title = getString(R.string.installer_resolve_failed)
        
        // 解析失败状态：显示取消按钮
        val actions = listOf(MiuiIslandHelper.createCancelActionParams())
        val params = MiuiIslandHelper.generateIslandParams(title, "", 0, actions)
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForResolvedFailed: Title=$title, Added Cancel action")

        
        // 创建操作按钮
        val actionsBundle = Bundle()
        // 将finishIntent包装在Notification.Action对象中
        val finishAction = Notification.Action.Builder(
            R.drawable.ic_action_cancel, // 使用MIUI风格图标
            getString(R.string.cancel),
            finishIntent
        ).build()
        actionsBundle.putParcelable("miui.focus.action_finish", finishAction)
        

        extras.putBundle("miui.focus.actions", actionsBundle)
        extras.putString("miui.focus.param", params)
        val notification = builder.setContentTitle(title)
            .setExtras(extras)
            .build()
        
        Timber.d("[id=${installer.id}] buildIslandNotificationForResolvedFailed: Resolved failed notification created with action")
        return notification
    }

    
    /**
     * 获取待安装应用的图标
     * @param preferSystemIcon 是否优先使用系统图标
     * @return 应用图标Drawable或null
     */
    private suspend fun getInstallAppIcon(preferSystemIcon: Boolean): android.graphics.drawable.Drawable? {
        Timber.d("[id=${installer.id}] getInstallAppIcon: 尝试获取待安装应用图标")
        
        // 获取选中的应用实体
        val selectedEntities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .distinctBy { it.packageName }
        
        if (selectedEntities.isEmpty()) {
            Timber.d("[id=${installer.id}] getInstallAppIcon: 没有选中的应用实体")
            return null
        }
        
        // 优先获取第一个应用的图标
        val entityToInstall = selectedEntities.first()
        val packageName = entityToInstall.packageName
        
        try {
            // 使用AppIconRepo获取图标
            val icon = appIconRepo.getIcon(
                sessionId = installer.id,
                packageName = packageName,
                entityToInstall = entityToInstall,
                iconSizePx = 256, // 小米超级岛通知适合的图标大小
                preferSystemIcon = preferSystemIcon
            )
            
            if (icon != null) {
                Timber.d("[id=${installer.id}] getInstallAppIcon: 成功获取应用 $packageName 的图标")
            } else {
                Timber.d("[id=${installer.id}] getInstallAppIcon: 无法获取应用 $packageName 的图标")
            }
            
            return icon
        } catch (e: Exception) {
            Timber.e(e, "[id=${installer.id}] getInstallAppIcon: 获取应用 $packageName 图标时出现异常")
            return null
        }
    }
    
    // A data class to hold information about each installation stage.
    private data class InstallStageInfo(
        val progressClass: KClass<out ProgressEntity>,
        val weight: Float // Represents the relative length of this stage's segment.
    )

    private val installStages = listOf(
        InstallStageInfo(ProgressEntity.InstallResolving::class, 1f),    // Short
        InstallStageInfo(ProgressEntity.InstallPreparing::class, 4f),    // Long, with sub-progress
        InstallStageInfo(ProgressEntity.InstallAnalysing::class, 1f),     // Short
        InstallStageInfo(ProgressEntity.Installing::class, 4f)         // Long
    )

    private val totalProgressWeight = installStages.sumOf { it.weight.toDouble() }.toFloat()

    /**
     * Builds and returns a Notification for Android 15+ devices.
     * This is the entry point for the modern notification logic.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun buildModernNotification(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false
    ): Notification? {
        if (progress is ProgressEntity.Finish || progress is ProgressEntity.Error || progress is ProgressEntity.InstallAnalysedUnsupported) {
            return null
        }
        val builder = newModernNotificationBuilder(progress, showDialog, preferSystemIcon)

        // Finalize notification content for terminal states, which might require async operations.
        return when (progress) {
            is ProgressEntity.InstallFailed -> onInstallFailed(builder).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder).build()
            else -> builder.build()
        }
    }

    /**
     * Creates and configures a NotificationCompat.Builder using rich ProgressStyle.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun newModernNotificationBuilder(
        progress: ProgressEntity,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false
    ): NotificationCompat.Builder {
        val channel = NotificationChannelCompat.Builder(
            Channel.InstallerLiveChannel.value,
            NotificationManagerCompat.IMPORTANCE_MAX
        ).setName(getString(R.string.installer_live_channel_name)).build()
        notificationManager.createNotificationChannel(channel)
        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification,
            ConfigEntity.InstallMode.AutoNotification -> if (showDialog) openIntent else null

            else -> openIntent
        }
        val isDarkTheme = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val brandColor = if (isDarkTheme) primaryDark else primaryLight
        val baseBuilder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(Icon.LOGO.resId)
            .setColor(brandColor.toArgb())
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)

        // --- New Weighted & Dynamic Progress Logic ---
        // Find the current stage's information.
        val currentStageIndex = installStages.indexOfFirst { it.progressClass.isInstance(progress) }
        // Create the raw, uncolored segments with correct lengths.
        val segments = createInstallSegments(installStages)
        // Create the ProgressStyle.
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(segments)
            .setStyledByProgress(true)
        // Calculate the precise progress value.
        var contentTitle: String
        var shortText: String? = null
        // Calculate the total weight of all stages *before* the current one.
        val previousStagesWeight = if (currentStageIndex > 0) {
            installStages.subList(0, currentStageIndex).sumOf { it.weight.toDouble() }.toFloat()
        } else {
            0f
        }

        when (progress) {
            is ProgressEntity.InstallResolving, is ProgressEntity.InstallResolveSuccess -> {
                contentTitle = getString(R.string.installer_resolving)
                shortText = getString(R.string.installer_live_channel_short_text_resolving)
                // Progress is half of the current (first) segment's weight.
                val progressValue = previousStagesWeight + (installStages[0].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallPreparing -> {
                contentTitle = getString(R.string.installer_preparing)
                shortText = getString(R.string.installer_live_channel_short_text_preparing)
                // Real-time progress within this segment.
                val progressInCurrentSegment = installStages[1].weight * progress.progress
                val progressValue = previousStagesWeight + progressInCurrentSegment
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallResolvedFailed -> {
                contentTitle = getString(R.string.installer_resolve_failed)
                shortText = getString(R.string.installer_live_channel_short_text_resolve_failed)

                baseBuilder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false)
                    .addAction(0, getString(R.string.cancel), finishIntent)
            }

            is ProgressEntity.InstallAnalysing -> {
                contentTitle = getString(R.string.installer_analysing)
                shortText = getString(R.string.installer_live_channel_short_text_analysing)
                // Progress is the sum of previous segments + half of the current one.
                val progressValue = previousStagesWeight + (installStages[2].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
            }

            is ProgressEntity.InstallAnalysedSuccess -> {
                // Get all entities from analysis result, not just selected ones, for the check.
                val allEntities = installer.analysisResults.flatMap { it.appEntities }
                contentTitle = allEntities.map { it.app }.getInfo(context).title
                shortText = getString(R.string.installer_live_channel_short_text_pending)

                // Progress is now at the end of the Analysing stage.
                val progressValue = installStages.subList(0, 3).sumOf { it.weight.toDouble() }.toFloat()
                progressStyle.setProgress(progressValue.toInt())

                // Check for the specific Mixed Module/APK case first, just like in the legacy notification.
                val isMixedType = allEntities.any {
                    it.app.containerType == DataType.MIXED_MODULE_APK
                }

                if (isMixedType) {
                    // For mixed types, we must prompt the user to open the app for selection.
                    // We only provide a "Cancel" action, not an "Install" action.
                    baseBuilder.setContentText(getString(R.string.installer_mixed_module_apk_description_notification))
                        .addAction(0, getString(R.string.cancel), finishIntent)
                } else {
                    // This is the original logic for standard installs.
                    baseBuilder.setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                        .addAction(0, getString(R.string.install), installIntent)
                        .addAction(0, getString(R.string.cancel), finishIntent)
                }
                baseBuilder.setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            is ProgressEntity.Installing -> {
                contentTitle = installer.analysisResults.flatMap { it.appEntities }
                    .filter { it.selected }.map { it.app }.getInfo(context).title
                shortText = getString(R.string.installer_live_channel_short_text_installing)
                // Progress is the sum of previous segments + half of the current one.
                val progressValue = previousStagesWeight + (installStages[3].weight / 2f)
                progressStyle.setProgress(progressValue.toInt())
                baseBuilder.setContentText(getString(R.string.installer_installing))
            }

            is ProgressEntity.InstallSuccess -> {
                contentTitle = getString(R.string.installer_install_success)
                shortText = getString(R.string.installer_live_channel_short_text_success)
                progressStyle.setProgress(totalProgressWeight.toInt())
                baseBuilder.setOnlyAlertOnce(false).setLargeIcon(getLargeIconBitmap(preferSystemIcon))
            }

            is ProgressEntity.InstallFailed -> {
                contentTitle = getString(R.string.installer_install_failed)
                shortText = getString(R.string.installer_live_channel_short_text_install_failed)
                progressStyle.setProgress(previousStagesWeight.toInt())
                baseBuilder.setContentText(installer.error.getErrorMessage(context)).setOnlyAlertOnce(false)
            }

            else -> {
                contentTitle = getString(R.string.installer_ready)
                progressStyle.setProgress(0)
            }
        }

        baseBuilder.setContentTitle(contentTitle)
        shortText?.let { baseBuilder.setShortCriticalText(it) }
        baseBuilder.setStyle(progressStyle)

        return baseBuilder
    }

    /**
     * Creates a list of styled segments for the notification progress bar based on the current install stage.
     * @param stages The ordered list of installation stages.
     * @return A list of NotificationCompat.ProgressStyle.Segment objects.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createInstallSegments(
        stages: List<InstallStageInfo>
    ): List<NotificationCompat.ProgressStyle.Segment> {
        return stages.map { stageInfo ->
            // The length of each segment is now its defined weight.
            NotificationCompat.ProgressStyle.Segment(stageInfo.weight.toInt())
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun setNotification(notification: Notification? = null) {
        if (notification == null) {
            Timber.d("[id=${installer.id}] setNotification: Cancelling notification with id: $notificationId")
            notificationManager.cancel(notificationId)
            return
        }
        
        // 检查是否包含超级岛参数
        val isMiuiIslandNotification = notification.extras?.containsKey("miui.focus.param") ?: false
        val islandParams = if (isMiuiIslandNotification) "yes" else "no"
        
        Timber.d("[id=${installer.id}] setNotification: Posting/Updating notification with id: $notificationId")
        Timber.d("[id=${installer.id}] setNotification: Is MIUI Island notification: $islandParams")
        
        // 记录通知的标题和内容（如果可用）
        val title = notification.extras?.getString(Notification.EXTRA_TITLE) ?: "N/A"
        val content = notification.extras?.getString(Notification.EXTRA_TEXT) ?: "N/A"
        Timber.d("[id=${installer.id}] setNotification: Notification title: $title, content: $content")
        
        notificationManager.notify(notificationId, notification)
    }

    enum class Channel(val value: String) {
        InstallerChannel("installer_channel"),
        InstallerProgressChannel("installer_progress_channel"),
        InstallerLiveChannel("installer_live_channel")
    }

    enum class Icon(@param:DrawableRes val resId: Int) {
        LOGO(R.drawable.ic_notification_logo),
        Working(R.drawable.round_hourglass_empty_black_24),
        Pausing(R.drawable.round_hourglass_disabled_black_24)
    }

    private val notificationChannels = mapOf(
        Channel.InstallerChannel to NotificationChannelCompat.Builder(
            Channel.InstallerChannel.value,
            NotificationManagerCompat.IMPORTANCE_HIGH // Use HIGH for final pop-up
        ).setName(getString(R.string.installer_channel_name)).build(),

        Channel.InstallerProgressChannel to NotificationChannelCompat.Builder(
            Channel.InstallerProgressChannel.value,
            NotificationManagerCompat.IMPORTANCE_LOW // Use LOW to be less intrusive
        ).setName(getString(R.string.installer_progress_channel_name)).build(),

        Channel.InstallerLiveChannel to NotificationChannelCompat.Builder(
            Channel.InstallerLiveChannel.value, NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName(getString(R.string.installer_live_channel_name)).build()
    )

    private val workingProgresses = listOf(
        ProgressEntity.Ready,
        ProgressEntity.InstallResolving,
        ProgressEntity.InstallResolveSuccess,
        ProgressEntity.InstallAnalysing,
        ProgressEntity.InstallAnalysedSuccess,
        ProgressEntity.Installing,
        ProgressEntity.InstallSuccess
    )

    private val importanceProgresses = listOf(
        ProgressEntity.InstallResolvedFailed,
        ProgressEntity.InstallAnalysedFailed,
        ProgressEntity.InstallAnalysedSuccess,
        ProgressEntity.InstallFailed,
        ProgressEntity.InstallSuccess
    )

    private val installProgresses = mapOf(
        ProgressEntity.InstallResolving to 0,
        ProgressEntity.InstallAnalysing to 40,
        ProgressEntity.Installing to 80
    )

    /**
     * Gets the large icon for notifications as a Bitmap by fetching it from the AppIconRepository.
     */
    private suspend fun getLargeIconBitmap(preferSystemIcon: Boolean): Bitmap? {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val entityToInstall = entities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
            ?: entities.sortedBest().firstOrNull()
            ?: return null // Return null if no suitable entity is found

        // Use standard notification large icon dimensions
        val iconSizePx =
            context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

        // Get the drawable from the central repository
        val drawable = appIconRepo.getIcon(
            sessionId = installer.id,
            packageName = entityToInstall.packageName,
            entityToInstall = entityToInstall,
            iconSizePx = iconSizePx,
            preferSystemIcon = preferSystemIcon
        )

        // Convert the drawable to a bitmap of the correct size
        return drawable?.toBitmapOrNull(width = iconSizePx, height = iconSizePx)
    }

    /**
     * (Renamed from newNotification)
     * Builds and returns a Notification for legacy Android versions.
     */
    private suspend fun buildLegacyNotification(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean,
        preferSystemIcon: Boolean = false
    ): Notification? {
        val builder = newLegacyNotificationBuilder(progress, background, showDialog)
        return when (progress) {
            is ProgressEntity.Ready -> onReady(builder)
            is ProgressEntity.InstallResolving -> onResolving(builder)
            is ProgressEntity.InstallResolvedFailed -> onResolvedFailed(builder)
            is ProgressEntity.InstallResolveSuccess -> onResolveSuccess(builder)
            is ProgressEntity.InstallPreparing -> onPreparing(builder, progress)
            is ProgressEntity.InstallAnalysing -> onAnalysing(builder)
            is ProgressEntity.InstallAnalysedFailed -> onAnalysedFailed(builder)
            is ProgressEntity.InstallAnalysedSuccess -> onAnalysedSuccess(builder, preferSystemIcon)
            is ProgressEntity.Installing -> onInstalling(builder, preferSystemIcon)
            is ProgressEntity.InstallFailed -> onInstallFailed(builder).build()
            is ProgressEntity.InstallSuccess -> onInstallSuccess(builder).build()
            is ProgressEntity.Finish, is ProgressEntity.Error, is ProgressEntity.InstallAnalysedUnsupported -> null
            else -> null // TODO temporarily disable uninstall notification
        }
    }

    /**
     * (Renamed from newNotificationBuilder)
     * Creates and configures a standard NotificationCompat.Builder for legacy Android versions.
     */
    private fun newLegacyNotificationBuilder(
        progress: ProgressEntity,
        background: Boolean,
        showDialog: Boolean
    ): NotificationCompat.Builder {
        val isWorking = workingProgresses.contains(progress)
        val isImportance = importanceProgresses.contains(progress)

        // 1. Determine the correct enum constant directly. This is safe and reliable.
        val channelEnum = if (isImportance && background) {
            Channel.InstallerChannel
        } else {
            Channel.InstallerProgressChannel
        }

        // 2. Get the actual NotificationChannelCompat object from the map using the enum as the key.
        val channel = notificationChannels[channelEnum]!!
        notificationManager.createNotificationChannel(channel)

        val icon = (if (isWorking) Icon.Working else Icon.Pausing).resId

        val contentIntent = when (installer.config.installMode) {
            ConfigEntity.InstallMode.Notification, ConfigEntity.InstallMode.AutoNotification -> {
                if (showDialog) openIntent else null
            }

            else -> openIntent
        }

        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setDeleteIntent(finishIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (progress is ProgressEntity.InstallSuccess || progress is ProgressEntity.InstallFailed) {
            builder.setOngoing(false)
                .setOnlyAlertOnce(false)
        }

        installProgresses[progress]?.let {
            builder.setProgress(100, it, false)
        }

        return builder
    }


    private fun getString(@StringRes resId: Int): String = context.getString(resId)
    private val openIntent = BroadcastHandler.openIntent(context, installer)

    private val analyseIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Analyse)

    private val installIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Install)

    private val finishIntent =
        BroadcastHandler.namedIntent(context, installer, BroadcastHandler.Name.Finish)

    private fun onReady(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_ready))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onResolving(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolving)).build()

    private fun onPreparing(
        builder: NotificationCompat.Builder,
        progress: ProgressEntity.InstallPreparing
    ) =
        builder.setContentTitle(getString(R.string.installer_prepare_install))
            .setProgress(100, (progress.progress * 100).toInt(), progress.progress < 0)
            .build()

    private fun onResolvedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolve_failed))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onResolveSuccess(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_resolve_success))
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private fun onAnalysing(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_analysing)).build()

    private fun onAnalysedFailed(builder: NotificationCompat.Builder) =
        builder.setContentTitle(getString(R.string.installer_analyse_failed))
            .addAction(0, getString(R.string.retry), analyseIntent)
            .addAction(0, getString(R.string.cancel), finishIntent).build()

    private suspend fun onAnalysedSuccess(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): Notification {
        val selectedEntities = installer.analysisResults
            .flatMap { it.appEntities }
        // In notification mode, nothing is pre-selected, so we consider all entities.
        // .filter { it.selected }
        val selectedApps = selectedEntities.map { it.app }
        val info = selectedApps.getInfo(context)

        // Check for the specific Mixed Module/APK case first. This is the highest priority.
        val isMixedType = selectedEntities.any {
            it.app.containerType == DataType.MIXED_MODULE_APK
        }

        if (isMixedType) {
            // If it's a mixed type, always show the specific description and install options.
            return builder.setContentTitle(info.title)
                .setContentText(getString(R.string.installer_mixed_module_apk_description_notification))
                .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                .addAction(0, getString(R.string.cancel), finishIntent)
                .build()
        }

        // Fallback to the original logic for standard single or multi-app installs.
        return (if (selectedApps.groupBy { it.packageName }.size != 1) {
            // This is for true multi-APK installs (e.g., sharing 3 different APKs).
            builder.setContentTitle(getString(R.string.installer_prepare_install))
                .addAction(0, getString(R.string.cancel), finishIntent)
        } else {
            // This is for a standard single APK install.
            builder.setContentTitle(info.title)
                .setContentText(getString(R.string.installer_prepare_type_unknown_confirm))
                .setLargeIcon(getLargeIconBitmap(preferSystemIcon))
                .addAction(0, getString(R.string.install), installIntent)
                .addAction(0, getString(R.string.cancel), finishIntent)
        }).build()
    }

    private suspend fun onInstalling(builder: NotificationCompat.Builder, preferSystemIcon: Boolean): Notification {
        val info = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .getInfo(context)
        return builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_installing))
            .setLargeIcon(getLargeIconBitmap(preferSystemIcon)).build()
    }

    private fun onInstallFailed(
        builder: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        val info = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
            .getInfo(context)
        val reason = installer.error.getErrorMessage(context)
        val contentText = getString(R.string.installer_install_failed)
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(info.title)
            .bigText(
                "$contentText\n" + reason
            )
        return builder.setContentTitle(info.title) // Return builder directly
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .addAction(0, getString(R.string.retry), installIntent)
            .addAction(0, getString(R.string.cancel), finishIntent)
    }

    private fun onInstallSuccess(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        val entities = installer.analysisResults
            .flatMap { it.appEntities }
            .filter { it.selected }
            .map { it.app }
        val info = entities.getInfo(context)
        var openPendingIntent: PendingIntent? = null

        val launchIntent = entities.firstOrNull()?.packageName?.let {
            context.packageManager.getLaunchIntentForPackage(it)
        }
        if (launchIntent != null) {
            val supportsPrivileged = installer.config.authorizer in listOf(
                ConfigEntity.Authorizer.Root,
                ConfigEntity.Authorizer.Shizuku,
                ConfigEntity.Authorizer.Customize
            )

            openPendingIntent = if (supportsPrivileged) {
                BroadcastHandler.privilegedLaunchAndFinishIntent(context, installer)
            } else {
                BroadcastHandler.launchIntent(context, installer, launchIntent)
            }
        }

        var newBuilder = builder.setContentTitle(info.title)
            .setContentText(getString(R.string.installer_install_success))
        if (openPendingIntent != null)
            newBuilder = newBuilder.addAction(0, getString(R.string.open), openPendingIntent)

        return newBuilder.addAction(0, getString(R.string.finish), finishIntent)
    }
}