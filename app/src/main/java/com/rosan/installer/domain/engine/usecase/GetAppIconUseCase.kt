// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import android.graphics.Bitmap
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.repository.AppIconRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class GetAppIconUseCase(
    private val appIconRepo: AppIconRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        packageName: String,
        entityToInstall: AppEntity? = null,
        userId: Int = 0,
        iconSizePx: Int = 256,
        preferSystemIcon: Boolean = true
    ): Bitmap? = try {
        appIconRepo.getIcon(
            sessionId = sessionId,
            packageName = packageName,
            entityToInstall = entityToInstall,
            userId = userId,
            iconSizePx = iconSizePx,
            preferSystemIcon = preferSystemIcon
        )
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to get icon: $packageName")
        null
    }
}
