// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.state

/**
 * Represents the primary action the user will take.
 */
enum class InstallActionType {
    INSTALL,
    UPGRADE,
    DOWNGRADE_INSTALL_ANYWAY,
    UNARCHIVE,
    REINSTALL,
    SIGNATURE_MISMATCH_INSTALL_ANYWAY
}
