// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.updater.model

data class UpdateInfo(
    val hasUpdate: Boolean,
    val remoteVersion: String,
    val releaseUrl: String,
    val downloadUrl: String
)
