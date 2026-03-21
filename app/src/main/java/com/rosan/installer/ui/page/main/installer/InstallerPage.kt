// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import androidx.compose.runtime.Composable
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.dialog.DialogPage

@Composable
fun InstallerPage(session: InstallerSessionRepository) {
    DialogPage(session = session)
}