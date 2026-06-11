// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.config.DeletedConfigSnapshot
import com.rosan.installer.domain.settings.repository.ConfigRepository

class RestoreDeletedConfigSnapshotUseCase(
    private val configRepo: ConfigRepository
) {
    suspend operator fun invoke(snapshot: DeletedConfigSnapshot) {
        configRepo.restoreDeleted(snapshot)
    }
}
