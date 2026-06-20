// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.error

import android.content.pm.PackageManagerHidden
import com.rosan.installer.R

/**
 * Enumeration mapping legacy uninstall error codes to their respective string resources.
 */
enum class UninstallErrorType(val legacyCode: Int, val stringResId: Int) {
    // --- Legacy Install Error Codes From Package Manager Service ---
    INTERNAL_ERROR(PackageManagerHidden.DELETE_FAILED_INTERNAL_ERROR, R.string.exception_uninstall_failed_internal_error),
    DEVICE_POLICY_MANAGER(PackageManagerHidden.DELETE_FAILED_DEVICE_POLICY_MANAGER, R.string.exception_uninstall_failed_device_policy),
    USER_RESTRICTED(PackageManagerHidden.DELETE_FAILED_USER_RESTRICTED, R.string.exception_install_failed_user_restricted),
    OWNER_BLOCKED(PackageManagerHidden.DELETE_FAILED_OWNER_BLOCKED, R.string.exception_uninstall_failed_owner_blocked),
    ABORTED(PackageManagerHidden.DELETE_FAILED_ABORTED, R.string.exception_uninstall_failed_aborted),
    USED_SHARED_LIBRARY(PackageManagerHidden.DELETE_FAILED_USED_SHARED_LIBRARY, R.string.exception_uninstall_failed_used_shared_library),
    APP_PINNED(PackageManagerHidden.DELETE_FAILED_APP_PINNED, R.string.exception_uninstall_failed_app_pinned),
    HYPEROS_SYSTEM_APP(-1000, R.string.exception_uninstall_failed_hyperos_system_app),

    // --- Fallback for unknown status codes ---
    UNKNOWN(Int.MAX_VALUE, R.string.exception_install_failed_unknown);

    companion object {
        fun fromLegacyCode(code: Int): UninstallErrorType {
            return entries.find { it.legacyCode == code } ?: UNKNOWN
        }
    }
}
