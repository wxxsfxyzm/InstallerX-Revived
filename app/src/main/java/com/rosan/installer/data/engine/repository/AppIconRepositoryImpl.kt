// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * The default implementation of [com.rosan.installer.domain.engine.repository.AppIconRepository].
 * It handles the logic for loading icons from the installed package (for upgrades)
 * or from the APK entity (for new installs) and caches the results.
 */
class AppIconRepositoryImpl(
    private val context: Context
) : AppIconRepository, KoinComponent {
    private val pm: PackageManager by lazy { context.packageManager }

    // Cache to store loading operations (Deferred) to handle concurrent requests.
    private val iconCache = ConcurrentHashMap<String, Deferred<Drawable?>>()

    override suspend fun getIcon(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity?,
        iconSizePx: Int,
        preferSystemIcon: Boolean
    ): Drawable? = coroutineScope {
        val cacheKey = "$sessionId-$packageName"

        val deferred = iconCache.getOrPut(cacheKey) {
            async(Dispatchers.IO) {
                val baseEntity = entityToInstall as? AppEntity.BaseEntity
                val rawApkIcon = baseEntity?.icon
                // val apkPath = baseEntity?.data?.sourcePath()
                val apkPath = (baseEntity?.data as? DataEntity.FileEntity)?.path

                // Try to get ApplicationInfo from an already installed package.
                val installedAppInfo = try {
                    context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }

                if (preferSystemIcon) {
                    // --- User prefers system/themed icon ---
                    // Priority 1: If the app is installed (upgrade), load its icon directly.
                    // This correctly applies system theming and icon packs.
                    if (installedAppInfo != null) {
                        Timber.d("preferSystemIcon is true, upgrade, load from installedAppInfo")
                        return@async AppIconCache.loadIconDrawable(context, installedAppInfo, iconSizePx)
                    }

                    // Priority 2: If it's a new install, try to load the themed icon
                    // from the APK file using the system's loader.
                    if (apkPath != null) {
                        Timber.d("preferSystemIcon is true, new install, load from APK")
                        val themedIconFromApk = loadIconFromApkWithSystemLoader(apkPath, iconSizePx)
                        if (themedIconFromApk != null) {
                            Timber.d("preferSystemIcon is true, new install, load from APK with system loader")
                            return@async themedIconFromApk
                        }
                    }

                    // Priority 3 (Fallback): If all else fails, use the pre-parsed raw icon.
                    Timber.d("preferSystemIcon is true, falling back to load from rawApkIcon")
                    rawApkIcon
                } else {
                    // --- Default behavior, prefer new APK's icon ---
                    Timber.d("preferSystemIcon is false")
                    Timber.d("rawApkIcon is $rawApkIcon")
                    rawApkIcon ?: if (installedAppInfo != null) {
                        Timber.d("installedAppInfo is not null, load from installedAppInfo")
                        AppIconCache.loadIconDrawable(context, installedAppInfo, iconSizePx)
                    } else {
                        null
                    }
                }
            }
        }
        try {
            deferred.await()
        } catch (_: Exception) {
            iconCache.remove(cacheKey, deferred)
            null
        }
    }

    override suspend fun extractColorFromApp(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity.BaseEntity?,
        preferSystemIcon: Boolean
    ): Int? {
        val icon = getIcon(sessionId, packageName, entityToInstall, 256, preferSystemIcon)
        return extractColorFromDrawable(icon)
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

    override fun clearCacheForPackage(packageName: String) {
        iconCache.remove(packageName)
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
     * Loads an icon from an uninstalled APK file using the system's resource loader.
     * This allows icon packs and theming to be applied.
     *
     * @param apkPath The absolute path to the APK file.
     * @param iconSizePx The desired icon size in pixels.
     * @return A themed [Drawable] if successful, or null otherwise.
     */
    private suspend fun loadIconFromApkWithSystemLoader(apkPath: String, iconSizePx: Int): Drawable? {
        return try {
            val packageInfo = pm.getPackageArchiveInfo(apkPath, 0)
            packageInfo?.applicationInfo?.let { appInfo ->
                // The ApplicationInfo from getPackageArchiveInfo lacks the source path.
                // We must manually set it so the system knows where to load resources from.
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath

                // Now, use the same high-quality loader as for installed apps.
                AppIconCache.loadIconDrawable(context, appInfo, iconSizePx)
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not load themed icon from APK at path: $apkPath")
            null
        }
    }

}