// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.model

data class PendingSelfUpdate(
    val sessionId: String,
    val previousUpdateTime: Long,
    val armedAtElapsed: Long
)
