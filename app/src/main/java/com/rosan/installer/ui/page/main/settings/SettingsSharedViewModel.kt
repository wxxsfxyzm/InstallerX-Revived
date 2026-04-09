// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsSharedViewModel : ViewModel() {

    private val _state = MutableStateFlow(SettingsSharedState())
    val state: StateFlow<SettingsSharedState> = _state.asStateFlow()

    fun updateLastMainPageIndex(index: Int) {
        _state.update { currentState ->
            currentState.copy(lastMainPageIndex = index)
        }
    }
}
