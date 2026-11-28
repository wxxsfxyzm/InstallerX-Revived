// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.core.widgets.chip

import androidx.compose.ui.graphics.Color

// Data model to hold both short tag and full description
data class NoticeModel(
    val shortLabel: String, // Displayed on the chip
    val fullDescription: String, // Displayed in the dialog
    val color: Color, // Color of the chip
)
