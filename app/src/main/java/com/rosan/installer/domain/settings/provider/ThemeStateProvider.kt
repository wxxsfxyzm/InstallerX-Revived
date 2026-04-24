// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.provider

import com.rosan.installer.domain.settings.model.ThemeState
import kotlinx.coroutines.flow.StateFlow

interface ThemeStateProvider {
    val themeStateFlow: StateFlow<ThemeState>
}
