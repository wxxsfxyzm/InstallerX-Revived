// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model

// Data class representing Xposed module information
data class XposedModuleInfo(
    val minApi: String?,
    val targetApi: String?,
    val description: String?
)
