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
import java.util.concurrent.ConcurrentHashMap

/**
 * The default implementation of [AppIconRepository].
 * It handles the logic for loading icons from the installed package (for upgrades)
 * or from the APK entity (for new installs) and caches the results.
 */
class AppIconRepoImpl : AppIconRepo, KoinComponent {

    private val context by inject<Context>()

    // Cache to store loading operations (Deferred) to handle concurrent requests.
    private val iconCache = ConcurrentHashMap<String, Deferred<Drawable?>>()

    override suspend fun getIcon(
        packageName: String,
        entityToInstall: AppEntity?,
        iconSizePx: Int
    ): Drawable? = coroutineScope {
        val deferred = iconCache.getOrPut(packageName) {
            async(Dispatchers.IO) {
                val installedAppInfo = try {
                    context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                } catch (e: PackageManager.NameNotFoundException) {
                    null // App is not currently installed.
                }

                if (installedAppInfo != null) {
                    // UPGRADE: Load high-quality icon from the installed application.
                    AppIconCache.loadIconDrawable(context, installedAppInfo, iconSizePx)
                } else {
                    // NEW INSTALL: Use the icon from the parsed APK entity.
                    (entityToInstall as? AppEntity.BaseEntity)?.icon
                }
            }
        }
        try {
            deferred.await()
        } catch (e: Exception) {
            // On failure, remove the deferred from the cache to allow retries.
            iconCache.remove(packageName, deferred)
            null
        }
    }

    override fun clearCacheForPackage(packageName: String) {
        iconCache.remove(packageName)
    }
}