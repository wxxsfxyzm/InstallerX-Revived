// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog

import androidx.compose.runtime.Composable

data class DialogInnerParams(
    val id: String,
    val content: (@Composable () -> Unit)? = null
)
