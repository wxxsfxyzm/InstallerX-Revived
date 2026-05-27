// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.exception

import com.rosan.installer.core.exception.InstallerException
import com.rosan.installer.domain.engine.model.error.ModuleInstallErrorType

/**
 * Unified exception for all module installation failures.
 */
class ModuleInstallException(
    val errorType: ModuleInstallErrorType,
    message: String? = null,
    cause: Throwable? = null
) : InstallerException(message, cause) {

    override fun getStringResId(): Int {
        return errorType.stringResId
    }
}
