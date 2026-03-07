// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.provider

import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.updater.provider.InAppInstallProvider
import timber.log.Timber
import java.io.InputStream

class OfflineInAppInstallProviderImpl : InAppInstallProvider {
    override suspend fun executeInstall(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        config: ConfigModel
    ) {
        Timber.d("Offline build: executeInstall called but safely ignored.")
    }
}
