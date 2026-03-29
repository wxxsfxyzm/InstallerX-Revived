// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.apply

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.engine.repository.AppIconRepository.Companion.SETTINGS_APP_LIST
import com.rosan.installer.domain.engine.usecase.ClearAppIconCacheUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.provider.SystemAppProvider
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.time.Duration.Companion.seconds

class ApplyViewModel(
    private val id: Long,
    appSettingsRepo: AppSettingsRepository,
    private val appRepo: AppRepository,
    private val systemAppProvider: SystemAppProvider,
    private val toggleAppTargetConfig: ToggleAppTargetConfigUseCase,
    private val updateSetting: UpdateSettingUseCase,
    private val getAppIcon: GetAppIconUseCase,
    private val clearAppIconCache: ClearAppIconCacheUseCase
) : ViewModel(), KoinComponent {

    // Internal mutable flows for data that are not saved in the settings repository
    private val _apps = MutableStateFlow<ViewContent<List<ApplyViewApp>>>(
        ViewContent(data = emptyList(), progress = ViewContent.Progress.Loading)
    )
    private val _appEntities = MutableStateFlow<ViewContent<List<AppModel>>>(
        ViewContent(data = emptyList(), progress = ViewContent.Progress.Loading)
    )
    private val _search = MutableStateFlow("")

    // Map to hold loaded icons as ImageBitmap for Compose
    private val _displayIcons = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    private val iconJobs = mutableMapOf<String, Job>()

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

    // Heavy lifting: filter and sort apps on a background thread to prevent UI stuttering
    private val processedAppsFlow = combine(
        _apps, _appEntities, _search, applyPrefsFlow
    ) { appsState, entitiesState, searchQuery, prefs ->
        val rawApps = appsState.data
        if (rawApps.isEmpty()) return@combine emptyList<ApplyViewApp>()

        // Pre-build a set for O(1) lookup performance
        val selectedPackages = entitiesState.data.mapNotNull { it.packageName }.toSet()
        val orderType = runCatching { ApplyViewState.OrderType.valueOf(prefs.orderTypeStr) }
            .getOrDefault(ApplyViewState.OrderType.Label)

        // Filter apps based on settings and search query
        val filtered = rawApps.filter { app ->
            val matchSystem = prefs.showSystemApp || !app.isSystemApp
            val matchSearch = searchQuery.isEmpty() ||
                    app.packageName.contains(searchQuery, ignoreCase = true) ||
                    (app.label?.contains(searchQuery, ignoreCase = true) == true)
            matchSystem && matchSearch
        }

        // Build sorting comparators
        val comparators = mutableListOf<(ApplyViewApp) -> Comparable<*>?>()

        // Priority 1: Selected apps first
        if (prefs.selectedFirst) {
            comparators.add { app -> if (selectedPackages.contains(app.packageName)) 0 else 1 }
        }

        val getProperty: (ApplyViewApp, ApplyViewState.OrderType) -> Comparable<*>? = { app, type ->
            when (type) {
                ApplyViewState.OrderType.Label -> app.label ?: ""
                ApplyViewState.OrderType.PackageName -> app.packageName
                ApplyViewState.OrderType.FirstInstallTime -> app.firstInstallTime
            }
        }

        // Priority 2: Primary selected sort order
        comparators.add { app -> getProperty(app, orderType) }

        // Priority 3: Fallback properties to maintain deterministic ordering
        ApplyViewState.OrderType.entries.filter { it != orderType }.forEach { fallbackType ->
            comparators.add { app -> getProperty(app, fallbackType) }
        }

        // Execute sorting
        var finalComparator = compareBy(*comparators.toTypedArray())
        if (prefs.orderInReverse) {
            finalComparator = finalComparator.reversed()
        }

        filtered.sortedWith(finalComparator)
    }.flowOn(Dispatchers.Default)

    // Intermediate state to avoid exceeding combine's argument limit
    private data class UiStateData(
        val applyPrefs: ApplyPrefs,
        val useBlur: Boolean,
        val search: String,
        val icons: Map<String, ImageBitmap?>
    )

    private val uiStateFlow = combine(
        applyPrefsFlow,
        appSettingsRepo.preferencesFlow,
        _search,
        _displayIcons
    ) { prefs, globalPrefs, search, icons ->
        UiStateData(prefs, globalPrefs.useBlur, search, icons)
    }

    // Combine all flows to generate the final UI state
    val state: StateFlow<ApplyViewState> = combine(
        uiStateFlow,
        _apps,
        _appEntities,
        processedAppsFlow
    ) { uiData, apps, appEntities, checkedApps ->
        val orderType = runCatching { ApplyViewState.OrderType.valueOf(uiData.applyPrefs.orderTypeStr) }
            .getOrDefault(ApplyViewState.OrderType.Label)

        ApplyViewState(
            apps = apps,
            appEntities = appEntities,
            checkedApps = checkedApps,
            displayIcons = uiData.icons,
            orderType = orderType,
            orderInReverse = uiData.applyPrefs.orderInReverse,
            selectedFirst = uiData.applyPrefs.selectedFirst,
            showSystemApp = uiData.applyPrefs.showSystemApp,
            showPackageName = uiData.applyPrefs.showPackageName,
            useBlur = uiData.useBlur,
            search = uiData.search
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
            is ApplyViewAction.LoadIcon -> loadAppIcon(action.packageName)
            is ApplyViewAction.ApplyPackageName -> applyPackageName(action.packageName, action.applied)
            is ApplyViewAction.Order -> viewModelScope.launch { updateSetting(StringSetting.ApplyOrderType, action.type.name) }
            is ApplyViewAction.OrderInReverse -> viewModelScope.launch { updateSetting(BooleanSetting.ApplyOrderInReverse, action.enabled) }
            is ApplyViewAction.SelectedFirst -> viewModelScope.launch { updateSetting(BooleanSetting.ApplySelectedFirst, action.enabled) }
            is ApplyViewAction.ShowSystemApp -> viewModelScope.launch { updateSetting(BooleanSetting.ApplyShowSystemApp, action.enabled) }
            is ApplyViewAction.ShowPackageName -> viewModelScope.launch { updateSetting(BooleanSetting.ApplyShowPackageName, action.enabled) }
            is ApplyViewAction.Search -> _search.value = action.text
        }
    }

    private var loadAppsJob: Job? = null

    private fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch(Dispatchers.IO) {
            _apps.value = _apps.value.copy(progress = ViewContent.Progress.Loading)
            clearAppIconCache(sessionId = SETTINGS_APP_LIST)
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
            toggleAppTargetConfig(packageName, id, applied)
        }
    }

    private fun loadAppIcon(packageName: String) {
        if (_displayIcons.value.containsKey(packageName) || iconJobs[packageName]?.isActive == true) return

        // Set initial state to null to indicate loading
        _displayIcons.update { it + (packageName to null) }

        iconJobs[packageName] = viewModelScope.launch(Dispatchers.IO) {
            val iconBitmap = getAppIcon(
                sessionId = SETTINGS_APP_LIST,
                packageName = packageName,
                iconSizePx = 144,
                preferSystemIcon = true
            )

            val finalImageBitmap = iconBitmap?.asImageBitmap()

            _displayIcons.update { it + (packageName to finalImageBitmap) }
        }
    }
}
