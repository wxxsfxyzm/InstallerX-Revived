// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import android.graphics.drawable.Drawable

data class UninstallInfo(
    val packageName: String,
    val appLabel: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val isArchived: Boolean = false,
    val appIcon: Drawable? = null,
    val seedColor: Int? = null
)
