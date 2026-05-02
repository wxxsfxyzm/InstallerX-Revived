// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import com.rosan.installer.framework.accessibility.AuxiliaryInstallAccessibilityService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuxiliaryInstallSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<AuxiliaryInstallSettingsEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    private val accessibilityServiceEnabledFlow = MutableStateFlow(false)

    val state: StateFlow<AuxiliaryInstallSettingsState> = combine(
        appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallAutoConfirmUsb, false),
        appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallShowToast, false),
        appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallDelayedRetry, true),
        appSettingsRepo.getBoolean(BooleanSetting.AuxiliaryInstallRequireScreenOn, true),
        accessibilityServiceEnabledFlow
    ) { autoConfirmUsbInstall, showToast, delayedRetry, requireScreenOn, accessibilityServiceEnabled ->
        AuxiliaryInstallSettingsState(
            autoConfirmUsbInstall = autoConfirmUsbInstall,
            showToast = showToast,
            delayedRetry = delayedRetry,
            requireScreenOn = requireScreenOn,
            accessibilityServiceEnabled = accessibilityServiceEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuxiliaryInstallSettingsState()
    )

    init {
        refreshAccessibilityServiceStatus()
    }

    fun dispatch(action: AuxiliaryInstallSettingsAction) {
        when (action) {
            AuxiliaryInstallSettingsAction.OpenAccessibilitySettings -> openAccessibilitySettings()
            AuxiliaryInstallSettingsAction.RefreshAccessibilityServiceStatus -> refreshAccessibilityServiceStatus()
            is AuxiliaryInstallSettingsAction.ChangeAutoConfirmUsbInstall ->
                changeAutoConfirmUsbInstall(action.enabled)
            is AuxiliaryInstallSettingsAction.ChangeShowToast ->
                updateBooleanSetting(BooleanSetting.AuxiliaryInstallShowToast, action.enabled)
            is AuxiliaryInstallSettingsAction.ChangeDelayedRetry ->
                updateBooleanSetting(BooleanSetting.AuxiliaryInstallDelayedRetry, action.enabled)
            is AuxiliaryInstallSettingsAction.ChangeRequireScreenOn ->
                updateBooleanSetting(BooleanSetting.AuxiliaryInstallRequireScreenOn, action.enabled)
        }
    }

    private fun openAccessibilitySettings() {
        runCatching { systemEnvProvider.openAccessibilitySettings() }
    }

    private fun refreshAccessibilityServiceStatus() {
        accessibilityServiceEnabledFlow.value =
            systemEnvProvider.isAccessibilityServiceEnabled(AuxiliaryInstallAccessibilityService::class.java.name)
    }

    private fun changeAutoConfirmUsbInstall(enabled: Boolean) = viewModelScope.launch {
        refreshAccessibilityServiceStatus()
        if (enabled && !accessibilityServiceEnabledFlow.value) {
            _uiEvents.emit(
                AuxiliaryInstallSettingsEvent.ShowMessage(R.string.auxiliary_install_accessibility_required)
            )
            openAccessibilitySettings()
            return@launch
        }
        updateSetting(BooleanSetting.AuxiliaryInstallAutoConfirmUsb, enabled)
    }

    private fun updateBooleanSetting(setting: BooleanSetting, enabled: Boolean) = viewModelScope.launch {
        updateSetting(setting, enabled)
    }
}
