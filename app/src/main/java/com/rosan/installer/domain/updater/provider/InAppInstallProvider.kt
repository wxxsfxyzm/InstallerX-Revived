// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.updater.provider

import com.rosan.installer.domain.settings.model.ConfigModel
import java.io.InputStream

/**
 * An abstraction to decouple the Updater domain from the complex Installer domain.
 */
interface InAppInstallProvider {
    /**
     * Executes the installation of an update package from a stream.
     */
    suspend fun executeInstall(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        config: ConfigModel
    )
}
