// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.history.usecase

import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.repository.OperationHistoryRepository

class RecordOperationHistoryUseCase(
    private val repository: OperationHistoryRepository
) {
    suspend operator fun invoke(model: OperationHistoryModel) {
        repository.insert(model)
    }
}

fun Throwable.historyErrorType(): String = when (this) {
    is InstallException -> errorType.name
    else -> this::class.simpleName ?: "Unknown"
}

fun Throwable.historyErrorSummary(): String =
    message?.take(MAX_ERROR_SUMMARY_LENGTH) ?: this::class.simpleName.orEmpty()

private const val MAX_ERROR_SUMMARY_LENGTH = 500
