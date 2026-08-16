// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.history.repository.OperationHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: OperationHistoryRepository,
    private val capabilityProvider: DeviceCapabilityProvider
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val searchField = MutableStateFlow(HistorySearchField.ALL)

    val state: StateFlow<HistoryViewState> = combine(
        repository.flowAll(),
        searchQuery,
        searchField
    ) { records, query, field ->
            HistoryViewState(
                records = records,
                searchQuery = query,
                searchField = field,
                isLoading = false,
                isSystemApp = capabilityProvider.isSystemApp
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HistoryViewState(isLoading = true)
        )

    fun dispatch(action: HistoryViewAction) {
        when (action) {
            HistoryViewAction.ClearHistory -> viewModelScope.launch {
                repository.clear()
            }

            is HistoryViewAction.Search -> searchQuery.update { action.query }
            is HistoryViewAction.SelectSearchField -> searchField.update { action.field }
        }
    }
}
