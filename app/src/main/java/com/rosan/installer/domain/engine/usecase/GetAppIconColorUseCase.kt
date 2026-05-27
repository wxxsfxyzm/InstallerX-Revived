// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import android.graphics.Bitmap
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.repository.AppIconRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * UseCase for extracting a representative seed color from an application's icon.
 * This is used for dynamic theming across the installer UI.
 */
class GetAppIconColorUseCase(
    private val appIconRepo: AppIconRepository
) {
    /**
     * Extracts a seed color based on the app's identity or installation entity.
     * @return An ARGB color integer, or null if extraction fails.
     */
    suspend operator fun invoke(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity? = null,
        preferSystemIcon: Boolean = true
    ): Int? = try {
        appIconRepo.extractColorFromApp(
            sessionId = sessionId,
            packageName = packageName,
            entityToInstall = entityToInstall,
            preferSystemIcon = preferSystemIcon
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to extract color for package: $packageName")
        null
    }

    /**
     * Extracts a seed color directly from an icon bitmap.
     * @return An ARGB color integer, or null if extraction fails.
     */
    suspend operator fun invoke(
        bitmap: Bitmap?
    ): Int? = try {
        appIconRepo.extractColorFromBitmap(bitmap)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to extract color from bitmap")
        null
    }
}
