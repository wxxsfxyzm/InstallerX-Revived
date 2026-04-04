// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.notification

import com.rosan.installer.domain.session.model.ProgressEntity

interface SessionNotifier {
    /**
     * Pushes the latest state from the data layer.
     * Throttling and UI rendering will be handled internally.
     */
    fun updateState(progress: ProgressEntity, background: Boolean)

    fun showToast(message: String)

    fun cancel()

    suspend fun cleanup()
}
