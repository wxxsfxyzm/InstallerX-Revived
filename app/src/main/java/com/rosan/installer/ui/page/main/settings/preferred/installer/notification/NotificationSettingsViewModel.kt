// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.installer.notification

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

class NotificationSettingsViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateSetting: UpdateSettingUseCase
) : ViewModel() {

    val state: StateFlow<NotificationSettingsState> = appSettingsRepo.preferencesFlow.map { prefs ->
        NotificationSettingsState(
            showLiveActivity = prefs.showLiveActivity,
            showMiIsland = prefs.useMiIsland,
            miIslandBypassRestriction = prefs.useMiIslandBypassRestriction,
            miIslandOuterGlow = prefs.useMiIslandOuterGlow,
            successAutoClearSeconds = prefs.notificationSuccessAutoClearSeconds,
            showDialogOnPress = prefs.showDialogWhenPressingNotification,
            miIslandBlockingInterval = prefs.useMiIslandBlockingIntervalMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationSettingsState()
    )

    fun dispatch(action: NotificationSettingsAction) {
        when (action) {
            is NotificationSettingsAction.ChangeStyle -> viewModelScope.launch {
                when (action.style) {
                    NotificationStyle.STANDARD -> {
                        updateSetting(BooleanSetting.ShowLiveActivity, false)
                        updateSetting(BooleanSetting.ShowMiIsland, false)
                    }

                    NotificationStyle.LIVE_ACTIVITY -> {
                        updateSetting(BooleanSetting.ShowLiveActivity, true)
                        updateSetting(BooleanSetting.ShowMiIsland, false)
                    }

                    NotificationStyle.MI_ISLAND -> {
                        updateSetting(BooleanSetting.ShowLiveActivity, false)
                        updateSetting(BooleanSetting.ShowMiIsland, true)
                    }
                }
            }

            is NotificationSettingsAction.ChangeAutoClearSeconds -> viewModelScope.launch {
                updateSetting(IntSetting.NotificationSuccessAutoClearSeconds, action.seconds)
            }

            is NotificationSettingsAction.ChangeShowDialogOnPress -> viewModelScope.launch {
                updateSetting(BooleanSetting.ShowDialogWhenPressingNotification, action.show)
            }

            is NotificationSettingsAction.ChangeMiIslandBypassRestriction -> viewModelScope.launch {
                updateSetting(BooleanSetting.ShowMiIslandBypassRestriction, action.bypass)
            }

            is NotificationSettingsAction.ChangeMiIslandOuterGlow -> viewModelScope.launch {
                updateSetting(BooleanSetting.ShowMiIslandOuterGlow, action.glow)
            }

            is NotificationSettingsAction.ChangeMiIslandBlockingInterval -> viewModelScope.launch {
                updateSetting(IntSetting.ShowMiIslandBlockingInterval, action.ms)
            }
        }
    }
}
