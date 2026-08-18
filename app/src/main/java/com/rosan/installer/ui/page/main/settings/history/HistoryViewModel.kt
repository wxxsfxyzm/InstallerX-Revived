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
    private val searchCriteria = MutableStateFlow(HistorySearchCriteria())

    val state: StateFlow<HistoryViewState> = combine(
        repository.flowAll(),
        searchCriteria
    ) { records, criteria ->
            HistoryViewState(
                records = records,
                searchCriteria = criteria,
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

            is HistoryViewAction.UpdateSearchQuery -> searchCriteria.update {
                it.copy(query = action.query)
            }

            is HistoryViewAction.SelectSearchField -> searchCriteria.update {
                it.copy(field = action.field)
            }

            HistoryViewAction.ClearSearch -> searchCriteria.value = HistorySearchCriteria()
        }
    }
}
