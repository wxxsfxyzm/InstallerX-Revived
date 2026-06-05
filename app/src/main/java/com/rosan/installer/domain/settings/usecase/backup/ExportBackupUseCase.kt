// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.backup

import com.rosan.installer.domain.settings.repository.BackupRepository
import kotlinx.serialization.json.Json

class ExportBackupUseCase(
    private val repository: BackupRepository,
    private val json: Json
) {
    suspend operator fun invoke(): String =
        json.encodeToString(repository.exportBackup())
}
