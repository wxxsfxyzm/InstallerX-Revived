// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

data class DialogButton(
    val text: String,
    val weight: Float = 1f,
    val onClick: () -> Unit,
)
