package com.rosan.installer.data.app.model.impl

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.AppIconCache
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * The default implementation of [AppIconRepo].
 * It handles the logic for loading icons from the installed package (for upgrades)
 * or from the APK entity (for new installs) and caches the results.
 */
class AppIconRepoImpl : AppIconRepo, KoinComponent {

    private val context by inject<Context>()

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

                // Try to get ApplicationInfo from an already installed package.
                val installedAppInfo = try {
                    context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }

                if (preferSystemIcon) {
                    // --- User prefers system/themed icon ---
                    // Attempt to load the icon from the installed system package.
                    val systemIcon = if (installedAppInfo != null) {
                        AppIconCache.loadIconDrawable(context, installedAppInfo, iconSizePx)
                    } else {
                        null
                    }
                    // If system loading fails (e.g., app not installed), fall back to the high-quality APK loader.
                    systemIcon ?: rawApkIcon
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
        } catch (e: Exception) {
            iconCache.remove(cacheKey, deferred)
            null
        }
    }

    override fun clearCacheForPackage(packageName: String) {
        iconCache.remove(packageName)
    }
}