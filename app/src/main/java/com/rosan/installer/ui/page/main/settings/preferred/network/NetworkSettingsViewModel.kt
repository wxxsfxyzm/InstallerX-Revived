// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    val state: StateFlow<NetworkSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        NetworkSettingsState(
            allowInternetAccess = prefs.allowInternetAccess,
            networkSourceMode = prefs.networkSourceMode,
            networkSourceModeWarningAcknowledged = prefs.networkSourceModeWarningAcknowledged,
            httpProfile = prefs.labHttpProfile,
            githubUpdateChannel = prefs.githubUpdateChannel,
            customGithubProxyUrl = prefs.customGithubProxyUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NetworkSettingsState()
    )

    fun dispatch(action: NetworkSettingsAction) {
        when (action) {
            is NetworkSettingsAction.ChangeInternetAccess -> viewModelScope.launch {
                updateSetting(BooleanSetting.AllowInternetAccess, action.enabled)
            }

            is NetworkSettingsAction.ChangeNetworkSourceMode -> viewModelScope.launch {
                updateSetting(
                    StringSetting.NetworkSourceMode,
                    action.mode.value
                )
            }

            is NetworkSettingsAction.ConfirmNetworkSourceMode -> viewModelScope.launch {
                updateSetting(StringSetting.NetworkSourceMode, action.mode.value)
                updateSetting(BooleanSetting.NetworkSourceModeWarningAcknowledged, true)
            }

            is NetworkSettingsAction.ChangeHttpProfile -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LabHttpProfile,
                    action.profile.name
                )
            }

            is NetworkSettingsAction.ChangeGithubUpdateChannel -> viewModelScope.launch {
                updateSetting(
                    StringSetting.GithubUpdateChannel,
                    action.channel.name
                )
            }

            is NetworkSettingsAction.ChangeCustomGithubProxyUrl -> viewModelScope.launch {
                updateSetting(
                    StringSetting.CustomGithubProxyUrl,
                    action.url
                )
            }
        }
    }
}
