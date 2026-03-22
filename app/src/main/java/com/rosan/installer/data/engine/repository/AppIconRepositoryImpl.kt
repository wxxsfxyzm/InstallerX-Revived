// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.repository.AppIconRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import android.os.Process as AndroidProcess

/**
 * The default implementation of [AppIconRepository].
 * It handles the logic for loading icons from the installed package (for upgrades)
 * or from the APK entity (for new installs) and caches the results.
 */
class AppIconRepositoryImpl(
    private val context: Context
) : AppIconRepository {
    private val pm: PackageManager by lazy { context.packageManager }

    /**
     * Efficiently identifies the User ID of the current process using the standard UID range formula.
     */
    private val currentUserId: Int by lazy {
        AndroidProcess.myUid() / 100000
    }

    // Cache to store loading operations returning Bitmap
    private val iconCache = ConcurrentHashMap<String, Deferred<Bitmap?>>()

    override suspend fun getIcon(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity?,
        userId: Int, // Explicitly passed (e.g., from Installer session)
        iconSizePx: Int,
        preferSystemIcon: Boolean
    ): Bitmap? = coroutineScope {
        // Cache key now includes userId to prevent cross-user leakage
        val cacheKey = "$sessionId-$packageName-$userId-$iconSizePx"

        val deferred = iconCache.getOrPut(cacheKey) {
            async(Dispatchers.IO) {
                val baseEntity = entityToInstall as? AppEntity.BaseEntity
                val rawApkIconDrawable = baseEntity?.icon
                val apkPath = (baseEntity?.data as? DataEntity.FileEntity)?.path

                val installedAppInfo = try {
                    context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }

                if (preferSystemIcon) {
                    if (installedAppInfo != null) {
                        return@async AppIconCache.loadIconBitmap(context, installedAppInfo, userId, iconSizePx)
                    }

                    if (apkPath != null) {
                        // Pass userId to the system loader helper
                        val themedIconFromApk = loadIconFromApkWithSystemLoader(apkPath, userId, iconSizePx)
                        if (themedIconFromApk != null) {
                            return@async themedIconFromApk
                        }
                    }

                    rawApkIconDrawable?.toBitmap()
                } else {
                    rawApkIconDrawable?.toBitmap() ?: if (installedAppInfo != null) {
                        AppIconCache.loadIconBitmap(context, installedAppInfo, userId, iconSizePx)
                    } else {
                        null
                    }
                }
            }
        }
        try {
            deferred.await() ?: getFallbackSystemIcon(context)
        } catch (_: Exception) {
            iconCache.remove(cacheKey, deferred)
            getFallbackSystemIcon(context)
        }
    }

    override suspend fun extractColorFromApp(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity.BaseEntity?,
        preferSystemIcon: Boolean
    ): Int? {
        // Uses the current process userId for color extraction as it's typically for the local UI context
        val iconBitmap = getIcon(sessionId, packageName, entityToInstall, currentUserId, 256, preferSystemIcon)
        return iconBitmap?.extractSeedColor()
    }

    override suspend fun extractColorFromDrawable(drawable: Drawable?): Int? {
        if (drawable == null) return null
        return try {
            val bitmap = drawable.toBitmap()
            bitmap.extractSeedColor()
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract color from drawable")
            null
        }
    }

    override fun clearCacheForSession(sessionId: String) {
        val keysToRemove = iconCache.keys.filter { it.startsWith("$sessionId-") }
        keysToRemove.forEach { key ->
            iconCache.remove(key)?.cancel()
        }
    }

    override fun clearCacheForPackage(packageName: String) {
        // Filters across all sessions and users for this specific package
        val keysToRemove = iconCache.keys.filter { it.contains("-$packageName-") || it.endsWith("-$packageName") }
        keysToRemove.forEach { key ->
            iconCache.remove(key)?.cancel()
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && this.bitmap != null) return this.bitmap
        val bitmap = createBitmap(if (intrinsicWidth > 0) intrinsicWidth else 1, if (intrinsicHeight > 0) intrinsicHeight else 1)
        val canvas = Canvas(bitmap)
        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)
        return bitmap
    }

    private suspend fun Bitmap.extractSeedColor(
        maxColors: Int = 128,
        fallbackColorArgb: Int = -12417548
    ): Int = withContext(Dispatchers.Default) {
        val width = this@extractSeedColor.width
        val height = this@extractSeedColor.height
        val pixels = IntArray(width * height)
        this@extractSeedColor.getPixels(pixels, 0, width, 0, 0, width, height)

        val colorToCountMap: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)
        val sortedColors: List<Int> = Score.score(colorToCountMap, 1, fallbackColorArgb, true)

        sortedColors.first()
    }

    /**
     * Loads an icon from an APK file using the system's resource loader to apply themes.
     */
    private suspend fun loadIconFromApkWithSystemLoader(apkPath: String, userId: Int, iconSizePx: Int): Bitmap? {
        return try {
            val packageInfo = pm.getPackageArchiveInfo(apkPath, 0)
            packageInfo?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath
                // Delegate to AppIconCache with the specified userId
                AppIconCache.loadIconBitmap(context, appInfo, userId, iconSizePx)
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not load themed icon from APK at path: $apkPath")
            null
        }
    }

    private fun getFallbackSystemIcon(context: Context): Bitmap? {
        return ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
            ?.toBitmap()
    }
}
