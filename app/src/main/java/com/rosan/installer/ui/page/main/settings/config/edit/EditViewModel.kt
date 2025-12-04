package com.rosan.installer.ui.page.main.settings.config.edit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.recycle.model.impl.PrivilegedManager
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.repo.ConfigRepo
import com.rosan.installer.data.settings.util.ConfigUtil.Companion.getGlobalAuthorizer
import com.rosan.installer.data.settings.util.ConfigUtil.Companion.getGlobalInstallMode
import com.rosan.installer.data.settings.util.ConfigUtil.Companion.readGlobal
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class EditViewModel(
    private val repo: ConfigRepo,
    private val appDataStore: AppDataStore,
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
            }
            return errors
        }

    val hasErrors: Boolean
        get() = activeErrorMessages.isNotEmpty()

    var globalAuthorizer by mutableStateOf(ConfigEntity.Authorizer.Global)
        private set

    var globalInstallMode by mutableStateOf(ConfigEntity.InstallMode.Global)
        private set

    private val _eventFlow = MutableSharedFlow<EditViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun dispatch(action: EditViewAction) {
        Timber.i("[DISPATCH] Action received: ${action::class.simpleName}")
        viewModelScope.launch {
            val errorMessage = runCatching {
                when (action) {
                    is EditViewAction.Init -> init()
                    is EditViewAction.ChangeDataName -> changeDataName(action.name)
                    is EditViewAction.ChangeDataDescription -> changeDataDescription(action.description)
                    is EditViewAction.ChangeDataAuthorizer -> changeDataAuthorizer(action.authorizer)
                    is EditViewAction.ChangeDataCustomizeAuthorizer -> changeDataCustomizeAuthorizer(action.customizeAuthorizer)
                    is EditViewAction.ChangeDataInstallMode -> changeDataInstallMode(action.installMode)
                    is EditViewAction.ChangeDataEnableCustomizePackageSource -> changeDataEnableCustomPackageSource(action.enable)
                    is EditViewAction.ChangeDataPackageSource -> changeDataPackageSource(action.packageSource)
                    is EditViewAction.ChangeDataDeclareInstaller -> changeDataDeclareInstaller(action.declareInstaller)
                    is EditViewAction.ChangeDataInstaller -> changeDataInstaller(action.installer)
                    is EditViewAction.ChangeDataCustomizeUser -> changeDataCustomizeUser(action.enable)
                    is EditViewAction.ChangeDataTargetUserId -> changeDataTargetUserId(action.userId)
                    is EditViewAction.ChangeDataEnableManualDexopt -> changeDataEnableManualDexopt(action.enable)
                    is EditViewAction.ChangeDataForceDexopt -> changeDataForceDexopt(action.force)
                    is EditViewAction.ChangeDataDexoptMode -> changeDataDexoptMode(action.mode)
                    is EditViewAction.ChangeDataAutoDelete -> changeDataAutoDelete(action.autoDelete)
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

                    is EditViewAction.LoadData -> loadData()
                    is EditViewAction.SaveData -> saveData()
                }
            }.exceptionOrNull()?.message
            if (errorMessage != null) {
                _eventFlow.emit(EditViewEvent.SnackBar(message = errorMessage))
            }
        }
    }

    private var isInited: Boolean = false

    private fun init() {
        synchronized(this) {
            if (isInited) return
            isInited = true
            loadData()
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

    private fun changeDataAuthorizer(authorizer: ConfigEntity.Authorizer) {
        var updatedData = state.data.copy(
            authorizer = authorizer
        )

        // Dependency logic for Dhizuku: also disable declareInstaller
        if (authorizer == ConfigEntity.Authorizer.Dhizuku) {
            updatedData = updatedData.copy()
        }

        // Determine the effective authorizer after this change.
        val effectiveAuthorizer = if (authorizer == ConfigEntity.Authorizer.Global)
            globalAuthorizer
        else authorizer

        // If the effective authorizer is Dhizuku, force disable the customize user feature.
        if (effectiveAuthorizer == ConfigEntity.Authorizer.Dhizuku) {
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

    private fun changeDataInstallMode(installMode: ConfigEntity.InstallMode) {
        state = state.copy(
            data = state.data.copy(
                installMode = installMode
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

    private fun changeDataPackageSource(packageSource: ConfigEntity.PackageSource) {
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

    private fun changeDataDexoptMode(mode: ConfigEntity.DexoptMode) {
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
                autoDelete = autoDelete
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

    private fun loadAvailableUsers() {
        viewModelScope.launch {
            Timber.i("[LOAD_USERS] Starting to load available users.")
            val newAvailableUsers = runCatching {
                val authorizer = state.data.authorizer.readGlobal()
                withContext(Dispatchers.IO) { PrivilegedManager.getUsers(authorizer) }
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
            // Check if it is a new configuration or an existing one.
            val configEntity = if (id == null) {
                // If new, use default config but with an empty name.
                ConfigEntity.default.copy(name = "")
            } else {
                // If editing, find the config by id. Fallback to a default empty one if not found.
                repo.find(id) ?: ConfigEntity.default.copy(name = "")
            }
            // Build the initial data state from the entity.
            var initialData = EditViewState.Data.build(configEntity)
            val currentGlobalAuthorizer = getGlobalAuthorizer()
            globalAuthorizer = currentGlobalAuthorizer
            globalInstallMode = getGlobalInstallMode()
            // Check if the effective authorizer is Dhizuku and force disable customize user if so.
            val effectiveAuthorizer = if (configEntity.authorizer == ConfigEntity.Authorizer.Global)
                currentGlobalAuthorizer
            else
                configEntity.authorizer

            if (effectiveAuthorizer == ConfigEntity.Authorizer.Dhizuku) {
                // Overwrite the enableCustomizeUser value from the loaded entity.
                initialData = initialData.copy(
                    declareInstaller = false,
                    enableCustomizeUser = false,
                    enableManualDexopt = false
                )
            }
            // Store this initial data as the "original" state.
            originalData = initialData
            val managedInstallerPackages =
                appDataStore.getNamedPackageList(AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST).firstOrNull()
                    ?: emptyList()
            // Set the initial state, but with an empty user list for now.
            state = state.copy(
                data = initialData,
                managedInstallerPackages = managedInstallerPackages,
                availableUsers = emptyMap()
            )
            // Only load the available users if the feature is enabled.
            if (initialData.enableCustomizeUser) {
                // This call is non-blocking and will update the state again once users are loaded.
                loadAvailableUsers()
            }
            Timber.i("[LOAD_DATA] Original data has been set: $initialData")

        }
    }

    private var saveDataJob: Job? = null

    private fun saveData() {
        saveDataJob?.cancel()
        saveDataJob = viewModelScope.launch(Dispatchers.IO) {
            val message = when {
                state.data.errorName -> context.getString(R.string.config_error_name)
                state.data.errorCustomizeAuthorizer -> context.getString(R.string.config_error_customize_authorizer)
                state.data.errorInstaller -> context.getString(R.string.config_error_installer)
                else -> null
            }
            if (message != null) {
                _eventFlow.emit(EditViewEvent.SnackBar(message = message))
            } else {
                val entity = state.data.toConfigEntity()
                if (id == null) repo.insert(entity)
                else repo.update(entity.also {
                    it.id = id
                })
                // After a successful save, update the "original" data
                // to match the current state. This resets the hasUnsavedChanges flag.
                originalData = state.data
                Timber.i("[SAVE_DATA] Data saved. Original data updated to: $originalData")
                _eventFlow.emit(EditViewEvent.Saved)
            }
        }
    }
}