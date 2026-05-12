// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.DexoptMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.InstallReason
import com.rosan.installer.domain.settings.model.InstallerMode
import com.rosan.installer.domain.settings.model.PackageSource
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.usecase.config.GetConfigDraftUseCase
import com.rosan.installer.domain.settings.usecase.config.SaveConfigUseCase
import com.rosan.installer.domain.settings.usecase.settings.GetPackageUidUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class EditViewModel(
    private val id: Long? = null,
    private val appSettingsRepo: AppSettingsRepository,
    private val getConfigDraft: GetConfigDraftUseCase,
    private val saveConfig: SaveConfigUseCase,
    private val getAvailableUsers: GetAvailableUsersUseCase,
    private val getPackageUid: GetPackageUidUseCase
) : ViewModel() {

    // Separate mutable states for editable data to combine later
    private val _data = MutableStateFlow(EditViewState.Data.build(ConfigModel.default))
    private val _originalData = MutableStateFlow<EditViewState.Data?>(null)
    private val _availableUsers = MutableStateFlow<Map<Int, String>>(emptyMap())

    // Combine local mutable states with reactive data store flows
    val state: StateFlow<EditViewState> = combine(
        _data,
        _originalData,
        _availableUsers,
        combine(
            appSettingsRepo.preferencesFlow,
            appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedInstallerPackages),
            appSettingsRepo.getBoolean(BooleanSetting.LabSetInstallRequester),
            ::Triple
        )
    ) { data, originalData, availableUsers, settings ->
        val (prefs, managedInstallerPackages, isCustomInstallRequesterEnabled) = settings
        EditViewState(
            data = data,
            originalData = originalData,
            availableUsers = availableUsers,
            managedInstallerPackages = managedInstallerPackages,
            isCustomInstallRequesterEnabled = isCustomInstallRequesterEnabled,
            globalAuthorizer = prefs.authorizer,
            globalInstallerBiometricAuthMode = prefs.installerRequireBiometricAuth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EditViewState()
    )

    private val _eventFlow = MutableSharedFlow<EditViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    fun dispatch(action: EditViewAction) {
        Timber.i("[DISPATCH] Action received: ${action::class.simpleName}")
        viewModelScope.launch {
            val errorMessage = runCatching {
                when (action) {
                    is EditViewAction.ChangeDataName -> changeDataName(action.name)
                    is EditViewAction.ChangeDataDescription -> changeDataDescription(action.description)
                    is EditViewAction.ChangeDataAuthorizer -> changeDataAuthorizer(action.authorizer)
                    is EditViewAction.ChangeDataCustomizeAuthorizer -> changeDataCustomizeAuthorizer(action.customizeAuthorizer)
                    is EditViewAction.ChangeDataInstallMode -> changeDataInstallMode(action.installMode)
                    is EditViewAction.ChangeDataShowToast -> changeDataShowToast(action.showToast)
                    is EditViewAction.ChangeDataEnableCustomizePackageSource -> changeDataEnableCustomPackageSource(action.enable)
                    is EditViewAction.ChangeDataPackageSource -> changeDataPackageSource(action.packageSource)
                    is EditViewAction.ChangeDataEnableCustomizeInstallReason -> changeDataEnableCustomInstallReason(action.enable)
                    is EditViewAction.ChangeDataInstallReason -> changeDataInstallReason(action.installReason)
                    is EditViewAction.ChangeDataEnableCustomizeInstallRequester -> changeDataEnableCustomInstallRequester(action.enable)
                    is EditViewAction.ChangeDataInstallRequester -> changeDataInstallRequester(action.packageName)
                    is EditViewAction.ChangeDataInstallerMode -> changeDataInstallerMode(action.installerMode)
                    is EditViewAction.ChangeDataInstaller -> changeDataInstaller(action.installer)
                    is EditViewAction.ChangeDataCustomizeUser -> changeDataCustomizeUser(action.enable)
                    is EditViewAction.ChangeDataTargetUserId -> changeDataTargetUserId(action.userId)
                    is EditViewAction.ChangeDataEnableManualDexopt -> changeDataEnableManualDexopt(action.enable)
                    is EditViewAction.ChangeDataForceDexopt -> changeDataForceDexopt(action.force)
                    is EditViewAction.ChangeDataDexoptMode -> changeDataDexoptMode(action.mode)
                    is EditViewAction.ChangeDataAutoDelete -> changeDataAutoDelete(action.autoDelete)
                    is EditViewAction.ChangeDataZipAutoDelete -> changeDataZipAutoDelete(action.autoDelete)
                    is EditViewAction.ChangeDisplaySdk -> changeDisplaySdk(action.displaySdk)
                    is EditViewAction.ChangeDisplaySize -> changeDisplaySize(action.displaySize)
                    is EditViewAction.ChangeDataForAllUser -> changeDataForAllUser(action.forAllUser)
                    is EditViewAction.ChangeDataAllowTestOnly -> changeDataAllowTestOnly(action.allowTestOnly)
                    is EditViewAction.ChangeDataAllowDowngrade -> changeDataAllowDowngrade(action.allowDowngrade)
                    is EditViewAction.ChangeDataBypassLowTargetSdk -> changeDataBypassLowTargetSdk(action.bypassLowTargetSdk)
                    is EditViewAction.ChangeDataAllowSigMismatch -> changeDataAllowSigMismatch(action.allowSigMismatch)
                    is EditViewAction.ChangeDataAllowSigUnknown -> changeDataAllowSigUnknown(action.allowSigUnknown)
                    is EditViewAction.ChangeDataAllowAllRequestedPermissions -> changeDataAllowAllRequestedPermissions(action.allowAllRequestedPermissions)
                    is EditViewAction.ChangeDataRequestUpdateOwnership -> changeDataRequestUpdateOwnership(action.requestUpdateOwnership)
                    is EditViewAction.ChangeSplitChooseAll -> changeSplitChooseAll(action.splitChooseAll)
                    is EditViewAction.ChangeApkChooseAll -> changeApkChooseAll(action.apkChooseAll)
                    is EditViewAction.ChangeRequireBiometricAuth -> changeRequireBiometricAuth(action.require)
                    is EditViewAction.LoadData -> loadData()
                    is EditViewAction.SaveData -> saveData()
                }
            }.exceptionOrNull()?.message

            if (errorMessage != null) {
                _eventFlow.emit(EditViewEvent.SnackBar(message = errorMessage))
            }
        }
    }

    private fun changeDataName(name: String) {
        if (name.length > 20) return
        if (name.lines().size > 1) return
        _data.update { it.copy(name = name) }
    }

    private fun changeDataDescription(description: String) {
        if (description.length > 4096) return
        if (description.lines().size > 8) return
        _data.update { it.copy(description = description) }
    }

    private fun changeDataAuthorizer(authorizer: Authorizer) {
        _data.update { currentData ->
            var updatedData = currentData.copy(authorizer = authorizer)
            val effectiveAuthorizer = if (authorizer == Authorizer.Global) state.value.globalAuthorizer else authorizer

            if (effectiveAuthorizer == Authorizer.Dhizuku) {
                updatedData = updatedData.copy(
                    enableCustomizePackageSource = false,
                    installerMode = InstallerMode.Self,
                    enableCustomizeUser = false,
                    enableManualDexopt = false
                )
            }
            updatedData
        }

        // Handle side-effects after state is safely updated
        if (_data.value.enableCustomizeUser) {
            loadAvailableUsers()
        } else {
            _availableUsers.value = emptyMap()
            _data.update { it.copy(targetUserId = 0) }
        }
    }

    private fun changeDataCustomizeAuthorizer(customizeAuthorizer: String) {
        _data.update { it.copy(customizeAuthorizer = customizeAuthorizer) }
    }

    private fun changeDataInstallMode(installMode: InstallMode) {
        _data.update { it.copy(installMode = installMode) }
    }

    private fun changeDataShowToast(showToast: Boolean) {
        _data.update { it.copy(showToast = showToast) }
    }

    private fun changeDataEnableCustomInstallReason(enable: Boolean) {
        _data.update { it.copy(enableCustomizeInstallReason = enable) }
    }

    private fun changeDataInstallReason(installReason: InstallReason) {
        _data.update { it.copy(installReason = installReason) }
    }

    private fun changeDataEnableCustomPackageSource(enable: Boolean) {
        _data.update { it.copy(enableCustomizePackageSource = enable) }
    }

    private fun changeDataPackageSource(packageSource: PackageSource) {
        _data.update { it.copy(packageSource = packageSource) }
    }

    private fun changeDataEnableCustomInstallRequester(enable: Boolean) {
        _data.update { it.copy(enableCustomizeInstallRequester = enable) }
        if (enable) {
            changeDataInstallRequester(_data.value.installRequester)
        }
    }

    private var installRequesterJob: Job? = null

    private fun changeDataInstallRequester(packageName: String) {
        _data.update { it.copy(installRequester = packageName, installRequesterUid = null) }
        if (packageName.isBlank()) return

        installRequesterJob?.cancel()
        installRequesterJob = viewModelScope.launch {
            // Debounce for 300ms to avoid frequent queries while typing
            delay(300)

            // The UseCase handles the IO thread switching and exception catching internally
            val uid = getPackageUid(packageName)

            _data.update { currentData ->
                // Double check if the package name has changed during the delay
                if (currentData.installRequester == packageName) {
                    currentData.copy(installRequesterUid = uid)
                } else currentData
            }
        }
    }

    private fun changeDataInstallerMode(mode: InstallerMode) {
        _data.update { it.copy(installerMode = mode) }
    }

    private fun changeDataInstaller(installer: String) {
        _data.update { it.copy(installer = installer) }
    }

    private fun changeDataCustomizeUser(enable: Boolean) {
        _data.update { it.copy(enableCustomizeUser = enable) }
        if (enable) {
            loadAvailableUsers()
        } else {
            _availableUsers.value = emptyMap()
            _data.update { it.copy(targetUserId = 0) }
        }
    }

    private fun changeDataTargetUserId(userId: Int) {
        _data.update { it.copy(targetUserId = userId) }
    }

    private fun changeDataEnableManualDexopt(enable: Boolean) {
        _data.update { it.copy(enableManualDexopt = enable) }
    }

    private fun changeDataForceDexopt(force: Boolean) {
        _data.update { it.copy(forceDexopt = force) }
    }

    private fun changeDataDexoptMode(mode: DexoptMode) {
        _data.update { it.copy(dexoptMode = mode) }
    }

    private fun changeDataAutoDelete(autoDelete: Boolean) {
        _data.update { it.copy(autoDelete = autoDelete, autoDeleteZip = false) }
    }

    private fun changeDataZipAutoDelete(autoDelete: Boolean) {
        _data.update { it.copy(autoDeleteZip = autoDelete) }
    }

    private fun changeDisplaySdk(displaySdk: Boolean) {
        _data.update { it.copy(displaySdk = displaySdk) }
    }

    private fun changeDisplaySize(displaySize: Boolean) {
        _data.update { it.copy(displaySize = displaySize) }
    }

    private fun changeDataForAllUser(forAllUser: Boolean) {
        _data.update { it.copy(forAllUser = forAllUser) }
    }

    private fun changeDataAllowTestOnly(allowTestOnly: Boolean) {
        _data.update { it.copy(allowTestOnly = allowTestOnly) }
    }

    private fun changeDataAllowDowngrade(allowDowngrade: Boolean) {
        _data.update { it.copy(allowDowngrade = allowDowngrade) }
    }

    private fun changeDataBypassLowTargetSdk(bypassLowTargetSdk: Boolean) {
        _data.update { it.copy(bypassLowTargetSdk = bypassLowTargetSdk) }
    }

    private fun changeDataAllowSigMismatch(allow: Boolean) {
        _data.update { it.copy(allowSigMismatch = allow) }
    }

    private fun changeDataAllowSigUnknown(allow: Boolean) {
        _data.update { it.copy(allowSigUnknown = allow) }
    }

    private fun changeDataAllowAllRequestedPermissions(allowAllRequestedPermissions: Boolean) {
        _data.update { it.copy(allowAllRequestedPermissions = allowAllRequestedPermissions) }
    }

    private fun changeDataRequestUpdateOwnership(requestUpdateOwnership: Boolean) {
        _data.update { it.copy(requestUpdateOwnership = requestUpdateOwnership) }
    }

    private fun changeSplitChooseAll(splitChooseAll: Boolean) {
        _data.update { it.copy(splitChooseAll = splitChooseAll) }
    }

    private fun changeApkChooseAll(apkChooseAll: Boolean) {
        _data.update { it.copy(apkChooseAll = apkChooseAll) }
    }

    private fun changeRequireBiometricAuth(require: Boolean) {
        _data.update { it.copy(requireBiometricAuth = require) }
    }

    private fun loadAvailableUsers() {
        viewModelScope.launch {
            val currentData = _data.value
            val authorizer =
                if (currentData.authorizer == Authorizer.Global) state.value.globalAuthorizer else currentData.authorizer

            val newAvailableUsers = getAvailableUsers(authorizer).getOrElse { emptyMap() }

            _availableUsers.value = newAvailableUsers

            _data.update { data ->
                val currentTargetUserId = data.targetUserId
                val newTargetUserId = if (newAvailableUsers.containsKey(currentTargetUserId)) currentTargetUserId else 0
                data.copy(targetUserId = newTargetUserId)
            }
        }
    }

    private var loadDataJob: Job? = null

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            val prefs = appSettingsRepo.preferencesFlow.first()
            val configModel = getConfigDraft(id, prefs.authorizer)

            val initialData = EditViewState.Data.build(configModel).copy(
                installRequesterUid = configModel.callingFromUid
            )

            // Local state updates only. Global states are now entirely managed reactively by StateFlow combined flow.
            _data.value = initialData
            _originalData.value = initialData

            if (initialData.enableCustomizeUser) {
                loadAvailableUsers()
            }
        }
    }

    private var saveDataJob: Job? = null

    private fun saveData() {
        saveDataJob?.cancel()
        saveDataJob = viewModelScope.launch(Dispatchers.IO) {
            val currentData = _data.value
            var model = currentData.toConfigModel()
            if (id != null) model = model.copy(id = id)
            val hasRequesterUid = currentData.installRequesterUid != null

            val result = saveConfig(model, hasRequesterUid)

            result.onSuccess {
                _originalData.value = currentData
                _eventFlow.emit(EditViewEvent.Saved)
            }.onFailure { error ->
                Timber.e(error, "Failed to save config")

                if (error is SaveConfigUseCase.SaveConfigException) {
                    val errorResId = when (error.error) {
                        SaveConfigUseCase.Error.NAME_EMPTY -> R.string.config_error_name
                        SaveConfigUseCase.Error.CUSTOM_AUTHORIZER_EMPTY -> R.string.config_error_customize_authorizer
                        SaveConfigUseCase.Error.INSTALLER_EMPTY -> R.string.config_error_installer
                        SaveConfigUseCase.Error.REQUESTER_NOT_FOUND -> R.string.config_error_package_not_found
                    }
                    _eventFlow.emit(EditViewEvent.SnackBar(messageResId = errorResId))
                } else {
                    // Fallback to error message if present, otherwise use the localized unknown error resource
                    val errorMsg = error.message
                    if (!errorMsg.isNullOrBlank()) {
                        _eventFlow.emit(EditViewEvent.SnackBar(message = errorMsg))
                    } else {
                        _eventFlow.emit(EditViewEvent.SnackBar(messageResId = R.string.installer_unknown_error))
                    }
                }
            }
        }
    }
}
