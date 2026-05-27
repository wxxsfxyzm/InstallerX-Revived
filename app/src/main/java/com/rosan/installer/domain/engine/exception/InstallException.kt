// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.exception

import com.rosan.installer.core.exception.InstallerException
import com.rosan.installer.domain.engine.model.error.InstallErrorType

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
