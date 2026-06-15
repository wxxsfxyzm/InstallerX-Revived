// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import android.content.IntentSender

data class UnarchiveInfo(
    val packageName: String,
    val appLabel: CharSequence,
    val installerLabel: CharSequence,
    val intentSender: IntentSender
)
