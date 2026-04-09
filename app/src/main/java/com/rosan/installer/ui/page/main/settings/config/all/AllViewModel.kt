// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.all

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.domain.settings.util.ConfigOrder
import com.rosan.installer.ui.navigation.Navigator
import com.rosan.installer.ui.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class AllViewModel(
    var navigator: Navigator,
    private val repo: ConfigRepository,
    private val appSettingsRepo: AppSettingsRepository
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow(AllViewState())
    val uiState: StateFlow<AllViewState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AllViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var loadDataJob: Job? = null

    init {
        // Fetch the initial "user read tips" state before starting the continuous data flow
        viewModelScope.launch {
            val userReadScopeTips = appSettingsRepo.getBoolean(BooleanSetting.UserReadScopeTips, default = false).first()
            _uiState.update { it.copy(userReadScopeTips = userReadScopeTips) }
            loadData()
        }
    }

    fun dispatch(action: AllViewAction) {
        when (action) {
            is AllViewAction.LoadData -> loadData()
            is AllViewAction.UserReadScopeTips -> userReadTips()
            is AllViewAction.ChangeDataConfigOrder -> changeDataConfigOrder(action.configOrder)
            is AllViewAction.DeleteDataConfig -> deleteDataConfig(action.configModel)
            is AllViewAction.RestoreDataConfig -> restoreDataConfig(action.configModel)
            is AllViewAction.EditDataConfig -> editDataConfig(action.configModel)
            is AllViewAction.ApplyConfig -> applyConfig(action.configModel)
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        // Set loading state
        _uiState.update { currentState ->
            currentState.copy(
                data = currentState.data.copy(
                    progress = AllViewState.Data.Progress.Loading
                )
            )
        }

        // Start listening to the config DB
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            // Collect the flow from DB based on the current order
            repo.flowAll(_uiState.value.data.configOrder).collect { newConfigs ->
                _uiState.update { currentState ->
                    currentState.copy(
                        data = currentState.data.copy(
                            configs = newConfigs,
                            progress = AllViewState.Data.Progress.Loaded
                        )
                    )
                }
            }
        }
    }

    private fun userReadTips() {
        _uiState.update { it.copy(userReadScopeTips = true) }
        viewModelScope.launch {
            appSettingsRepo.putBoolean(BooleanSetting.UserReadScopeTips, true)
        }
    }

    private fun changeDataConfigOrder(configOrder: ConfigOrder) {
        // Update the order state
        _uiState.update { currentState ->
            currentState.copy(data = currentState.data.copy(configOrder = configOrder))
        }
        // Restart the data collection with the new order
        loadData()
    }

    private fun editDataConfig(configModel: ConfigModel) {
        viewModelScope.launch {
            navigator.push(
                Route.EditConfig(
                    configModel.id
                )
            )
        }
    }

    private fun deleteDataConfig(configModel: ConfigModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(configModel)
            _eventFlow.emit(AllViewEvent.DeletedConfig(configModel))
        }
    }

    private fun restoreDataConfig(configModel: ConfigModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insert(configModel)
        }
    }

    private fun applyConfig(configModel: ConfigModel) {
        viewModelScope.launch {
            navigator.push(
                Route.ApplyConfig(
                    configModel.id
                )
            )
        }
    }
}
