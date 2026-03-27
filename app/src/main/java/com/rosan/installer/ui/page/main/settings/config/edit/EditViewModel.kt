// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.DexoptMode
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.model.InstallReason
import com.rosan.installer.domain.settings.model.PackageSource
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class EditViewModel(
    private val id: Long? = null,
    private val appSettingsRepo: AppSettingsRepo,
    private val getConfigDraft: GetConfigDraftUseCase,
    private val saveConfig: SaveConfigUseCase,
    private val getAvailableUsers: GetAvailableUsersUseCase,
    private val getPackageUid: GetPackageUidUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditViewState())
    val state: StateFlow<EditViewState> = _state.asStateFlow()

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
                    is EditViewAction.ChangeDataDeclareInstaller -> changeDataDeclareInstaller(action.declareInstaller)
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
                    is EditViewAction.ChangeDataAllowAllRequestedPermissions -> changeDataAllowAllRequestedPermissions(action.allowAllRequestedPermissions)
                    is EditViewAction.ChangeDataRequestUpdateOwnership -> changeDataRequestUpdateOwnership(action.requestUpdateOwnership)
                    is EditViewAction.ChangeSplitChooseAll -> changeSplitChooseAll(action.splitChooseAll)
                    is EditViewAction.ChangeApkChooseAll -> changeApkChooseAll(action.apkChooseAll)
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
        _state.update { it.copy(data = it.data.copy(name = name)) }
    }

    private fun changeDataDescription(description: String) {
        if (description.length > 4096) return
        if (description.lines().size > 8) return
        _state.update { it.copy(data = it.data.copy(description = description)) }
    }

    private fun changeDataAuthorizer(authorizer: Authorizer) {
        _state.update { currentState ->
            var updatedData = currentState.data.copy(authorizer = authorizer)
            val effectiveAuthorizer = if (authorizer == Authorizer.Global) currentState.globalAuthorizer else authorizer

            if (effectiveAuthorizer == Authorizer.Dhizuku) {
                updatedData = updatedData.copy(
                    enableCustomizePackageSource = false,
                    declareInstaller = false,
                    enableCustomizeUser = false,
                    enableManualDexopt = false
                )
            }
            currentState.copy(data = updatedData)
        }

        // Handle side-effects after state is safely updated
        val currentState = _state.value
        if (currentState.data.enableCustomizeUser) {
            loadAvailableUsers()
        } else {
            _state.update { it.copy(availableUsers = emptyMap(), data = it.data.copy(targetUserId = 0)) }
        }
    }

    private fun changeDataCustomizeAuthorizer(customizeAuthorizer: String) {
        _state.update { it.copy(data = it.data.copy(customizeAuthorizer = customizeAuthorizer)) }
    }

    private fun changeDataInstallMode(installMode: InstallMode) {
        _state.update { it.copy(data = it.data.copy(installMode = installMode)) }
    }

    private fun changeDataShowToast(showToast: Boolean) {
        _state.update { it.copy(data = it.data.copy(showToast = showToast)) }
    }

    private fun changeDataEnableCustomInstallReason(enable: Boolean) {
        _state.update { it.copy(data = it.data.copy(enableCustomizeInstallReason = enable)) }
    }

    private fun changeDataInstallReason(installReason: InstallReason) {
        _state.update { it.copy(data = it.data.copy(installReason = installReason)) }
    }

    private fun changeDataEnableCustomPackageSource(enable: Boolean) {
        _state.update { it.copy(data = it.data.copy(enableCustomizePackageSource = enable)) }
    }

    private fun changeDataPackageSource(packageSource: PackageSource) {
        _state.update { it.copy(data = it.data.copy(packageSource = packageSource)) }
    }

    private fun changeDataDeclareInstaller(declareInstaller: Boolean) {
        _state.update { it.copy(data = it.data.copy(declareInstaller = declareInstaller)) }
    }

    private fun changeDataEnableCustomInstallRequester(enable: Boolean) {
        _state.update { it.copy(data = it.data.copy(enableCustomizeInstallRequester = enable)) }
        if (enable) {
            changeDataInstallRequester(_state.value.data.installRequester)
        }
    }

    private var installRequesterJob: Job? = null

    private fun changeDataInstallRequester(packageName: String) {
        _state.update { it.copy(data = it.data.copy(installRequester = packageName, installRequesterUid = null)) }
        if (packageName.isBlank()) return

        installRequesterJob?.cancel()
        installRequesterJob = viewModelScope.launch {
            // Debounce for 300ms to avoid frequent queries while typing
            delay(300)

            // The UseCase handles the IO thread switching and exception catching internally
            val uid = getPackageUid(packageName)

            _state.update { currentState ->
                // Double check if the package name has changed during the delay
                if (currentState.data.installRequester == packageName) {
                    currentState.copy(data = currentState.data.copy(installRequesterUid = uid))
                } else currentState
            }
        }
    }

    private fun changeDataInstaller(installer: String) {
        _state.update { it.copy(data = it.data.copy(installer = installer)) }
    }

    private fun changeDataCustomizeUser(enable: Boolean) {
        _state.update { it.copy(data = it.data.copy(enableCustomizeUser = enable)) }
        if (enable) {
            loadAvailableUsers()
        } else {
            _state.update { it.copy(availableUsers = emptyMap(), data = it.data.copy(targetUserId = 0)) }
        }
    }

    private fun changeDataTargetUserId(userId: Int) {
        _state.update { it.copy(data = it.data.copy(targetUserId = userId)) }
    }

    private fun changeDataEnableManualDexopt(enable: Boolean) {
        _state.update { it.copy(data = it.data.copy(enableManualDexopt = enable)) }
    }

    private fun changeDataForceDexopt(force: Boolean) {
        _state.update { it.copy(data = it.data.copy(forceDexopt = force)) }
    }

    private fun changeDataDexoptMode(mode: DexoptMode) {
        _state.update { it.copy(data = it.data.copy(dexoptMode = mode)) }
    }

    private fun changeDataAutoDelete(autoDelete: Boolean) {
        _state.update { it.copy(data = it.data.copy(autoDelete = autoDelete, autoDeleteZip = false)) }
    }

    private fun changeDataZipAutoDelete(autoDelete: Boolean) {
        _state.update { it.copy(data = it.data.copy(autoDeleteZip = autoDelete)) }
    }

    private fun changeDisplaySdk(displaySdk: Boolean) {
        _state.update { it.copy(data = it.data.copy(displaySdk = displaySdk)) }
    }

    private fun changeDisplaySize(displaySize: Boolean) {
        _state.update { it.copy(data = it.data.copy(displaySize = displaySize)) }
    }

    private fun changeDataForAllUser(forAllUser: Boolean) {
        _state.update { it.copy(data = it.data.copy(forAllUser = forAllUser)) }
    }

    private fun changeDataAllowTestOnly(allowTestOnly: Boolean) {
        _state.update { it.copy(data = it.data.copy(allowTestOnly = allowTestOnly)) }
    }

    private fun changeDataAllowDowngrade(allowDowngrade: Boolean) {
        _state.update { it.copy(data = it.data.copy(allowDowngrade = allowDowngrade)) }
    }

    private fun changeDataBypassLowTargetSdk(bypassLowTargetSdk: Boolean) {
        _state.update { it.copy(data = it.data.copy(bypassLowTargetSdk = bypassLowTargetSdk)) }
    }

    private fun changeDataAllowAllRequestedPermissions(allowAllRequestedPermissions: Boolean) {
        _state.update { it.copy(data = it.data.copy(allowAllRequestedPermissions = allowAllRequestedPermissions)) }
    }

    private fun changeDataRequestUpdateOwnership(requestUpdateOwnership: Boolean) {
        _state.update { it.copy(data = it.data.copy(requestUpdateOwnership = requestUpdateOwnership)) }
    }

    private fun changeSplitChooseAll(splitChooseAll: Boolean) {
        _state.update { it.copy(data = it.data.copy(splitChooseAll = splitChooseAll)) }
    }

    private fun changeApkChooseAll(apkChooseAll: Boolean) {
        _state.update { it.copy(data = it.data.copy(apkChooseAll = apkChooseAll)) }
    }

    private fun loadAvailableUsers() {
        viewModelScope.launch {
            val currentState = _state.value
            val authorizer =
                if (currentState.data.authorizer == Authorizer.Global) currentState.globalAuthorizer else currentState.data.authorizer

            val newAvailableUsers = getAvailableUsers(authorizer).getOrElse { emptyMap() }

            _state.update { state ->
                val currentTargetUserId = state.data.targetUserId
                val newTargetUserId = if (newAvailableUsers.containsKey(currentTargetUserId)) currentTargetUserId else 0
                state.copy(
                    availableUsers = newAvailableUsers,
                    data = state.data.copy(targetUserId = newTargetUserId)
                )
            }
        }
    }

    private var loadDataJob: Job? = null

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            val prefs = appSettingsRepo.preferencesFlow.first()
            val managedPackages =
                appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedInstallerPackages).firstOrNull() ?: emptyList()
            val isCustomInstallRequesterEnabled = appSettingsRepo.getBoolean(BooleanSetting.LabSetInstallRequester).first()
            val configModel = getConfigDraft(id, prefs.authorizer)

            val initialData = EditViewState.Data.build(configModel).copy(
                installRequesterUid = configModel.callingFromUid
            )

            _state.update {
                it.copy(
                    data = initialData,
                    originalData = initialData,
                    globalAuthorizer = prefs.authorizer,
                    globalInstallMode = prefs.installMode,
                    managedInstallerPackages = managedPackages,
                    availableUsers = emptyMap(),
                    isCustomInstallRequesterEnabled = isCustomInstallRequesterEnabled
                )
            }

            if (initialData.enableCustomizeUser) {
                loadAvailableUsers()
            }
        }
    }

    private var saveDataJob: Job? = null

    private fun saveData() {
        saveDataJob?.cancel()
        saveDataJob = viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.value
            var model = currentState.data.toConfigModel()
            if (id != null) model = model.copy(id = id)
            val hasRequesterUid = currentState.data.installRequesterUid != null

            val result = saveConfig(model, hasRequesterUid)

            result.onSuccess {
                _state.update { it.copy(originalData = it.data) }
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
