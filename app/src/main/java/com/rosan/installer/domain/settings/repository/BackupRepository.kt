// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.repository

import com.rosan.installer.domain.settings.model.backup.BackupEnvelope
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.RestoreResult

interface BackupRepository {
    suspend fun exportBackup(): BackupEnvelope

    fun validateBackup(envelope: BackupEnvelope): BackupRestorePreview

    suspend fun restoreBackup(envelope: BackupEnvelope): RestoreResult
}
