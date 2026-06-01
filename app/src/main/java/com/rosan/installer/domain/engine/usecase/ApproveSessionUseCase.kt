// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.history.usecase.RecordOperationHistoryUseCase
import com.rosan.installer.domain.history.usecase.historyErrorSummary
import com.rosan.installer.domain.history.usecase.historyErrorType
import com.rosan.installer.domain.session.model.ConfirmationDetails
import com.rosan.installer.domain.settings.model.config.ConfigModel
import timber.log.Timber

/**
 * UseCase to approve or reject an existing PackageInstaller session.
 * Used primarily for Binder-based confirmers.
 */
class ApproveSessionUseCase(
    private val appInstaller: AppInstallerRepository,
    private val recordOperationHistory: RecordOperationHistoryUseCase
) {
    /**
     * Submits the user's decision (granted or rejected) for the given session.
     */
    suspend operator fun invoke(
        sessionId: Int,
        granted: Boolean,
        config: ConfigModel,
        details: ConfirmationDetails? = null
    ) {
        Timber.d("Approving session $sessionId (granted: $granted) via ${config.authorizer}")

        val result = try {
            appInstaller.approveSession(config, sessionId, granted)
            Timber.d("Session $sessionId approval processed successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to approve/reject session via ${config.authorizer}")
            Result.failure(e)
        }

        recordSessionConfirmationHistory(config, details, granted, result)
        result.onFailure { throw it }
    }

    private suspend fun recordSessionConfirmationHistory(
        config: ConfigModel,
        details: ConfirmationDetails?,
        granted: Boolean,
        result: Result<Unit>
    ) {
        val packageName = details?.packageName?.takeIf { it.isNotBlank() } ?: "unknown"
        runCatching {
            recordOperationHistory(
                OperationHistoryModel(
                    operationType = OperationType.SESSION_CONFIRM,
                    status = if (result.isSuccess && granted) OperationStatus.SUCCESS else OperationStatus.FAILED,
                    packageName = packageName,
                    appLabel = details?.appLabel?.toString(),
                    isFreshInstall = details?.isUpdate?.let { !it },
                    versionChange = VersionChange.UNKNOWN,
                    initiatorPackageName = config.initiatorPackageName,
                    installerPackageName = details?.installerPackageName,
                    installMethod = InstallMethod.SESSION,
                    authorizer = config.authorizer,
                    installMode = config.installMode,
                    errorSummary = when {
                        result.isFailure -> result.exceptionOrNull()?.historyErrorSummary()
                        !granted -> "Session confirmation denied"
                        else -> null
                    },
                    errorType = when {
                        result.isFailure -> result.exceptionOrNull()?.historyErrorType()
                        !granted -> "DENIED"
                        else -> null
                    }
                )
            )
        }.onFailure { e ->
            Timber.e(e, "Failed to record session confirmation history for $packageName")
        }
    }
}
