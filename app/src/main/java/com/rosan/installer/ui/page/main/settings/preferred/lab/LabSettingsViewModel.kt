// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerPreferences
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    val state: StateFlow<LabSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        LabSettingsState(
            labRootEnableModuleFlash = prefs.labRootEnableModuleFlash,
            labRootShowModuleArt = prefs.labRootShowModuleArt,
            labRootMode = prefs.labRootMode,
            labHttpProfile = prefs.labHttpProfile,
            labHttpSaveFile = prefs.labHttpSaveFile,
            labAllowInstallWithoutUserAction = prefs.labInstallWithoutUserAction,
            labRespectPlatformInstallPolicy = prefs.labRespectPlatformInstallPolicy,
            tryMultipleAuthorizersOnInstall = prefs.tryMultipleAuthorizersOnInstall,
            smartAuthorizerCandidates = prefs.smartAuthorizerCandidates,
            githubUpdateChannel = prefs.githubUpdateChannel,
            customGithubProxyUrl = prefs.customGithubProxyUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LabSettingsState()
    )

    fun dispatch(action: LabSettingsAction) {
        when (action) {
            is LabSettingsAction.LabChangeRootModuleFlash -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabEnableModuleFlash,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeRootShowModuleArt -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabModuleFlashShowArt,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeRootImplementation -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LabRootImplementation,
                    action.implementation.name
                )
            }

            is LabSettingsAction.LabChangeHttpProfile -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LabHttpProfile,
                    action.profile.name
                )
            }

            is LabSettingsAction.LabChangeHttpSaveFile -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabHttpSaveFile,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeAllowInstallWithoutUserAction -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabInstallWithoutUserAction,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeRespectPlatformInstallPolicy -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabRespectPlatformInstallPolicy,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeTryMultipleAuthorizersOnInstall -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.TryMultipleAuthorizersOnInstall,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeSmartAuthorizerCandidates -> viewModelScope.launch {
                updateSetting(
                    StringSetting.SmartAuthorizerCandidates,
                    SmartAuthorizerPreferences.encode(action.candidates)
                )
            }

            is LabSettingsAction.LabChangeGithubUpdateChannel -> viewModelScope.launch {
                updateSetting(
                    StringSetting.GithubUpdateChannel,
                    action.channel.name
                )
            }

            is LabSettingsAction.LabChangeCustomGithubProxyUrl -> viewModelScope.launch {
                updateSetting(
                    StringSetting.CustomGithubProxyUrl,
                    action.url
                )
            }
        }
    }
}
