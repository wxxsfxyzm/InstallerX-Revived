// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.device.provider

import com.rosan.installer.domain.device.model.PermissionType

/**
 * Interface to check if a specific permission is granted.
 * This belongs to the Domain layer, completely decoupled from Android UI.
 */
interface PermissionChecker {
    /**
     * Checks if the specified permission is currently granted.
     *
     * @param type The type of permission to check.
     * @return True if granted, false otherwise.
     */
    fun hasPermission(type: PermissionType): Boolean
}
