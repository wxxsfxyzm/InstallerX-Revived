// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPackageUidUseCase(
    private val systemEnvProvider: SystemEnvProvider
) {
    suspend operator fun invoke(packageName: String): Int? {
        if (packageName.isBlank()) return null

        return runCatching {
            withContext(Dispatchers.IO) {
                systemEnvProvider.getPackageUid(packageName)
            }
        }.getOrNull()
    }
}
