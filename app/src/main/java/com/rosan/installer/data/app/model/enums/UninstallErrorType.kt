// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.app.model.enums

import com.rosan.installer.R

/**
 * Enumeration mapping legacy uninstall error codes to their respective string resources.
 */
enum class UninstallErrorType(val legacyCode: Int, val stringResId: Int) {
    INTERNAL_ERROR(-1, R.string.exception_uninstall_failed_internal_error),
    DEVICE_POLICY_MANAGER(-2, R.string.exception_install_failed_unknown),
    USER_RESTRICTED(-3, R.string.exception_install_failed_user_restricted),
    OWNER_BLOCKED(-4, R.string.exception_install_failed_unknown),
    ABORTED(-5, R.string.exception_uninstall_failed_aborted),
    HYPEROS_SYSTEM_APP(-1000, R.string.exception_uninstall_failed_hyperos_system_app),

    // Fallback for unknown status codes
    UNKNOWN(Int.MAX_VALUE, R.string.exception_install_failed_unknown);

    companion object {
        fun fromLegacyCode(code: Int): UninstallErrorType {
            return entries.find { it.legacyCode == code } ?: UNKNOWN
        }
    }
}