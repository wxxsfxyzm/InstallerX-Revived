package com.rosan.installer.ui.page.main.settings.config.edit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.settings.util.ConfigUtil.getGlobalAuthorizer
import com.rosan.installer.data.settings.util.ConfigUtil.getGlobalInstallMode
import com.rosan.installer.data.settings.util.ConfigUtil.readGlobal
import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
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
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class EditViewModel(
    private val appSettingsRepo: AppSettingsRepo,
    private val getConfigDraftUseCase: GetConfigDraftUseCase,
    private val saveConfigUseCase: SaveConfigUseCase,
    private val systemInfoProvider: SystemInfoProvider,
    private val id: Long? = null
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf(EditViewState())
        private set

    // A property to store the original, unmodified data.
    private var originalData: EditViewState.Data? = null

    // A computed property to check for unsaved changes.
    // It's true if the original data has been loaded and the current data is different.
    val hasUnsavedChanges: Boolean
        get() = originalData != null && state.data != originalData

    // A new property that returns a list of specific, human-readable error messages.
    val activeErrorMessages: List<String>
        get() {
            val errors = mutableListOf<String>()
            with(state.data) {
                if (errorName) {
                    errors.add(context.getString(R.string.config_error_name))
                }
                if (errorCustomizeAuthorizer) {
                    errors.add(context.getString(R.string.config_error_customize_authorizer))
                }
                if (errorInstaller) {
                    errors.add(context.getString(R.string.config_error_installer))
                }
                if (errorInstallRequester) {
                    errors.add(context.getString(R.string.config_error_package_not_found))
                }
            }
            return errors
        }

    val hasErrors: Boolean
        get() = activeErrorMessages.isNotEmpty()

    var globalAuthorizer by mutableStateOf(Authorizer.Global)
        private set

    var globalInstallMode by mutableStateOf(InstallMode.Global)
        private set

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

                    is EditViewAction.ChangeDataAllowAllRequestedPermissions -> changeDataAllowAllRequestedPermissions(
                        action.allowAllRequestedPermissions
                    )

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
        Timber.d("[STATE_CHANGE] Name changing to: '$name'")
        state = state.copy(
            data = state.data.copy(
                name = name
            )
        )
    }

    private fun changeDataDescription(description: String) {
        if (description.length > 4096) return
        if (description.lines().size > 8) return
        state = state.copy(
            data = state.data.copy(
                description = description
            )
        )
    }

    private fun changeDataAuthorizer(authorizer: Authorizer) {
        var updatedData = state.data.copy(
            authorizer = authorizer
        )

        // Dependency logic for Dhizuku: also disable declareInstaller
        if (authorizer == Authorizer.Dhizuku) {
            updatedData = updatedData.copy()
        }

        // Determine the effective authorizer after this change.
        val effectiveAuthorizer = if (authorizer == Authorizer.Global)
            globalAuthorizer
        else authorizer

        // If the effective authorizer is Dhizuku, force disable the customize user feature.
        if (effectiveAuthorizer == Authorizer.Dhizuku) {
            updatedData = updatedData.copy(
                enableCustomizePackageSource = false,
                declareInstaller = false,
                enableCustomizeUser = false,
                enableManualDexopt = false
            )
        }

        // Apply the data changes to the state.
        state = state.copy(data = updatedData)

        // Now, decide what to do with the user list based on the *new* state.
        if (state.data.enableCustomizeUser) {
            // If customize user is enabled (which means authorizer is not Dhizuku),
            // we should reload the user list as it might have changed.
            loadAvailableUsers()
        } else {
            // If customize user is now disabled (either manually or forced by Dhizuku),
            // clear the user list and reset the target user ID.
            state = state.copy(
                availableUsers = emptyMap(),
                data = state.data.copy(targetUserId = 0)
            )
        }
    }

    private fun changeDataCustomizeAuthorizer(customizeAuthorizer: String) {
        state = state.copy(
            data = state.data.copy(
                customizeAuthorizer = customizeAuthorizer
            )
        )
    }

    private fun changeDataInstallMode(installMode: InstallMode) {
        state = state.copy(
            data = state.data.copy(
                installMode = installMode
            )
        )
    }

    private fun changeDataEnableCustomInstallReason(enable: Boolean) {
        state = state.copy(
            data = state.data.copy(
                enableCustomizeInstallReason = enable
            )
        )
    }

    private fun changeDataInstallReason(installReason: InstallReason) {
        state = state.copy(
            data = state.data.copy(
                installReason = installReason
            )
        )
    }

    private fun changeDataEnableCustomPackageSource(enable: Boolean) {
        state = state.copy(
            data = state.data.copy(
                enableCustomizePackageSource = enable
            )
        )
    }

    private fun changeDataPackageSource(packageSource: PackageSource) {
        state = state.copy(
            data = state.data.copy(
                packageSource = packageSource
            )
        )
    }

    private fun changeDataDeclareInstaller(declareInstaller: Boolean) {
        state = state.copy(
            data = state.data.copy(
                declareInstaller = declareInstaller
            )
        )
    }

    private fun changeDataEnableCustomInstallRequester(enable: Boolean) {
        state = state.copy(
            data = state.data.copy(
                enableCustomizeInstallRequester = enable
            )
        )
        // If enabling, we should re-validate the current text immediately
        if (enable) {
            changeDataInstallRequester(state.data.installRequester)
        }
    }

    private var installRequesterJob: Job? = null

    private fun changeDataInstallRequester(packageName: String) {
        // Update UI immediately (do not wait for UID)
        state = state.copy(
            data = state.data.copy(
                installRequester = packageName,
                installRequesterUid = null // Clear UID first to avoid stale data
            )
        )

        // Return immediately if blank to avoid unnecessary IO
        if (packageName.isBlank()) return

        // Cancel the previous job to prevent concurrency issues and overwrites
        installRequesterJob?.cancel()
        installRequesterJob = viewModelScope.launch(Dispatchers.IO) {
            // Debounce for input fields
            delay(300)

            val uid = try {
                context.packageManager
                    .getApplicationInfo(packageName, 0)
                    .uid
            } catch (_: Exception) {
                null
            }

            // Switch back to Main thread & race condition check
            withContext(Dispatchers.Main.immediate) {
                // Ensure the package name hasn't changed during the async operation
                if (state.data.installRequester == packageName) {
                    state = state.copy(
                        data = state.data.copy(
                            installRequesterUid = uid
                        )
                    )
                }
            }
        }
    }

    private fun changeDataInstaller(installer: String) {
        state = state.copy(
            data = state.data.copy(
                installer = installer
            )
        )
    }

    private fun changeDataCustomizeUser(enable: Boolean) {
        val updatedData = state.data.copy(
            enableCustomizeUser = enable
        )
        if (enable) {
            state = state.copy(data = updatedData)
            loadAvailableUsers()
        } else {
            // When disabling, also clear the user list and reset the selected user ID.
            state = state.copy(
                data = updatedData.copy(targetUserId = 0),
                availableUsers = emptyMap()
            )
        }
    }

    private fun changeDataTargetUserId(userId: Int) {
        state = state.copy(
            data = state.data.copy(
                targetUserId = userId
            )
        )
    }

    private fun changeDataEnableManualDexopt(enable: Boolean) {
        state = state.copy(
            data = state.data.copy(
                enableManualDexopt = enable
            )
        )
    }

    private fun changeDataForceDexopt(force: Boolean) {
        state = state.copy(
            data = state.data.copy(forceDexopt = force)
        )
    }

    private fun changeDataDexoptMode(mode: DexoptMode) {
        state = state.copy(
            data = state.data.copy(
                dexoptMode = mode
            )
        )
    }

    private fun changeDataAutoDelete(autoDelete: Boolean) {
        Timber.d("[STATE_CHANGE] AutoDelete changing to: $autoDelete")
        state = state.copy(
            data = state.data.copy(
                autoDelete = autoDelete,
                autoDeleteZip = false
            )
        )
    }

    private fun changeDataZipAutoDelete(autoDelete: Boolean) {
        Timber.d("[STATE_CHANGE] Zip AutoDelete changing to: $autoDelete")
        state = state.copy(
            data = state.data.copy(
                autoDeleteZip = autoDelete
            )
        )
    }

    private fun changeDisplaySdk(displaySdk: Boolean) {
        state = state.copy(
            data = state.data.copy(
                displaySdk = displaySdk
            )
        )
    }

    private fun changeDisplaySize(displaySize: Boolean) {
        state = state.copy(
            data = state.data.copy(
                displaySize = displaySize
            )
        )
    }

    private fun changeDataForAllUser(forAllUser: Boolean) {
        state = state.copy(
            data = state.data.copy(
                forAllUser = forAllUser
            )
        )
    }

    private fun changeDataAllowTestOnly(allowTestOnly: Boolean) {
        state = state.copy(
            data = state.data.copy(
                allowTestOnly = allowTestOnly
            )
        )
    }

    private fun changeDataAllowDowngrade(allowDowngrade: Boolean) {
        state = state.copy(
            data = state.data.copy(
                allowDowngrade = allowDowngrade
            )
        )
    }

    private fun changeDataBypassLowTargetSdk(bypassLowTargetSdk: Boolean) {
        state = state.copy(
            data = state.data.copy(
                bypassLowTargetSdk = bypassLowTargetSdk
            )
        )
    }

    private fun changeDataAllowAllRequestedPermissions(allowAllRequestedPermissions: Boolean) {
        state = state.copy(
            data = state.data.copy(
                allowAllRequestedPermissions = allowAllRequestedPermissions
            )
        )
    }

    private fun changeSplitChooseAll(splitChooseAll: Boolean) {
        state = state.copy(
            data = state.data.copy(
                splitChooseAll = splitChooseAll
            )
        )
    }

    private fun changeApkChooseAll(apkChooseAll: Boolean) {
        state = state.copy(
            data = state.data.copy(
                apkChooseAll = apkChooseAll
            )
        )
    }

    private fun loadAvailableUsers() {
        viewModelScope.launch {
            Timber.i("[LOAD_USERS] Starting to load available users.")
            val newAvailableUsers = runCatching {
                val authorizer = state.data.authorizer.readGlobal()
                withContext(Dispatchers.IO) { systemInfoProvider.getUsers(authorizer) }
            }.getOrElse {
                Timber.e(it, "Failed to load available users.")
                _eventFlow.emit(EditViewEvent.SnackBar(message = it.getErrorMessage(context)))
                emptyMap()
            }

            val currentTargetUserId = state.data.targetUserId
            // Validate if the current target user still exists. If not, reset to 0.
            val newTargetUserId = if (newAvailableUsers.containsKey(currentTargetUserId)) {
                currentTargetUserId
            } else {
                Timber.w("[LOAD_USERS] Current target user ID $currentTargetUserId not found in new user list. Resetting to 0.")
                0
            }

            state = state.copy(
                availableUsers = newAvailableUsers,
                data = state.data.copy(targetUserId = newTargetUserId)
            )
            Timber.i("[LOAD_USERS] Available users loaded: ${newAvailableUsers.keys}. Target user ID set to: $newTargetUserId")
        }
    }

    private var loadDataJob: Job? = null

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {

            globalAuthorizer = getGlobalAuthorizer()
            globalInstallMode = getGlobalInstallMode()

            val managedPackages =
                appSettingsRepo.getNamedPackageList(NamedPackageListSetting.ManagedInstallerPackages).firstOrNull() ?: emptyList()
            val isCustomInstallRequesterEnabled = appSettingsRepo.getBoolean(BooleanSetting.LabSetInstallRequester).first()

            val configModel = getConfigDraftUseCase(id, globalAuthorizer)

            val initialData = EditViewState.Data.build(configModel).copy(
                installRequesterUid = configModel.callingFromUid
            )

            originalData = initialData

            state = state.copy(
                data = initialData,
                managedInstallerPackages = managedPackages,
                availableUsers = emptyMap(),
                isCustomInstallRequesterEnabled = isCustomInstallRequesterEnabled
            )

            Timber.i("[LOAD_DATA] Data loaded via UseCase: $initialData")

            if (initialData.enableCustomizeUser) {
                loadAvailableUsers()
            }
        }
    }

    private var saveDataJob: Job? = null

    private fun saveData() {
        saveDataJob?.cancel()
        saveDataJob = viewModelScope.launch(Dispatchers.IO) {
            val model = state.data.toConfigModel()
            // Assume you have checked if UID exists in state.data.installRequesterUid != null
            val hasRequesterUid = state.data.installRequesterUid != null

            // Delegate all logic to UseCase
            val result = saveConfigUseCase(model, hasRequesterUid)

            result.onSuccess {
                originalData = state.data
                Timber.i("[SAVE_DATA] Data saved successfully.")
                _eventFlow.emit(EditViewEvent.Saved)
            }.onFailure { error ->
                // Map Domain error to UI String
                val message = when (error.message) {
                    SaveConfigUseCase.Error.NAME_EMPTY.name -> context.getString(R.string.config_error_name)
                    SaveConfigUseCase.Error.CUSTOM_AUTHORIZER_EMPTY.name -> context.getString(R.string.config_error_customize_authorizer)
                    SaveConfigUseCase.Error.INSTALLER_EMPTY.name -> context.getString(R.string.config_error_installer)
                    SaveConfigUseCase.Error.REQUESTER_NOT_FOUND.name -> context.getString(R.string.config_error_package_not_found)
                    else -> "Unknown error"
                }
                _eventFlow.emit(EditViewEvent.SnackBar(message = message))
            }
        }
    }
}