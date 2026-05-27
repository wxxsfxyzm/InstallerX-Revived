// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.install

object UninstallFlags {
    /**
     * Keep the package data directory.
     */
    const val DELETE_KEEP_DATA = 0x00000001

    /**
     * Delete the package for all users.
     */
    const val DELETE_ALL_USERS = 0x00000002

    /**
     * Mark a system app as uninstalled for the current user only.
     */
    const val DELETE_SYSTEM_APP = 0x00000004
}
