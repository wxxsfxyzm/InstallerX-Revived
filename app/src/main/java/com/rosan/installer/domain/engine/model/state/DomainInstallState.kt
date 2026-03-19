// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.state

/**
 * The pure business result of the analysis.
 */
data class DomainInstallState(
    val actionType: InstallActionType,
    val notices: List<InstallNotice>
)
