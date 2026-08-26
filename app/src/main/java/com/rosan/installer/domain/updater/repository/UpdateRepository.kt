// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.updater.repository

import com.rosan.installer.domain.updater.model.UpdateInfo
import java.io.InputStream
import kotlinx.coroutines.flow.StateFlow

interface UpdateRepository {
    /**
     * A StateFlow emitting the latest update information.
     */
    val updateInfoFlow: StateFlow<UpdateInfo?>

    /**
     * Checks for the latest application update.
     * @param force Whether to force the update check.
     *
     * @return UpdateInfo if a check is successful, or throws an exception on failure/skip conditions.
     */
    suspend fun checkUpdate(force: Boolean = false): UpdateInfo?

    /**
     * Opens a stream to download the update package.
     * @return A Pair containing the InputStream and the total content length (if known).
     */
    suspend fun downloadUpdate(url: String): Pair<InputStream, Long>?
}
