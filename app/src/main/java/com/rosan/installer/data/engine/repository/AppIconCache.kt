// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors

// Portions of this file are derived from RikkaApps/Shizuku
// (https://github.com/RikkaApps/Shizuku)
// Copyright (C) RikkaApps contributors
// Licensed under Apache-2.0
package com.rosan.installer.data.engine.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader

/**
 * Provides asynchronous application icon loading and memory caching.
 *
 * This utility employs an LRU cache to prevent redundant disk I/O and utilizes a
 * dedicated background dispatcher to ensure the main thread remains responsive during icon decoding.
 */
object AppIconCache {
    private class AppIconLruCache(maxSize: Int) :
        LruCache<Triple<String, Int, Int>, Bitmap>(maxSize) {
        override fun sizeOf(key: Triple<String, Int, Int>, value: Bitmap): Int {
            // Memory size in KB
            return value.byteCount / 1024
        }
    }

    private val lruCache: LruCache<Triple<String, Int, Int>, Bitmap>

    /**
     * Dedicated dispatcher with limited parallelism to handle heavy icon loading tasks
     * without overwhelming the system resources.
     */
    private val dispatcher: CoroutineDispatcher

    private var appIconLoaders = mutableMapOf<Int, AppIconLoader>()

    init {
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)

        // Dynamically calculate thread count based on available processors
        val threadCount = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        dispatcher = Dispatchers.IO.limitedParallelism(threadCount)
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
            // Determine if the icon should be shrunk based on AdaptiveIcon compatibility
            val shrink = context.applicationInfo.loadIcon(context.packageManager) is AdaptiveIconDrawable
            AppIconLoader(size, shrink, context)
        }

        val bitmap = loader.loadIcon(info, false)
        put(info.packageName, userId, size, bitmap)
        return bitmap
    }

    /**
     * Loads the application icon as a [Bitmap] on a background thread.
     *
     * @param context The application context.
     * @param info The [ApplicationInfo] of the target application.
     * @param userId The ID of the user the application belongs to.
     * @param size The target icon size in pixels.
     */
    suspend fun loadIconBitmap(
        context: Context,
        info: ApplicationInfo,
        userId: Int,
        size: Int
    ): Bitmap? {
        return try {
            withContext(dispatcher) {
                getOrLoadBitmap(context, info, userId, size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
