// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import com.rosan.installer.domain.engine.provider.SessionDetailsProvider
import com.rosan.installer.domain.session.model.ConfirmationDetails
import com.rosan.installer.domain.settings.model.config.ConfigModel
import timber.log.Timber

/**
 * UseCase to retrieve details (label and icon) for a specific installation session.
 * Utilizes IPC services depending on the application's current authorization level.
 */
class GetSessionConfirmationDetailsUseCase(
    private val sessionDetailsProvider: SessionDetailsProvider
) {
    /**
     * Retrieves session details based on the provided configuration.
     */
    operator fun invoke(
        sessionId: Int,
        config: ConfigModel,
        isSelfSession: Boolean = false,
        currentProgress: Int = 1,
        totalProgress: Int = 1
    ): ConfirmationDetails {
        var label: CharSequence? = null
        var icon: Bitmap? = null
        var packageName = ""
        var isUpdate = false
        var isOwnershipConflict = false
        var sourceAppLabel: CharSequence? = null
        var installerPackageName: String? = null

        Timber.d("Getting session details via service (Authorizer: ${config.authorizer})")

        var bundle: Bundle? = null
        try {
            bundle = sessionDetailsProvider.getSessionDetails(sessionId, config)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get session details via ${config.authorizer}")
        }

        if (bundle != null) {
            label = bundle.getCharSequence("appLabel") ?: "N/A"
            packageName = bundle.getString("packageName", "")
            isUpdate = bundle.getBoolean("isUpdate", false)
            isOwnershipConflict = bundle.getBoolean("isOwnershipConflict", false)
            sourceAppLabel = bundle.getCharSequence("sourceAppLabel")
            installerPackageName = bundle.getString("installerPackageName")

            try {
                icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable("appIcon", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelable("appIcon") as? Bitmap
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract icon bitmap from bundle")
            }
        } else {
            Timber.w("Service returned null bundle for session $sessionId")
        }

        return ConfirmationDetails(
            sessionId = sessionId,
            appLabel = label ?: "N/A",
            appIcon = icon,
            packageName = packageName,
            isUpdate = isUpdate,
            isOwnershipConflict = isOwnershipConflict,
            sourceAppLabel = sourceAppLabel,
            installerPackageName = installerPackageName,
            isSelfSession = isSelfSession,
            currentProgress = currentProgress,
            totalProgress = totalProgress
        )
    }
}
