// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.exception

import com.rosan.installer.R

/**
 * Custom exception for module installation failures.
 *
 * @param message A descriptive error message.
 * @param cause The underlying cause of the failure.
 */
class ModuleInstallFailedIncompatibleAuthorizerException(message: String, cause: Throwable? = null) : InstallerException(message, cause) {
    override fun getStringResId(): Int {
        return R.string.exception_module_install_failed_incompatible_authorizer
    }
}