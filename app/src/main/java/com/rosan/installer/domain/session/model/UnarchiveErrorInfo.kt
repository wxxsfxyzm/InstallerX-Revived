// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import android.app.PendingIntent
import com.rosan.installer.domain.archive.model.UnarchiveStatus

data class UnarchiveErrorInfo(
    val status: UnarchiveStatus,
    val requiredBytes: Long,
    val pendingIntent: PendingIntent?,
    val installerPackageName: String?,
    val installerLabel: CharSequence?
)
