// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors

package com.rosan.installer.ui.page.main.installer.dialog.inner

import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.page.main.widget.chip.WarningModel

// Encapsulate UI resources to keep the function signature clean
data class InstallWarningResources(
    val tagDowngrade: String,
    val textDowngrade: String,
    val tagSignature: String,
    val textSigMismatch: String,
    val textSigUnknown: String,
    val tagSdk: String,
    val textSdkIncompatible: String,
    val tagArch32: String,
    val textArch32: String,
    val tagEmulated: String,
    val textArchMismatchFormat: String, // Expecting a string with 2 placeholders
    val errorColor: Color,
    val tertiaryColor: Color
)

// Return type containing the list and the button ID
data class InstallStateResult(
    val warnings: List<WarningModel>,
    val buttonTextId: Int
)