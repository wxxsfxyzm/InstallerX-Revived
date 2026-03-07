// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.model

data class PostInstallTaskInfo(
    val packageName: String,
    val enableDexopt: Boolean = false,
    val dexoptMode: String = "speed-profile",
    val forceDexopt: Boolean = false,
    val enableAutoDelete: Boolean = false,
    val deletePaths: List<String> = emptyList()
) {
    val hasTasks: Boolean get() = enableDexopt || (enableAutoDelete && deletePaths.isNotEmpty())
}
