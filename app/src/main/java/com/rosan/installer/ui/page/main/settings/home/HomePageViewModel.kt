// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.RootMode
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomePageViewModel(
    private val appSettingsRepo: AppSettingsRepository,
    private val capabilityProvider: DeviceCapabilityProvider,
    private val privilegedProvider: PrivilegedProvider,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    // Trigger flow to manually refresh the combined state
    private val refreshFlow = MutableStateFlow(0)

    // UI events stream to communicate one-off actions to the UI layer
    private val _uiEvents = MutableSharedFlow<HomePageViewEvent>(
        extraBufferCapacity = 1
    )
    val uiEvents = _uiEvents.asSharedFlow()

    // Internal data class to bundle all privilege states efficiently
    private data class CapabilityState(
        val shizukuMode: ShizukuMode,
        val shizukuAuthorized: Boolean,
        val dhizukuAvailable: Boolean,
        val dhizukuAuthorized: Boolean,
        val rootMode: RootMode,
        val defaultInstaller: String
    )

    // Combine all capability flows into a single bundled state flow
    private val capabilityStateFlow = combine<Any, CapabilityState>(
        capabilityProvider.shizukuModeFlow,
        capabilityProvider.shizukuAuthorizedFlow,
        capabilityProvider.dhizukuAvailableFlow,
        capabilityProvider.dhizukuAuthorizedFlow,
        capabilityProvider.rootModeFlow,
        capabilityProvider.defaultInstallerFlow
    ) { array ->
        CapabilityState(
            shizukuMode = array[0] as ShizukuMode,
            shizukuAuthorized = array[1] as Boolean,
            dhizukuAvailable = array[2] as Boolean,
            dhizukuAuthorized = array[3] as Boolean,
            rootMode = array[4] as RootMode,
            defaultInstaller = array[5] as String
        )
    }

    // Main UI state flow emitting MainPageViewState
    val state: StateFlow<HomePageViewState> = combine(
        appSettingsRepo.preferencesFlow,
        capabilityStateFlow,
        refreshFlow
    ) { prefs, caps, _ ->

        // Sync user preference with capability provider
        capabilityProvider.isLSPosedActive = prefs.userSetLSPosedActive

        val isDefault = capabilityProvider.isDefaultInstaller
        val shizukuAvailable = caps.shizukuMode != ShizukuMode.NONE

        // Calculate total available authorization methods
        var availableCount = 0
        if (shizukuAvailable && caps.shizukuAuthorized) availableCount++
        if (caps.dhizukuAvailable && caps.dhizukuAuthorized) availableCount++
        if (caps.rootMode != RootMode.None) availableCount++
        if (capabilityProvider.isSystemApp) availableCount++

        val customizeAuthorizer = if (prefs.authorizer == Authorizer.Customize) prefs.customizeAuthorizer else ""

        HomePageViewState(
            globalAuthorizer = prefs.authorizer,
            customizeAuthorizer = customizeAuthorizer,
            isDefaultInstaller = isDefault,

            shizukuAvailable = shizukuAvailable,
            shizukuAuthorized = caps.shizukuAuthorized,
            shizukuMode = caps.shizukuMode,

            dhizukuAvailable = caps.dhizukuAvailable,
            dhizukuAuthorized = caps.dhizukuAuthorized,

            rootMode = caps.rootMode,
            isSystemApp = capabilityProvider.isSystemApp,
            availableAuthorizerCount = availableCount,
            userSetLSPosedActive = prefs.userSetLSPosedActive,
            autoLockInstaller = prefs.autoLockInstaller,
            defaultInstaller = caps.defaultInstaller
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomePageViewState()
    )

    // Handle user actions dispatched from the view
    fun dispatch(action: HomePageViewAction) {
        when (action) {
            is HomePageViewAction.RefreshActivateStatus -> {
                // Refresh is safely dispatched to IO thread inside the provider
                capabilityProvider.refreshPrivilegeStatus()
                refreshFlow.value++
            }

            is HomePageViewAction.ChangeAutoLockInstaller -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.AutoLockInstaller,
                    action.autoLockInstaller
                )
            }

            is HomePageViewAction.SetDefaultInstaller -> setDefaultInstaller(action.lock, action)

            is HomePageViewAction.ChangeAuthorizer -> viewModelScope.launch {
                updateSetting(
                    StringSetting.Authorizer,
                    action.authorizer.value
                )
            }

            // 处理新增的 Action
            is HomePageViewAction.ChangeUserSetLSPosedActive -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UserSetLSPosedActive,
                    action.active
                )
            }
        }
    }

    // Attempt to lock or unlock the default installer setting and emit corresponding UI events
    private fun setDefaultInstaller(lock: Boolean, action: HomePageViewAction) = viewModelScope.launch {
        runCatching {
            privilegedProvider.setDefaultInstaller(state.value.globalAuthorizer, lock)
        }.onSuccess {
            val successResId = if (lock) R.string.lock_default_installer_success else R.string.unlock_default_installer_success
            _uiEvents.emit(HomePageViewEvent.ShowDefaultInstallerResult(successResId))
        }.onFailure { e ->
            val errorResId = if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed
            _uiEvents.emit(HomePageViewEvent.ShowDefaultInstallerErrorDetail(errorResId, e, action))
        }
    }

}
