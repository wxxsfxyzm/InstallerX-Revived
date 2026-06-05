// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.backup

import com.rosan.installer.domain.settings.model.backup.BackupEnvelope
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.repository.BackupRepository

class ValidateBackupUseCase(
    private val repository: BackupRepository
) {
    operator fun invoke(envelope: BackupEnvelope): BackupRestorePreview =
        repository.validateBackup(envelope)
}
