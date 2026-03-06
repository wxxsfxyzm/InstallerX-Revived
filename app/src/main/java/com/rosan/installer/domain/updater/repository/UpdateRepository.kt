// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.updater.repository

import com.rosan.installer.domain.updater.model.UpdateInfo
import java.io.InputStream

interface UpdateRepository {
    /**
     * Checks for the latest application update.
     * @return UpdateInfo if a check is successful, or throws an exception on failure/skip conditions.
     */
    suspend fun checkUpdate(): UpdateInfo?

    /**
     * Opens a stream to download the update package.
     * @return A Pair containing the InputStream and the total content length (if known).
     */
    suspend fun downloadUpdate(url: String): Pair<InputStream, Long>?
}
