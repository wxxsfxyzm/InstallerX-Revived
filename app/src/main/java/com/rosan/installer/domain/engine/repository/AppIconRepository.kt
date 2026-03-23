// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.repository

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.repository.AppIconRepository.Companion.SETTINGS_APP_LIST

/**
 * Repository responsible for managing application icons and extracting
 * Material 3 seed colors.
 *
 * Implementations are expected to cache results internally. Callers should
 * use the [clearCacheForSession] and [clearCacheForPackage] methods to
 * invalidate stale entries when sessions end or packages change.
 */
interface AppIconRepository {

    companion object {
        /**
         * Reserved session ID for icons of apps already installed on the system.
         *
         * Using a shared, well-known session ID allows different UI screens
         * (e.g., settings app list, permission manager) to share the same
         * cached icon entries instead of loading duplicates.
         *
         * Usage:
         * ```
         * repository.getIcon(
         *     sessionId = AppIconRepository.SETTINGS_APP_LIST,
         *     packageName = "com.example.app",
         *     ...
         * )
         * ```
         */
        const val SETTINGS_APP_LIST = "system_installed_apps"
    }

    /**
     * Retrieves the application icon as a [Bitmap].
     *
     * The implementation resolves the icon using a priority chain that depends
     * on [preferSystemIcon]:
     * - **true** (upgrades): system launcher icon → APK system loader → raw APK drawable
     * - **false** (new installs): raw APK drawable → system launcher icon
     *
     * Results are cached by (sessionId, packageName, userId, iconSizePx).
     * A platform default icon is returned if all resolution strategies fail.
     *
     * @param sessionId   Logical grouping key for cache invalidation (e.g., an install
     *                    session ID or [SETTINGS_APP_LIST]).
     * @param packageName The application's package name.
     * @param entityToInstall The parsed APK entity, or null if unavailable.
     * @param userId      The Android user ID to load the icon for.
     * @param iconSizePx  Maximum icon dimension in pixels. The returned bitmap
     *                    will never exceed this size on either axis.
     * @param preferSystemIcon Whether to prefer the system-themed icon over
     *                         the raw APK drawable.
     * @return A [Bitmap] of the application icon, or a system fallback icon.
     *         Returns null only if even the fallback drawable is unavailable.
     */
    suspend fun getIcon(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity?,
        userId: Int,
        iconSizePx: Int = 256,
        preferSystemIcon: Boolean
    ): Bitmap?

    /**
     * Extracts the Material 3 seed color (ARGB int) from an application's icon.
     *
     * Internally calls [getIcon] with a size optimized for color extraction,
     * then runs the Celebi quantizer + HCT scoring algorithm.
     *
     * @param sessionId        Cache grouping key.
     * @param packageName      The application's package name.
     * @param entityToInstall  The parsed APK entity, or null.
     * @param preferSystemIcon Whether to prefer the system-themed icon.
     * @param userId           Android user ID; defaults to the current process
     *                         user if null.
     * @return The dominant seed color as an ARGB int, or null on failure.
     */
    suspend fun extractColorFromApp(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity.BaseEntity?,
        preferSystemIcon: Boolean,
        userId: Int? = null
    ): Int?

    /**
     * Extracts the Material 3 seed color directly from an existing [Drawable].
     *
     * Useful in flows where the icon is already available (e.g., uninstall
     * confirmation dialog) and does not need to be loaded from the cache.
     *
     * @param drawable The drawable to extract color from, or null.
     * @return The dominant seed color as an ARGB int, or null if [drawable]
     *         is null or extraction fails.
     */
    suspend fun extractColorFromDrawable(drawable: Drawable?): Int?

    /**
     * Invalidates all cached icons associated with the given [sessionId].
     *
     * Call this when an install/uninstall session completes, is cancelled,
     * or is otherwise no longer needed.
     */
    suspend fun clearCacheForSession(sessionId: String)

    /**
     * Invalidates cached icons for the given [packageName] across all sessions
     * and user IDs.
     *
     * Call this when an app is installed, updated, or uninstalled to ensure
     * subsequent [getIcon] calls return a fresh icon.
     */
    suspend fun clearCacheForPackage(packageName: String)
}
