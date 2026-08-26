// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.history.repository.OperationHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: OperationHistoryRepository,
    private val capabilityProvider: DeviceCapabilityProvider,
) : ViewModel() {
    val state: StateFlow<HistoryViewState> = combine(
        repository.flowAll(),
        repository.isEnabled,
        repository.areIndicatorsEnabled,
    ) { records, isHistoryEnabled, areIndicatorsEnabled ->
        HistoryViewState(
            records = records,
            isLoading = false,
            isSystemApp = capabilityProvider.isSystemApp,
            isHistoryEnabled = isHistoryEnabled,
            areIndicatorsEnabled = areIndicatorsEnabled,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HistoryViewState(isLoading = true),
        )

    fun dispatch(action: HistoryViewAction) {
        when (action) {
            HistoryViewAction.ClearHistory -> viewModelScope.launch {
                repository.clear()
            }

            is HistoryViewAction.SetHistoryEnabled -> viewModelScope.launch {
                repository.setEnabled(
                    enabled = action.enabled,
                    clearHistory = action.clearHistory,
                )
            }

            is HistoryViewAction.SetIndicatorsEnabled -> viewModelScope.launch {
                repository.setIndicatorsEnabled(action.enabled)
            }
        }
    }
}
