// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.repository

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.rosan.installer.domain.engine.model.AppEntity

/**
 * Repository responsible for managing application icons and extracting seed colors.
 */
interface AppIconRepository {

    companion object {
        /**
         * Reserved session ID for icons of apps already installed on the system.
         * Used to share cache across different configuration sessions.
         */
        const val SESSION_APP_LIST = "system_installed_apps"
    }

    /**
     * Gets the application icon.
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
     * Extracts the Material 3 seed color (ARGB) for a specific app.
     * Implementation should handle fetching the icon internally.
     */
    suspend fun extractColorFromApp(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity.BaseEntity?,
        preferSystemIcon: Boolean
    ): Int?

    /**
     * Directly extracts the seed color from an existing Drawable (e.g., in Uninstall flow).
     */
    suspend fun extractColorFromDrawable(drawable: Drawable?): Int?

    /**
     * Clears all cached icons associated with a specific session.
     * Use this when a session is finished or cancelled.
     */
    fun clearCacheForSession(sessionId: String)

    /**
     * Clears cached icons for a specific package across all sessions.
     * Useful when an app is updated/uninstalled to force an icon refresh.
     */
    fun clearCacheForPackage(packageName: String)
}
