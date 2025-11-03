package com.rosan.installer.data.installer.util

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.rosan.installer.R
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.lang.reflect.Method

/**
 * 小米超级岛通知助手类
 * 用于检测设备支持情况并生成超级岛通知所需的参数
 */
object MiuiIslandHelper {
    private const val TAG = "MiuiIslandHelper"
    
    /**
     * 检测设备是否支持小米超级岛通知
     * 使用官方推荐的方法检查notification_focus_protocol设置项
     * @param context 上下文对象
     * @return 设备是否支持小米超级岛通知
     */
    fun isMiuiIslandSupported(context: Context): Boolean {
        Timber.d("小米超级岛支持检测: 开始检查设备是否支持小米超级岛通知")
        try {
            // 首先检查是否为MIUI系统
            val miuiVersionName = getSystemProperty("ro.miui.ui.version.name", "")
            Timber.d("  - MIUI版本名称: '$miuiVersionName'")
            
            if (miuiVersionName.isNotEmpty()) {
                // 官方推荐方法：检查notification_focus_protocol设置项
                // 返回值含义:
                //   1: OS1版本
                //   2: OS2版本
                //   3: OS3版本，支持小米超级岛通知模板
                val focusProtocolVersion = Settings.System.getInt(
                    context.contentResolver,
                    "notification_focus_protocol", 0
                )
                
                Timber.d("  - 焦点协议版本: $focusProtocolVersion")
                
                // 只有OS3版本(返回3)才支持超级岛通知
                val isSupportedByProtocol = focusProtocolVersion == 3
                
                // 同时获取其他信息用于调试
                val androidVersion = Build.VERSION.SDK_INT
                val androidVersionName = Build.VERSION.RELEASE
                val miuiVersionCode = miuiVersionName.toIntOrNull() ?: 0
                
                Timber.d("  - Android版本: $androidVersionName (SDK $androidVersion)")
                Timber.d("  - MIUI版本号: $miuiVersionCode")
                Timber.d("  - 支持状态(通过协议): $isSupportedByProtocol")
                
                return isSupportedByProtocol
            } else {
                Timber.d("  - 非MIUI系统")
            }
        } catch (e: Exception) {
            Timber.e(e, "  - 检查小米超级岛支持时出错")
        }
        return false
    }
    
