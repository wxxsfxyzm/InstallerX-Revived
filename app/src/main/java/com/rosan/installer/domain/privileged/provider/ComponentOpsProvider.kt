// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import android.content.Intent
import com.rosan.installer.domain.settings.model.ConfigModel

/**
 * Provider for executing privileged Android component operations via Binder/IPC.
 * Handles tasks like starting activities or sending broadcasts across user boundaries.
 */
interface ComponentOpsProvider {
    suspend fun startActivityPrivileged(config: ConfigModel, intent: Intent): Boolean
    suspend fun sendBroadcastPrivileged(config: ConfigModel, intent: Intent): Boolean
}
