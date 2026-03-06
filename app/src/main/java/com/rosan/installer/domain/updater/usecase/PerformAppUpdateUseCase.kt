// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.updater.usecase

import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.updater.model.UpdateInfo
import com.rosan.installer.domain.updater.provider.InAppInstallProvider
import com.rosan.installer.domain.updater.repository.UpdateRepository
import java.io.IOException

class PerformAppUpdateUseCase(
    private val updateRepository: UpdateRepository,
    private val inAppInstallProvider: InAppInstallProvider
) {
    /**
     * Executes the full in-app update flow.
     * @throws IllegalStateException if the update info is invalid.
     * @throws IOException if the download stream fails to open.
     */
    suspend operator fun invoke(updateInfo: UpdateInfo?, config: ConfigModel) {
        if (updateInfo == null || !updateInfo.hasUpdate || updateInfo.downloadUrl.isEmpty()) {
            throw IllegalStateException("No valid update info found.")
        }

        // 1. Download the file stream
        val downloadData = updateRepository.downloadUpdate(updateInfo.downloadUrl)
            ?: throw IOException("Failed to open download stream from ${updateInfo.downloadUrl}")

        val (stream, length) = downloadData

        // 2. Trigger the installation using the provider (which shields us from Installer details)
        inAppInstallProvider.executeInstall(
            fileName = "base.apk",
            inputStream = stream,
            contentLength = length,
            config = config
        )
    }
}
