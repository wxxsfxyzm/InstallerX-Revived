// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.exception

import com.rosan.installer.core.exception.InstallerException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType

/**
 * Unified exception for all privileged operations (Root, Shizuku, Dhizuku, App_Process) failures.
 */
class PrivilegedException(
    val errorType: PrivilegedErrorType,
    message: String? = null,
    cause: Throwable? = null
) : InstallerException(message ?: cause?.message ?: cause?.toString(), cause) {
    override fun getStringResId() = errorType.stringResId
}
