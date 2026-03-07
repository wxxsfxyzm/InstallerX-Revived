// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.repository

import com.rosan.installer.domain.updater.model.UpdateInfo
import com.rosan.installer.domain.updater.repository.UpdateRepository
import timber.log.Timber
import java.io.InputStream

class OfflineUpdateRepositoryImpl : UpdateRepository {
    override suspend fun checkUpdate(): UpdateInfo? {
        Timber.d("Update check disabled: Offline build")
        return null
    }

    override suspend fun downloadUpdate(url: String): Pair<InputStream, Long>? {
        Timber.w("Download is not supported in Offline build")
        return null
    }
}
