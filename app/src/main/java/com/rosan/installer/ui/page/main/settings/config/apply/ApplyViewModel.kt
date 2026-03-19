// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.provider.SystemAppProvider
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.config.ToggleAppTargetConfigUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.time.Duration.Companion.seconds

class ApplyViewModel(
    appSettingsRepo: AppSettingsRepo,
    private val appRepo: AppRepository,
    private val systemAppProvider: SystemAppProvider,
    private val toggleAppTargetConfigUseCase: ToggleAppTargetConfigUseCase,
    private val updateSetting: UpdateSettingUseCase,
    private val id: Long
) : ViewModel(), KoinComponent {

    // Internal mutable flows for data that are not saved in the settings repository
    private val _apps = MutableStateFlow<ViewContent<List<ApplyViewApp>>>(
        ViewContent(data = emptyList(), progress = ViewContent.Progress.Loading)
    )
    private val _appEntities = MutableStateFlow<ViewContent<List<AppModel>>>(
        ViewContent(data = emptyList(), progress = ViewContent.Progress.Loading)
    )
    private val _search = MutableStateFlow("")

    // Helper data class to group the specific apply settings
    private data class ApplyPrefs(
        val orderTypeStr: String,
        val orderInReverse: Boolean,
        val selectedFirst: Boolean,
        val showSystemApp: Boolean,
        val showPackageName: Boolean
    )

    // Combine individual setting flows into a single flow for this page
    private val applyPrefsFlow = combine(
        appSettingsRepo.getString(StringSetting.ApplyOrderType),
        appSettingsRepo.getBoolean(BooleanSetting.ApplyOrderInReverse),
        appSettingsRepo.getBoolean(BooleanSetting.ApplySelectedFirst, default = true),
        appSettingsRepo.getBoolean(BooleanSetting.ApplyShowSystemApp),
        appSettingsRepo.getBoolean(BooleanSetting.ApplyShowPackageName, default = false)
    ) { orderTypeStr, orderInReverse, selectedFirst, showSystemApp, showPackageName ->
        ApplyPrefs(orderTypeStr, orderInReverse, selectedFirst, showSystemApp, showPackageName)
    }

    // Combine all flows to generate the final UI state
    val state: StateFlow<ApplyViewState> = combine(
        applyPrefsFlow,
        appSettingsRepo.preferencesFlow, // Use this strictly to get useBlur
        _apps,
        _appEntities,
        _search
    ) { applyPrefs, globalPrefs, apps, appEntities, search ->
        val orderType = runCatching { ApplyViewState.OrderType.valueOf(applyPrefs.orderTypeStr) }
            .getOrDefault(ApplyViewState.OrderType.Label)

        ApplyViewState(
            apps = apps,
            appEntities = appEntities,
            orderType = orderType,
            orderInReverse = applyPrefs.orderInReverse,
            selectedFirst = applyPrefs.selectedFirst,
            showSystemApp = applyPrefs.showSystemApp,
            showPackageName = applyPrefs.showPackageName,
            useBlur = globalPrefs.useBlur,
            search = search
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ApplyViewState()
    )

    init {
        loadApps()
        collectAppEntities()
    }

    fun dispatch(action: ApplyViewAction) {
        when (action) {
            ApplyViewAction.LoadApps -> loadApps()
            ApplyViewAction.LoadAppEntities -> collectAppEntities()
            is ApplyViewAction.ApplyPackageName -> applyPackageName(action.packageName, action.applied)
            is ApplyViewAction.Order -> viewModelScope.launch { updateSetting(StringSetting.ApplyOrderType, action.type.name) }
            is ApplyViewAction.OrderInReverse -> viewModelScope.launch { updateSetting(BooleanSetting.ApplyOrderInReverse, action.enabled) }
            is ApplyViewAction.SelectedFirst -> viewModelScope.launch { updateSetting(BooleanSetting.ApplySelectedFirst, action.enabled) }
            is ApplyViewAction.ShowSystemApp -> viewModelScope.launch { updateSetting(BooleanSetting.ApplyShowSystemApp, action.enabled) }
            is ApplyViewAction.ShowPackageName -> viewModelScope.launch { updateSetting(BooleanSetting.ApplyShowPackageName, action.enabled) }

            // Update local memory states
            is ApplyViewAction.Search -> _search.value = action.text
        }
    }

    private var loadAppsJob: Job? = null

    private fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch(Dispatchers.IO) {
            _apps.value = _apps.value.copy(progress = ViewContent.Progress.Loading)
            if (_apps.value.data.isNotEmpty()) delay(1.5.seconds)
            val list = systemAppProvider.getInstalledApps()
            _apps.value = _apps.value.copy(data = list, progress = ViewContent.Progress.Loaded)
        }
    }

    private var collectAppEntitiesJob: Job? = null

    private fun collectAppEntities() {
        collectAppEntitiesJob?.cancel()
        collectAppEntitiesJob = viewModelScope.launch(Dispatchers.IO) {
            _appEntities.value = _appEntities.value.copy(progress = ViewContent.Progress.Loading)
            appRepo.flowAll().collect { models ->
                _appEntities.value = _appEntities.value.copy(
                    data = models.filter { it.configId == id },
                    progress = ViewContent.Progress.Loaded
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
}
