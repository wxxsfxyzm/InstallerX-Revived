// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SettingsSharedViewModel : ViewModel() {
    var pendingNavigateToTheme by mutableStateOf(false)
        private set

    fun markPendingNavigateToTheme(pending: Boolean) {
        pendingNavigateToTheme = pending
    }
}
