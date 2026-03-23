// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.settings.model.ConfigModel
import timber.log.Timber

/**
 * UseCase to approve or reject an existing PackageInstaller session.
 * Used primarily for Binder-based confirmers.
 */
class ApproveSessionUseCase(
    private val appInstaller: AppInstallerRepository
) {
    /**
     * Submits the user's decision (granted or rejected) for the given session.
     */
    suspend operator fun invoke(
        sessionId: Int,
        granted: Boolean,
        config: ConfigModel
    ) {
        Timber.d("Approving session $sessionId (granted: $granted) via ${config.authorizer}")

        try {
            appInstaller.approveSession(config, sessionId, granted)
            Timber.d("Session $sessionId approval processed successfully.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to approve/reject session via ${config.authorizer}")
            throw e
        }
    }
}
