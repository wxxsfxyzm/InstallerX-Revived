// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.exception

import com.rosan.installer.data.app.model.enums.InstallErrorType

/**
 * Unified exception for all installation failures.
 */
class InstallException(
    val errorType: InstallErrorType,
    message: String
) : InstallerException(message) {
    override fun getStringResId(): Int {
        return errorType.stringResId
    }
}
