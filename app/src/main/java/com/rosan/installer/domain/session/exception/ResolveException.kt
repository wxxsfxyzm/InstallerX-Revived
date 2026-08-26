// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.exception

import com.rosan.installer.core.exception.InstallerException
import com.rosan.installer.domain.session.model.ResolveErrorType

/**
 * Unified exception for all resolve-phase failures.
 */
class ResolveException(val errorType: ResolveErrorType, message: String? = null, cause: Throwable? = null) : InstallerException(message, cause) {
    override fun getStringResId() = errorType.stringResId
}
