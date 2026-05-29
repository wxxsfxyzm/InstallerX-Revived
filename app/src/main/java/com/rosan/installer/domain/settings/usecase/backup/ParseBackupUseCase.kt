// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.backup

import com.rosan.installer.domain.settings.model.backup.BackupEnvelope
import kotlinx.serialization.json.Json

class ParseBackupUseCase(
    private val json: Json
) {
    operator fun invoke(rawJson: String): BackupEnvelope =
        json.decodeFromString(rawJson)
}
