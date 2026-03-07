// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.updater.model.UpdateInfo
import com.rosan.installer.domain.updater.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PreferredViewModel(
    private val appSettingsRepo: AppSettingsRepo,
    private val updateRepo: UpdateRepository,
    private val systemEnvProvider: SystemEnvProvider,
    private val privilegedProvider: PrivilegedProvider
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<PreferredViewEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    // --- External Environment State Flows ---
    private val updateInfoFlow = MutableStateFlow<UpdateInfo?>(null)

    private val adbVerifyEnabledFlow = MutableStateFlow(true)
    private val isIgnoringBatteryOptFlow = MutableStateFlow(true)

    val state: StateFlow<PreferredViewState> = combine(
        appSettingsRepo.preferencesFlow,
        adbVerifyEnabledFlow,
        isIgnoringBatteryOptFlow,
        updateInfoFlow
    ) { prefs, adbVerify, batteryOpt, updateInfo ->
        val customizeAuthorizer = if (prefs.authorizer == Authorizer.Customize) prefs.customizeAuthorizer else ""

        PreferredViewState(
            isLoading = false,
            useBlur = prefs.useBlur,
            authorizer = prefs.authorizer,
            customizeAuthorizer = customizeAuthorizer,
            autoLockInstaller = prefs.autoLockInstaller,
            adbVerifyEnabled = adbVerify,
            isIgnoringBatteryOptimizations = batteryOpt,
            hasUpdate = updateInfo?.hasUpdate ?: false,
            remoteVersion = updateInfo?.remoteVersion ?: ""
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferredViewState(isLoading = true)
    )

    init {
        refreshIgnoreBatteryOptStatus()
        refreshAdbVerifyStatus()
        checkUpdate()
    }

    fun dispatch(action: PreferredViewAction) {
        when (action) {
            is PreferredViewAction.ChangeAutoLockInstaller -> updateSetting(BooleanSetting.AutoLockInstaller, action.autoLockInstaller)
            is PreferredViewAction.SetAdbVerifyEnabledState -> setAdbVerifyEnabled(action.enabled, action)
            is PreferredViewAction.RequestIgnoreBatteryOptimization -> requestIgnoreBatteryOptimization()
            is PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus -> refreshIgnoreBatteryOptStatus()
            is PreferredViewAction.SetDefaultInstaller -> setDefaultInstaller(action.lock, action)
        }
    }

    private fun updateSetting(setting: BooleanSetting, value: Boolean) = viewModelScope.launch { appSettingsRepo.putBoolean(setting, value) }

    private fun requestIgnoreBatteryOptimization() {
        try {
            systemEnvProvider.requestIgnoreBatteryOptimization()
        } catch (_: Exception) {
        }
    }

    private fun refreshIgnoreBatteryOptStatus() = viewModelScope.launch {
        systemEnvProvider.isIgnoringBatteryOptimizationsFlow().collect { isIgnoring ->
            isIgnoringBatteryOptFlow.value = isIgnoring
        }
    }

    private fun setAdbVerifyEnabled(enabled: Boolean, action: PreferredViewAction) = viewModelScope.launch {
        runCatching {
            privilegedProvider.setAdbVerify(state.value.authorizer, state.value.customizeAuthorizer, enabled)
        }.onSuccess {
            adbVerifyEnabledFlow.value = enabled
        }.onFailure { e ->
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail(R.string.disable_adb_install_verify_failed, e, action))
        }
    }

    private fun refreshAdbVerifyStatus() = viewModelScope.launch {
        systemEnvProvider.isAdbVerifyEnabledFlow().collect { enabled ->
            adbVerifyEnabledFlow.value = enabled
        }
    }

    private fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        val result = updateRepo.checkUpdate()
        if (result != null) updateInfoFlow.value = result
    }

    private fun setDefaultInstaller(lock: Boolean, action: PreferredViewAction) = viewModelScope.launch {
        runCatching {
            privilegedProvider.setDefaultInstaller(state.value.authorizer, lock)
        }.onSuccess {
            val successResId = if (lock) R.string.lock_default_installer_success else R.string.unlock_default_installer_success
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerResult(successResId))
        }.onFailure { e ->
            val errorResId = if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail(errorResId, e, action))
        }
    }
}
