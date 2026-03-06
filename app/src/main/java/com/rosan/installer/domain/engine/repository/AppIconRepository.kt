// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.repository

import android.graphics.drawable.Drawable
import com.rosan.installer.domain.engine.model.AppEntity

/**
 * Repository responsible for managing application icons and extracting seed colors.
 */
interface AppIconRepository {

    /**
     * Gets the application icon.
     */
    suspend fun getIcon(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity?,
        iconSizePx: Int = 256,
        preferSystemIcon: Boolean
    ): Drawable?

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
     * Clears cached icons/colors for a specific package.
     */
    fun clearCacheForPackage(packageName: String)
}