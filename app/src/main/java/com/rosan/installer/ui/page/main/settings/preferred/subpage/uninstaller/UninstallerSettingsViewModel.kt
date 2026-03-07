// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.uninstaller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.usecase.settings.ToggleUninstallFlagUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UninstallerSettingsViewModel(
    appSettingsRepo: AppSettingsRepo,
    private val systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val toggleUninstallFlagUseCase: ToggleUninstallFlagUseCase
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<UninstallerSettingsEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    val state: StateFlow<UninstallerSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        UninstallerSettingsState(
            useBlur = prefs.useBlur,
            authorizer = prefs.authorizer,
            uninstallFlags = prefs.uninstallFlags,
            uninstallerRequireBiometricAuth = prefs.uninstallerRequireBiometricAuth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UninstallerSettingsState()
    )

    fun dispatch(action: UninstallerSettingsAction) {
        when (action) {
            is UninstallerSettingsAction.ToggleGlobalUninstallFlag -> toggleGlobalUninstallFlag(action.flag, action.enable)
            is UninstallerSettingsAction.ChangeBiometricAuth -> changeBiometricAuth(action.require)
        }
    }

    private fun changeBiometricAuth(biometricAuth: Boolean) = viewModelScope.launch {
        if (systemEnvProvider.authenticateBiometric(isInstaller = false)) {
            updateSetting(BooleanSetting.UninstallerRequireBiometricAuth, biometricAuth)
        }
    }

    private fun toggleGlobalUninstallFlag(flag: Int, enable: Boolean) = viewModelScope.launch {
        val disabledFlag = toggleUninstallFlagUseCase(flag, enable)
        if (disabledFlag != null) {
            val resId = if (disabledFlag == PackageManagerUtil.DELETE_SYSTEM_APP)
                R.string.uninstall_system_app_disabled
            else
                R.string.uninstall_all_users_disabled
            _uiEvents.tryEmit(UninstallerSettingsEvent.ShowMessage(resId))
        }
    }
}
