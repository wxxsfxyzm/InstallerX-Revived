// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupValidationException
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.usecase.backup.ExportBackupUseCase
import com.rosan.installer.domain.settings.usecase.backup.PrepareBackupRestoreUseCase
import com.rosan.installer.domain.settings.usecase.backup.RestoreBackupUseCase
import com.rosan.installer.domain.settings.usecase.settings.SetLauncherIconUseCase
import com.rosan.installer.domain.settings.usecase.settings.UpdateSettingUseCase
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
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferredViewModel(
    appSettingsRepo: AppSettingsRepository,
    private val updateRepo: UpdateRepository,
    private val systemEnvProvider: SystemEnvProvider,
    private val privilegedProvider: PrivilegedProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val setLauncherIcon: SetLauncherIconUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val prepareBackupRestore: PrepareBackupRestoreUseCase,
    private val restoreBackup: RestoreBackupUseCase
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<PreferredViewEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    private val adbVerifyEnabledFlow = MutableStateFlow(true)
    private val isIgnoringBatteryOptFlow = MutableStateFlow(true)
    private val backupBusyFlow = MutableStateFlow(false)
    private var pendingRestorePreview: BackupRestorePreview? = null

    val state: StateFlow<PreferredViewState> = combine(
        appSettingsRepo.preferencesFlow,
        adbVerifyEnabledFlow,
        isIgnoringBatteryOptFlow,
        updateRepo.updateInfoFlow,
        backupBusyFlow
    ) { prefs, adbVerify, batteryOpt, updateInfo, backupBusy ->
        val customizeAuthorizer = if (prefs.authorizer == Authorizer.Customize) prefs.customizeAuthorizer else ""

        PreferredViewState(
            authorizer = prefs.authorizer,
            customizeAuthorizer = customizeAuthorizer,
            autoLockInstaller = prefs.autoLockInstaller,
            showLauncherIcon = prefs.showLauncherIcon,
            adbVerifyEnabled = adbVerify,
            isIgnoringBatteryOptimizations = batteryOpt,
            hasUpdate = updateInfo?.hasUpdate ?: false,
            remoteVersion = updateInfo?.remoteVersion ?: "",
            backupBusy = backupBusy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferredViewState()
    )

    init {
        refreshIgnoreBatteryOptStatus()
        refreshAdbVerifyStatus()
        checkUpdate()
    }

    fun dispatch(action: PreferredViewAction) {
        when (action) {
            is PreferredViewAction.ChangeAutoLockInstaller -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.AutoLockInstaller,
                    action.autoLockInstaller
                )
            }

            is PreferredViewAction.SetAdbVerifyEnabledState -> setAdbVerifyEnabled(action.enabled, action)
            is PreferredViewAction.RequestIgnoreBatteryOptimization -> requestIgnoreBatteryOptimization()
            is PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus -> refreshIgnoreBatteryOptStatus()
            is PreferredViewAction.ChangeShowLauncherIcon -> viewModelScope.launch {
                setLauncherIcon(action.showLauncherIcon)
            }
            is PreferredViewAction.SetDefaultInstaller -> setDefaultInstaller(action.lock, action)
            is PreferredViewAction.RequestExportBackup -> requestExportBackup()
            is PreferredViewAction.PrepareRestoreBackup -> prepareRestoreBackup(action.rawJson)
            PreferredViewAction.ConfirmRestoreBackup -> confirmRestoreBackup()
        }
    }

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
            Timber.e(e, "Failed to set ADB install verification to $enabled")
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail(R.string.disable_adb_install_verify_failed, e, action))
        }
    }

    private fun refreshAdbVerifyStatus() = viewModelScope.launch {
        systemEnvProvider.isAdbVerifyEnabledFlow().collect { enabled ->
            adbVerifyEnabledFlow.value = enabled
        }
    }

    private fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        updateRepo.checkUpdate()
    }

    private fun setDefaultInstaller(lock: Boolean, action: PreferredViewAction) = viewModelScope.launch {
        runCatching {
            privilegedProvider.setDefaultInstaller(state.value.authorizer, lock)
        }.onSuccess {
            val successResId = if (lock) R.string.lock_default_installer_success else R.string.unlock_default_installer_success
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerResult(successResId))
        }.onFailure { e ->
            val errorResId = if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed
            Timber.e(e, "Failed to ${if (lock) "lock" else "unlock"} default installer")
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail(errorResId, e, action))
        }
    }

    private fun requestExportBackup() = viewModelScope.launch(Dispatchers.IO) {
        if (backupBusyFlow.value) return@launch
        backupBusyFlow.value = true
        try {
            runCatching {
                PreferredViewEvent.LaunchBackupExport(
                    fileName = buildBackupFileName(),
                    content = exportBackup()
                )
            }.onSuccess { event ->
                _uiEvents.emit(event)
            }.onFailure { e ->
                Timber.e(e, "Failed to export backup.")
                _uiEvents.emit(PreferredViewEvent.ShowBackupError(R.string.backup_settings_export_failed, e))
            }
        } finally {
            backupBusyFlow.value = false
        }
    }

    private fun prepareRestoreBackup(rawJson: String) = viewModelScope.launch(Dispatchers.IO) {
        if (backupBusyFlow.value) return@launch
        backupBusyFlow.value = true
        try {
            runCatching {
                prepareBackupRestore(rawJson)
            }.onSuccess { preview ->
                pendingRestorePreview = preview
                _uiEvents.emit(PreferredViewEvent.ShowBackupRestorePreview(preview))
            }.onFailure { e ->
                pendingRestorePreview = null
                if (e is BackupValidationException) {
                    _uiEvents.emit(PreferredViewEvent.ShowBackupValidationError(e.issues))
                } else {
                    Timber.e(e, "Failed to prepare backup restore.")
                    _uiEvents.emit(PreferredViewEvent.ShowBackupError(R.string.backup_settings_restore_failed, e))
                }
            }
        } finally {
            backupBusyFlow.value = false
        }
    }

    private fun confirmRestoreBackup() = viewModelScope.launch(Dispatchers.IO) {
        if (backupBusyFlow.value) return@launch
        val preview = pendingRestorePreview ?: return@launch
        backupBusyFlow.value = true
        try {
            runCatching {
                restoreBackup(preview.envelope)
            }.onSuccess {
                pendingRestorePreview = null
                _uiEvents.emit(PreferredViewEvent.ShowBackupMessage(R.string.backup_settings_restore_success))
            }.onFailure { e ->
                if (e is BackupValidationException) {
                    _uiEvents.emit(PreferredViewEvent.ShowBackupValidationError(e.issues))
                } else {
                    Timber.e(e, "Failed to restore backup.")
                    _uiEvents.emit(PreferredViewEvent.ShowBackupError(R.string.backup_settings_restore_failed, e))
                }
            }
        } finally {
            backupBusyFlow.value = false
        }
    }

    private fun buildBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "InstallerX-Revived-backup-$timestamp.installerx-backup.json"
    }
}
