// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthorizerCustViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    val state: StateFlow<AuthorizerCustState> = appSettingsRepo.preferencesFlow.map { prefs ->
        AuthorizerCustState(
            authorizer = prefs.authorizer,
            alwaysUseRootInSystem = prefs.alwaysUseRootInSystem,
            closeSessionCountDown = prefs.closeSessionCountDown,
            allowInstallWithoutUserAction = prefs.labInstallWithoutUserAction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AuthorizerCustState()
    )

    fun dispatch(action: AuthorizerCustAction) {
        when (action) {
            is AuthorizerCustAction.ChangeAlwaysUseRootInSystem -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.AlwaysUseRootInSystem,
                    action.alwaysUseRootInSystem
                )
            }

            is AuthorizerCustAction.ChangeCloseSessionCountDown -> {
                if (action.countDown in 1..10) viewModelScope.launch {
                    updateSetting(
                        IntSetting.CloseSessionCountdown,
                        action.countDown
                    )
                }
            }

            is AuthorizerCustAction.ChangeAllowInstallWithoutUserAction -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabInstallWithoutUserAction,
                    action.enable
                )
            }
        }
    }
}
