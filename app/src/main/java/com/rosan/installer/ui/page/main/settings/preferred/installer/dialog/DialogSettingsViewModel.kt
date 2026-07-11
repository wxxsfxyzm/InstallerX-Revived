// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DialogSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    val state: StateFlow<DialogSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        DialogSettingsState(
            hideIdenticalComparisons = prefs.hideIdenticalInstallComparisons,
            versionCompareInSingleLine = prefs.versionCompareInSingleLine,
            sdkCompareInMultiLine = prefs.sdkCompareInMultiLine,
            showDialogInstallExtendedMenu = prefs.showDialogInstallExtendedMenu,
            expandTemporarySettingsByDefault = prefs.expandDialogTemporarySettingsByDefault,
            showSmartSuggestion = prefs.showSmartSuggestion,
            autoSilentInstall = prefs.autoSilentInstall,
            longClickBackgroundInstall = prefs.longClickBackgroundInstall,
            disableNotificationForDialogInstall = prefs.disableNotificationForDialogInstall,
            tapIconToShare = prefs.labTapIconToShare,
            showFilePath = prefs.labShowFilePath,
            showInstallInitiator = prefs.labShowInstallInitiator
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DialogSettingsState()
    )

    fun dispatch(action: DialogSettingsAction) {
        when (action) {
            is DialogSettingsAction.ChangeHideIdenticalComparisons -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogHideIdenticalComparisons, action.hide)
            }

            is DialogSettingsAction.ChangeVersionCompareInSingleLine -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogVersionCompareSingleLine, action.compareInSingleLine)
            }

            is DialogSettingsAction.ChangeSdkCompareInMultiLine -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogSdkCompareMultiLine, action.compareInMultiLine)
            }

            is DialogSettingsAction.ChangeShowDialogInstallExtendedMenu -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogShowExtendedMenu, action.showMenu)
            }

            is DialogSettingsAction.ChangeExpandTemporarySettingsByDefault -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogExpandTemporarySettingsByDefault, action.expand)
            }

            is DialogSettingsAction.ChangeShowSuggestion -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogShowIntelligentSuggestion, action.showSuggestion)
            }

            is DialogSettingsAction.ChangeAutoSilentInstall -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogAutoSilentInstall, action.autoSilentInstall)
            }

            is DialogSettingsAction.ChangeLongClickBackgroundInstall -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogLongClickBackgroundInstall, action.enable)
            }

            is DialogSettingsAction.ChangeShowDisableNotification -> viewModelScope.launch {
                updateSetting(BooleanSetting.DialogDisableNotificationOnDismiss, action.disable)
            }

            is DialogSettingsAction.ChangeTapIconToShare -> viewModelScope.launch {
                updateSetting(BooleanSetting.LabTapIconToShare, action.enable)
            }

            is DialogSettingsAction.ChangeShowFilePath -> viewModelScope.launch {
                updateSetting(BooleanSetting.LabShowFilePath, action.enable)
            }

            is DialogSettingsAction.ChangeShowInstallInitiator -> viewModelScope.launch {
                updateSetting(BooleanSetting.LabShowInstallInitiator, action.enable)
            }
        }
    }
}