    /**
     * 获取系统属性
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 系统属性值或默认值
     */
    private fun getSystemProperty(key: String, defaultValue: String): String {
        Timber.d("获取系统属性: key='$key', defaultValue='$defaultValue'")
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val get: Method = clazz.getMethod("get", String::class.java, String::class.java)
            val result = get.invoke(null, key, defaultValue) as String
            Timber.d("  - 获取结果: '$result'")
            result
        } catch (e: Exception) {
            Timber.w(e, "  - 获取系统属性失败，返回默认值")
            defaultValue
        }
    }
    

    /**
     * 生成完整的超级岛参数（同时包含大岛和小岛配置）
     * @param title 标题
     * @param content 内容（可为空字符串）
     * @param progressValue 进度值(0-100)
     * @param actions 操作按钮列表（可为空列表）
     * @return 格式化的JSON字符串参数
     */
    fun generateIslandParams(
        title: String,
        content: String = "",
        progressValue: Int,
        actions: List<Map<String, Any>> = emptyList()
    ): String {
        Timber.d("生成超级岛参数:")
        Timber.d("  - 标题: '$title'")
        Timber.d("  - 内容: '$content'")
        Timber.d("  - 进度: $progressValue")
        Timber.d("  - 操作按钮数量: ${actions.size}")
        
        // 按照官方模板创建完整的参数结构
        val rootObject = JSONObject()
        val paramV2 = JSONObject()
        
        // 基础参数
        paramV2.put("protocol", 1)
        paramV2.put("business", "install")
        paramV2.put("enableFloat", true)
        paramV2.put("updatable", true)
        paramV2.put("timeout", 15)
        paramV2.put("ticker", title)
        paramV2.put("tickerPic", "miui.focus.pic_ticker")
        paramV2.put("aodTitle", title)
        paramV2.put("aodPic", "miui.focus.pic_aod")
        
        // 岛属性参数
        val paramIsland = JSONObject()
        paramIsland.put("islandProperty", 1)
        paramIsland.put("islandTimeout", 60)

        // 大岛区域配置（始终包含）
        val bigIslandArea = JSONObject()
        
        // 左侧图文信息
        val imageTextInfoLeft = JSONObject()
        imageTextInfoLeft.put("type", 1)
        val picInfo = JSONObject()
        picInfo.put("type", 1)
        // 使用miui.focus.pic_start作为自定义图片引用
        picInfo.put("pic", "miui.focus.pic_appicon")
        imageTextInfoLeft.put("picInfo", picInfo)
        bigIslandArea.put("imageTextInfoLeft", imageTextInfoLeft)
        
        // 进度信息
        val progressTextInfo = JSONObject()
        val progressInfo = JSONObject()
        progressInfo.put("progress", progressValue)
        progressTextInfo.put("progressInfo", progressInfo)
        bigIslandArea.put("progressTextInfo", progressTextInfo)
        
        // 将大岛区域添加到岛属性
        paramIsland.put("bigIslandArea", bigIslandArea)
        
        // 小岛区域配置（所有情况下都需要包含）
        val smallIslandArea = JSONObject()
        val smallPicInfo = JSONObject()
        smallPicInfo.put("type", 1)
        // 使用miui.focus.pic_start作为自定义图片引用
        smallPicInfo.put("pic", "miui.focus.pic_appicon")
        smallIslandArea.put("picInfo", smallPicInfo)
        paramIsland.put("smallIslandArea", smallIslandArea)
        
        // 聊天信息配置
        val chatInfo = JSONObject()
        chatInfo.put("picProfile", "miui.focus.pic_appicon")
        chatInfo.put("title", title)
        chatInfo.put("content", content)
        chatInfo.put("colorTitle", "#000000")
        chatInfo.put("colorTitleDark", "#FFFFFF")
        chatInfo.put("colorContent", "#000000")
        chatInfo.put("colorContentDark", "#FFFFFF")
        
        // 生成actions数组
        val actionsArray = JSONArray()
        actions.forEachIndexed { index, action ->
            Timber.d("  - 操作按钮 $index: ${action["action"]} (${action["actionText"]}) ")
            val actionObj = JSONObject()
            action.forEach { (key, value) ->
                // 处理嵌套的map，将其转换为JSONObject而不是字符串
                if (value is Map<*, *>) {
                    val nestedObj = JSONObject()
                    value.forEach { (nestedKey, nestedValue) ->
                        nestedKey?.let { nestedObj.put(it.toString(), nestedValue) }
                    }
                    actionObj.put(key, nestedObj)
                } else {
                    actionObj.put(key, value)
                }
            }
            actionsArray.put(actionObj)
        }
        
        // 组装完整结构
        paramV2.put("param_island", paramIsland)
        paramV2.put("chatInfo", chatInfo)
        paramV2.put("actions", actionsArray)
        rootObject.put("param_v2", paramV2)
        
        val result = rootObject.toString()
        Timber.d("  - 超级岛JSON参数内容: $result")
        return result
    }
    
    // 操作按钮创建方法组
    
    /**
     * 创建取消操作按钮参数
     * @return 取消操作参数Map
     */
    fun createCancelActionParams(): Map<String, Any> {
        Timber.d("创建取消操作按钮参数")
        return mapOf(
            "type" to 0,
            "actionIcon" to "miui.focus.pic_action_cancel",
            "actionIconDark" to "miui.focus.pic_action_cancel_dark",
            "action" to "miui.focus.action_finish",
            "actionIntentType" to 2 // 2-action to broadcast
        )
    }
    
    /**
     * 创建安装操作按钮参数
     * @return 安装操作参数Map
     */
    fun createInstallActionParams(): Map<String, Any> {
        Timber.d("创建安装操作按钮参数")
        return mapOf(
            "type" to 0,
            "actionIcon" to "miui.focus.pic_action_install",
            "actionIconDark" to "miui.focus.pic_action_install_dark",
            "action" to "miui.focus.action_install",
            "actionIntentType" to 2 // 2-action to broadcast
        )
    }
    
    /**
     * 创建重试操作按钮参数
     * @return 重试操作参数Map
     */
    fun createRetryActionParams(): Map<String, Any> {
        Timber.d("创建重试操作按钮参数")
        return mapOf(
            "type" to 0,
            "actionIcon" to "miui.focus.pic_action_install",
            "actionIconDark" to "miui.focus.pic_action_install_dark",
            "action" to "miui.focus.action_retry",
            "actionIntentType" to 2 // 2-action to broadcast
        )
    }
    
    /**
     * 创建打开操作按钮参数
     * @return 打开操作参数Map
     */
    fun createOpenActionParams(): Map<String, Any> {
        Timber.d("创建打开操作按钮参数")
        return mapOf(
            "type" to 1,
            "actionIcon" to "miui.focus.pic_action_open",
            "actionIconDark" to "miui.focus.pic_action_open_dark",
            "action" to "miui.focus.action_open",
            "actionIntentType" to 1,
            "progressInfo" to mapOf(
                "progress" to 100,
                "colorProgressDark" to "#43b244",
                "colorProgressEndDark" to "#29FFFFFF",
                "colorProgress" to "#43b244",
                "colorProgressEnd" to "#1A000000"
            )
        )
    }
    
    /**
     * 创建进度操作参数
     * @return 进度操作参数Map
     */
    fun createProgressActionParams(progress: Int): Map<String, Any> {
        Timber.d("创建进度操作参数，进度值: $progress")
        return mapOf(
            "type" to 1,
            "actionIcon" to "miui.focus.pic_action_progress",
            "actionIconDark" to "miui.focus.pic_action_progress_dark",
            "action" to "miui.focus.action_progress",
            "actionIntentType" to 2,
            "progressInfo" to mapOf(
                "progress" to progress,
                "colorProgressDark" to "#43b244",
                "colorProgressEndDark" to "#29FFFFFF",
                "colorProgress" to "#43b244",
                "colorProgressEnd" to "#1A000000"
            )
        )
    }
    
    // 自定义图片相关方法
    
    /**
     * 创建包含自定义图片资源的Bundle
     * @param context 上下文对象
     * @param iconResId 图片资源ID（默认图标）
     * @param appIcon 待安装应用图标（可选）
     * @return 包含所有使用的图片资源的Bundle
     */
    fun createPicsBundle(context: Context, iconResId: Int, appIcon: android.graphics.drawable.Drawable? = null): Bundle {
        val picsBundle = Bundle()
        // 创建默认Icon对象
        val defaultIcon = Icon.createWithResource(context, iconResId)
        
        // 添加所有在超级岛参数中使用的图片资源
        // 基础图片资源
        picsBundle.putParcelable("miui.focus.pic_ticker", defaultIcon) // 通知栏图片
        picsBundle.putParcelable("miui.focus.pic_aod", defaultIcon)    // 息屏显示图片
        
        // 如果提供了应用图标，则使用它作为appicon，否则使用默认图标
        if (appIcon != null) {
            try {
                // 将Drawable转换为Icon，支持更多类型的Drawable
                val bitmap = try {
                    // 优先尝试直接获取Bitmap
                    if (appIcon is android.graphics.drawable.BitmapDrawable) {
                        appIcon.bitmap
                    } else if (appIcon is android.graphics.drawable.VectorDrawable) {
                        // 处理VectorDrawable
                        val bitmap = android.graphics.Bitmap.createBitmap(appIcon.intrinsicWidth, appIcon.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        appIcon.setBounds(0, 0, canvas.width, canvas.height)
                        appIcon.draw(canvas)
                        bitmap
                    } else {
                        // 通用处理：将任何Drawable绘制到Bitmap上
                        val width = appIcon.intrinsicWidth.coerceAtLeast(256)
                        val height = appIcon.intrinsicHeight.coerceAtLeast(256)
                        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        appIcon.setBounds(0, 0, width, height)
                        appIcon.draw(canvas)
                        bitmap
                    }
                } catch (e: Exception) {
                    Timber.e(e, "转换Drawable为Bitmap时出错")
                    null
                }
                
                if (bitmap != null) {
                    val appIconObj = Icon.createWithBitmap(bitmap)
                    picsBundle.putParcelable("miui.focus.pic_appicon", appIconObj)
                    Timber.d("使用待安装应用图标作为miui.focus.pic_appicon")
                } else {
                    picsBundle.putParcelable("miui.focus.pic_appicon", defaultIcon)
                    Timber.d("无法转换应用图标，使用默认图标作为miui.focus.pic_appicon")
                }
            } catch (e: Exception) {
                picsBundle.putParcelable("miui.focus.pic_appicon", defaultIcon)
                Timber.e(e, "处理应用图标时出错，使用默认图标")
            }
        } else {
            picsBundle.putParcelable("miui.focus.pic_appicon", defaultIcon)
            Timber.d("未提供应用图标，使用默认图标作为miui.focus.pic_appicon")
        }
        
        // 操作按钮图片资源
        picsBundle.putParcelable("miui.focus.pic_action_cancel", Icon.createWithResource(context, R.drawable.ic_action_cancel))    // 取消按钮
        picsBundle.putParcelable("miui.focus.pic_action_install", Icon.createWithResource(context, R.drawable.ic_action_install))   // 安装按钮
        picsBundle.putParcelable("miui.focus.pic_action_cancel_dark", Icon.createWithResource(context, R.drawable.ic_action_cancel_dark))    // 取消按钮
        picsBundle.putParcelable("miui.focus.pic_action_install_dark", Icon.createWithResource(context, R.drawable.ic_action_install_dark))   // 安装按钮
        picsBundle.putParcelable("miui.focus.pic_action_open", Icon.createWithResource(context, R.drawable.ic_action_open))      // 打开按钮
        picsBundle.putParcelable("miui.focus.pic_action_progress", Icon.createWithResource(context, R.drawable.ic_action_install))  // 进度按钮
        picsBundle.putParcelable("miui.focus.pic_action_open_dark", Icon.createWithResource(context, R.drawable.ic_action_open_dark))      // 打开按钮
        picsBundle.putParcelable("miui.focus.pic_action_progress_dark", Icon.createWithResource(context, R.drawable.ic_action_install_dark))  // 进度按钮
        Timber.d("创建图片资源Bundle: 添加了${picsBundle.keySet().size}个图片资源")
        return picsBundle
    }

}