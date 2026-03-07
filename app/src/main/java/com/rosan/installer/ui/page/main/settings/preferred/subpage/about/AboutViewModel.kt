// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.subpage.about

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
import com.rosan.installer.domain.updater.model.UpdateInfo
import com.rosan.installer.domain.updater.repository.UpdateRepository
import com.rosan.installer.domain.updater.usecase.PerformAppUpdateUseCase
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
import timber.log.Timber

class AboutViewModel(
    appSettingsRepo: AppSettingsRepo,
    private val updateRepo: UpdateRepository,
    private val systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val performAppUpdateUseCase: PerformAppUpdateUseCase,
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<AboutEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    private val updateInfoFlow = MutableStateFlow<UpdateInfo?>(null)

    val state: StateFlow<AboutState> = combine(
        appSettingsRepo.preferencesFlow,
        updateInfoFlow
    ) { prefs, updateInfo ->
        AboutState(
            useBlur = prefs.useBlur,
            authorizer = prefs.authorizer,
            hasUpdate = updateInfo?.hasUpdate ?: false,
            remoteVersion = updateInfo?.remoteVersion ?: "",
            enableFileLogging = prefs.enableFileLogging
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AboutState()
    )

    init {
        checkUpdate()
    }

    fun dispatch(action: AboutAction) {
        when (action) {
            is AboutAction.PerformUpdate -> performInAppUpdate()
            is AboutAction.SetEnableFileLogging -> setEnableFileLogging(action.enable)
            is AboutAction.ShareLog -> shareLog()
        }
    }

    private fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        val result = updateRepo.checkUpdate()
        if (result != null) updateInfoFlow.value = result
    }

    private fun performInAppUpdate() = viewModelScope.launch {
        _uiEvents.emit(AboutEvent.ShowUpdateLoading)
        runCatching {
            val config = ConfigModel.default.copy(authorizer = state.value.authorizer)
            performAppUpdateUseCase(updateInfoFlow.value, config)
        }.onFailure { e ->
            Timber.e(e, "In-app update failed")
            _uiEvents.emit(AboutEvent.ShowInAppUpdateErrorDetail("Update Failed", e))
        }
        _uiEvents.emit(AboutEvent.HideUpdateLoading)
    }

    private fun setEnableFileLogging(enable: Boolean) = viewModelScope.launch {
        updateSetting(BooleanSetting.EnableFileLogging, enable)
    }

    private fun shareLog() = viewModelScope.launch {
        try {
            val uriStr = systemEnvProvider.getLatestLogUri()
            if (uriStr == null) {
                _uiEvents.emit(AboutEvent.ShareLogFailed("Log file is missing or empty"))
            } else {
                _uiEvents.emit(AboutEvent.OpenLogShare(uriStr.toUri()))
            }
        } catch (e: Exception) {
            _uiEvents.emit(AboutEvent.ShareLogFailed(e.message ?: "Failed"))
        }
    }
}
