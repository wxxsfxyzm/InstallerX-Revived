package com.rosan.installer.data.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * 提供应用图标异步加载和缓存功能。
 *
 * 该工具类包含一个内存 LRU 缓存，以避免重复的磁盘读取，并使用一个专用的后台线程池进行图标加载，以保持主线程的流畅。
 *
 * 核心的缓存与加载逻辑改编自 RikkaApps 的 Shizuku 项目中的 `AppIconCache.kt` 文件。
 *
 * 代码受 Apache License 2.0 许可协议保护。
 * @see <a href="https://github.com/RikkaApps/Shizuku/blob/master/LICENSE">Apache License 2.0</a>
 * @see <a href="https://github.com/RikkaApps/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/utils/AppIconCache.kt">RikkaApps/Shizuku - AppIconCache.kt</a>
 */
object AppIconCache : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private class AppIconLruCache(maxSize: Int) :
        LruCache<Triple<String, Int, Int>, Bitmap>(maxSize) {
        override fun sizeOf(key: Triple<String, Int, Int>, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val lruCache: LruCache<Triple<String, Int, Int>, Bitmap>
    private val dispatcher: CoroutineDispatcher
    private var appIconLoaders = mutableMapOf<Int, AppIconLoader>()

    init {
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)

        val threadCount = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        val loadIconExecutor = Executors.newFixedThreadPool(threadCount)
        dispatcher = loadIconExecutor.asCoroutineDispatcher()
    }

    private fun get(packageName: String, userId: Int, size: Int): Bitmap? =
        lruCache[Triple(packageName, userId, size)]

    private fun put(packageName: String, userId: Int, size: Int, bitmap: Bitmap) {
        if (get(packageName, userId, size) == null) {
            lruCache.put(Triple(packageName, userId, size), bitmap)
        }
    }

    private fun getOrLoadBitmap(
        context: Context,
        info: ApplicationInfo,
        userId: Int,
        size: Int
    ): Bitmap {
        get(info.packageName, userId, size)?.let { return it }

        val loader = appIconLoaders.getOrPut(size) {
            val shrink =
                context.applicationInfo.loadIcon(context.packageManager) is AdaptiveIconDrawable
            AppIconLoader(size, shrink, context)
        }

        val bitmap = loader.loadIcon(info, false)
        put(info.packageName, userId, size, bitmap)
        return bitmap
    }

    /**
     * Loads the application icon as a Drawable using a background thread and caching.
     *
     * @param context The context.
     * @param info The ApplicationInfo for the target app.
     * @param size The desired icon size in pixels.
     * @return A Drawable of the app icon, or null if loading fails.
     */
    suspend fun loadIconDrawable(context: Context, info: ApplicationInfo, size: Int): Drawable? {
        return try {
            val bitmap = withContext(dispatcher) {
                // Assuming primary user (ID 0)
                getOrLoadBitmap(context, info, 0, size)
            }
            bitmap.toDrawable(context.resources)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}