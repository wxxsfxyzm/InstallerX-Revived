// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabSettingsViewModel(
    appSettingsRepo: AppSettingsRepo,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    val state: StateFlow<LabSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        LabSettingsState(
            useBlur = prefs.useBlur,
            labRootEnableModuleFlash = prefs.labRootEnableModuleFlash,
            labRootShowModuleArt = prefs.labRootShowModuleArt,
            labRootModuleAlwaysUseRoot = prefs.labRootModuleAlwaysUseRoot,
            labRootImplementation = prefs.labRootImplementation,
            labUseMiIsland = prefs.labUseMiIsland,
            labSetInstallRequester = prefs.labSetInstallRequester,
            labHttpProfile = prefs.labHttpProfile,
            labHttpSaveFile = prefs.labHttpSaveFile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
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

            is LabSettingsAction.LabChangeRootModuleAlwaysUseRoot -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabModuleAlwaysRoot,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeRootImplementation -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LabRootImplementation,
                    action.implementation.name
                )
            }

            is LabSettingsAction.LabChangeUseMiIsland -> viewModelScope.launch { updateSetting(BooleanSetting.ShowMiIsland, action.enable) }
            is LabSettingsAction.LabChangeSetInstallRequester -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.LabSetInstallRequester,
                    action.enable
                )
            }

            is LabSettingsAction.LabChangeHttpProfile -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LabHttpProfile,
                    action.profile.name
                )
            }

            is LabSettingsAction.LabChangeHttpSaveFile -> viewModelScope.launch { updateSetting(BooleanSetting.LabHttpSaveFile, action.enable) }
        }
    }
}
