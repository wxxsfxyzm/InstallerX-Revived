// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.backup

import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupValidationException

class PrepareBackupRestoreUseCase(
    private val parseBackup: ParseBackupUseCase,
    private val validateBackup: ValidateBackupUseCase
) {
    operator fun invoke(rawJson: String): BackupRestorePreview {
        val envelope = runCatching {
            parseBackup(rawJson)
        }.getOrElse {
            throw BackupValidationException(
                messageResId = R.string.backup_settings_validation_invalid_file,
                code = "invalid_backup_file"
            )
        }
        return validateBackup(envelope)
    }
}
