// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import com.rosan.installer.domain.engine.model.install.InstallPhase

sealed interface ProgressEntity {
    data object Ready : ProgressEntity
    data object Error : ProgressEntity
    data object Finish : ProgressEntity

    data object InstallResolving : ProgressEntity
    data object InstallResolvedFailed : ProgressEntity
    data object InstallResolveSuccess : ProgressEntity

    /**
     * The new state for caching files, now with progress.
     * @param progress A value from 0.0f to 1.0f. A value of -1.0f can indicate an indeterminate progress.
     */
    data class InstallPreparing(val progress: Float) : ProgressEntity

    data object InstallAnalysing : ProgressEntity
    data object InstallAnalysedFailed : ProgressEntity
    data class InstallAnalysedUnsupported(val reason: String) : ProgressEntity
    data object InstallAnalysedSuccess : ProgressEntity

    /**
     * @param writeProgress Fraction of the current app's selected APK bytes written to its
     * PackageInstaller session, or null when byte progress is unavailable.
     * @param phase Whether payloads are still being written or PackageInstaller is processing the
     * staged session.
     */
    data class Installing(
        val current: Int = 1,
        val total: Int = 1,
        val appLabel: String? = null,
        val writeProgress: Float? = null,
        val phase: InstallPhase = InstallPhase.WRITING
    ) : ProgressEntity {
        init {
            require(writeProgress == null || writeProgress in 0f..1f) {
                "writeProgress must be between zero and one"
            }
        }

        fun overallProgress(): Float? {
            val itemProgress = writeProgress ?: return null
            val safeTotal = total.coerceAtLeast(1)
            val completedItems = (current - 1).coerceIn(0, safeTotal)
            return ((completedItems + itemProgress) / safeTotal).coerceIn(0f, 1f)
        }
    }
    data class InstallCompleted(val results: List<InstallResult>) : ProgressEntity
    data object InstallConfirming : ProgressEntity
    data object InstallWaitingUnknownSource : ProgressEntity
    data class InstallingModule(val output: List<String>) : ProgressEntity
    data object InstallFailed : ProgressEntity
    data object InstallSuccess : ProgressEntity

    data object UninstallResolving : ProgressEntity
    data object UninstallResolveFailed : ProgressEntity
    data object UninstallReady : ProgressEntity

    data object Uninstalling : ProgressEntity
    data object UninstallSuccess : ProgressEntity
    data object UninstallFailed : ProgressEntity

    data object UnarchiveResolving : ProgressEntity
    data object UnarchiveReady : ProgressEntity
    data object Unarchiving : ProgressEntity
    data object UnarchiveErrorReady : ProgressEntity
    data object UnarchiveFailed : ProgressEntity
}
