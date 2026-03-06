// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.provider.SystemAppProvider
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.config.ToggleAppTargetConfigUseCase
import com.rosan.installer.ui.common.ViewContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.time.Duration.Companion.seconds

class ApplyViewModel(
    private val appRepo: AppRepository,
    private val appSettingsRepo: AppSettingsRepo,
    private val systemAppProvider: SystemAppProvider,
    private val toggleAppTargetConfigUseCase: ToggleAppTargetConfigUseCase,
    private val id: Long
) : ViewModel(), KoinComponent {
    var state by mutableStateOf(ApplyViewState())

    init {
        loadAndObserveSettings()
        loadApps()
        collectAppEntities()
    }

    fun dispatch(action: ApplyViewAction) {
        when (action) {
            ApplyViewAction.LoadApps -> loadApps()
            ApplyViewAction.LoadAppEntities -> collectAppEntities()
            is ApplyViewAction.ApplyPackageName -> applyPackageName(
                action.packageName, action.applied
            )

            is ApplyViewAction.Order -> order(action.type)
            is ApplyViewAction.OrderInReverse -> orderInReverse(action.enabled)
            is ApplyViewAction.SelectedFirst -> selectedFirst(action.enabled)
            is ApplyViewAction.ShowSystemApp -> showSystemApp(action.enabled)
            is ApplyViewAction.ShowPackageName -> showPackageName(action.enabled)
            is ApplyViewAction.Search -> search(action.text)
        }
    }

    private var loadAppsJob: Job? = null

    private fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(
                apps = state.apps.copy(
                    progress = ViewContent.Progress.Loading
                )
            )
            if (state.apps.data.isNotEmpty()) delay(1.5.seconds)
            val list = systemAppProvider.getInstalledApps()
            state = state.copy(
                apps = state.apps.copy(data = list, progress = ViewContent.Progress.Loaded)
            )
        }
    }

    private var collectAppEntitiesJob: Job? = null

    private fun collectAppEntities() {
        collectAppEntitiesJob?.cancel()
        collectAppEntitiesJob = viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(
                appEntities = state.appEntities.copy(
                    progress = ViewContent.Progress.Loading
                )
            )
            // Retrieve Flow<List<AppModel>> from the repository
            appRepo.flowAll().collect { models ->
                state = state.copy(
                    appEntities = state.appEntities.copy(
                        // Filter the list in memory
                        data = models.filter { it.configId == id },
                        progress = ViewContent.Progress.Loaded
                    )
                )
            }
        }
    }

    private fun applyPackageName(packageName: String?, applied: Boolean) {
        if (packageName == null) return
        viewModelScope.launch(Dispatchers.IO) {
            toggleAppTargetConfigUseCase(packageName, id, applied)
        }
    }

    private fun loadAndObserveSettings() {
        viewModelScope.launch {
            val initialState = ApplyViewState(
                orderType = appSettingsRepo.getString(StringSetting.ApplyOrderType)
                    .first()
                    .let { name ->
                        runCatching { ApplyViewState.OrderType.valueOf(name) }
                            .getOrDefault(ApplyViewState.OrderType.Label)
                    },
                orderInReverse = appSettingsRepo.getBoolean(BooleanSetting.ApplyOrderInReverse).first(),
                selectedFirst = appSettingsRepo.getBoolean(BooleanSetting.ApplySelectedFirst, default = true).first(),
                showSystemApp = appSettingsRepo.getBoolean(BooleanSetting.ApplyShowSystemApp).first(),
                showPackageName = appSettingsRepo.getBoolean(BooleanSetting.ApplyShowPackageName, default = false).first()
            )

            state = state.copy(
                orderType = initialState.orderType,
                orderInReverse = initialState.orderInReverse,
                selectedFirst = initialState.selectedFirst,
                showSystemApp = initialState.showSystemApp,
                showPackageName = initialState.showPackageName
            )
        }
    }

    private fun order(type: ApplyViewState.OrderType) {
        state = state.copy(orderType = type)
        viewModelScope.launch {
            appSettingsRepo.putString(StringSetting.ApplyOrderType, type.name)
        }
    }

    private fun orderInReverse(enabled: Boolean) {
        state = state.copy(orderInReverse = enabled)
        viewModelScope.launch {
            appSettingsRepo.putBoolean(BooleanSetting.ApplyOrderInReverse, enabled)
        }
    }

    private fun selectedFirst(enabled: Boolean) {
        state = state.copy(selectedFirst = enabled)
        viewModelScope.launch {
            appSettingsRepo.putBoolean(BooleanSetting.ApplySelectedFirst, enabled)
        }
    }

    private fun showSystemApp(enabled: Boolean) {
        state = state.copy(showSystemApp = enabled)
        viewModelScope.launch {
            appSettingsRepo.putBoolean(BooleanSetting.ApplyShowSystemApp, enabled)
        }
    }

    private fun showPackageName(enabled: Boolean) {
        state = state.copy(showPackageName = enabled)
        viewModelScope.launch {
            appSettingsRepo.putBoolean(BooleanSetting.ApplyShowPackageName, enabled)
        }
    }

    private fun search(text: String) {
        state = state.copy(search = text)
    }
}