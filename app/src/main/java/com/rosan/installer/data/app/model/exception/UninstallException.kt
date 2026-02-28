// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.exception

import com.rosan.installer.data.app.model.enums.UninstallErrorType

/**
 * Unified exception for all uninstallation failures.
 */
class UninstallException(
    val errorType: UninstallErrorType,
    message: String
) : InstallerException(message) {
    override fun getStringResId(): Int {
        return errorType.stringResId
    }
}
