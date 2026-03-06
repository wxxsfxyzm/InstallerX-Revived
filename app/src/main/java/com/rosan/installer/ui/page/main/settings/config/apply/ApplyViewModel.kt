// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.repository.AppRepo
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.ConfigRepo
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.ui.common.ViewContent
import com.rosan.installer.util.hasFlag
import com.rosan.installer.util.pm.compatVersionCode
import com.rosan.installer.util.pm.getCompatInstalledPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class ApplyViewModel(
    private val configRepo: ConfigRepo,
    private val appRepo: AppRepo,
    private val id: Long,
    private val appSettingsRepo: AppSettingsRepo
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    private val packageManager = context.packageManager

    var state by mutableStateOf(ApplyViewState())

    fun dispatch(action: ApplyViewAction) {
        when (action) {
            ApplyViewAction.Init -> init()
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

    private var inited = false

    private fun init() {
        if (inited) return
        inited = true
        loadAndObserveSettings()
        loadApps()
        collectAppEntities()
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
            val list = packageManager.getCompatInstalledPackages(0).map {
                ApplyViewApp(
                    packageName = it.packageName,
                    versionName = it.versionName,
                    versionCode = it.compatVersionCode,
                    firstInstallTime = it.firstInstallTime,
                    lastUpdateTime = it.lastUpdateTime,
                    isSystemApp = it.applicationInfo!!.flags.hasFlag(ApplicationInfo.FLAG_SYSTEM),
                    label = it.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
                )
            }
            state = state.copy(
                apps = state.apps.copy(
                    data = list, progress = ViewContent.Progress.Loaded
                )
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
        viewModelScope.launch(Dispatchers.IO) {
            // Renamed 'entity' to 'model' for architectural clarity
            val model = appRepo.findByPackageName(packageName)

            if (applied) {
                if (model != null) {
                    // Use copy() to safely create a new instance with the updated configId
                    val updatedModel = model.copy(configId = id)
                    appRepo.update(updatedModel)
                } else {
                    // Instantiate AppModel instead of AppEntity
                    appRepo.insert(
                        AppModel(
                            id = 0L, // Assuming 0L triggers auto-generate in Room when mapped back
                            packageName = packageName,
                            configId = id,
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                model?.let { appRepo.delete(it) }
            }
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