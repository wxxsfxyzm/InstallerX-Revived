package com.rosan.installer.data.app.repo

import android.graphics.drawable.Drawable
import com.rosan.installer.data.app.model.entity.AppEntity

/**
 * An interface for a repository that provides and manages application icons.
 * This abstraction allows for decoupled icon loading logic.
 */
interface AppIconRepo {
    /**
     * Gets the application icon, utilizing a caching mechanism.
     *
     * @param packageName The package name of the app.
     * @param entityToInstall The AppEntity, used as a fallback for new installs.
     * @param iconSizePx The desired icon size in pixels.
     * @return The loaded Drawable, or null if it fails.
     */
    suspend fun getIcon(
        packageName: String,
        entityToInstall: AppEntity?,
        iconSizePx: Int
    ): Drawable?

    /**
     * Clears the icon cache for a specific package.
     * This should be called after an operation (like install/uninstall) that changes the app's icon.
     *
     * @param packageName The package name to clear from the cache.
     */
    fun clearCacheForPackage(packageName: String)
}