package com.rosan.installer.ui.page.settings.preferred

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.recycle.util.useUserService
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PreferredViewModel(
    private val appDataStore: AppDataStore,
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf(PreferredViewState())
        private set

    private var initialized = false

    fun dispatch(action: PreferredViewAction) =
        when (action) {
            is PreferredViewAction.Init -> init()
            is PreferredViewAction.ChangeGlobalAuthorizer -> changeGlobalAuthorizer(action.authorizer)
            is PreferredViewAction.ChangeGlobalCustomizeAuthorizer -> changeGlobalCustomizeAuthorizer(action.customizeAuthorizer)
            is PreferredViewAction.ChangeGlobalInstallMode -> changeGlobalInstallMode(action.installMode)
            is PreferredViewAction.ChangeAdbVerifyEnabledState -> changeAdbVerifyEnabled(action.enabled)
            is PreferredViewAction.ChangeShowDialogInstallExtendedMenu -> changeShowDialogInstallExtendedMenu(action.showMenu)
            is PreferredViewAction.ChangeShowSuggestion -> changeShowSuggestionState(action.showIntelligentSuggestion)
            is PreferredViewAction.ChangeShowDisableNotification -> changeDisableNotificationState(action.showDisableNotification)

            is PreferredViewAction.ChangeShowDialogWhenPressingNotification -> changeShowDialogWhenPressingNotification(
                action.showDialog
            )

            is PreferredViewAction.ChangeDhizukuAutoCloseCountDown -> changeDhizukuAutoCloseCountDown(action.countDown)
            is PreferredViewAction.ChangeShowRefreshedUI -> changeRefreshedUI(action.showRefreshedUI)
            is PreferredViewAction.AddManagedPackage -> addManagedPackage(action.item)
            is PreferredViewAction.RemoveManagedPackage -> removeManagedPackage(action.item)
        }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun init() {
        // DataStore async initialization
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val authorizerFlow = appDataStore.getString(AppDataStore.AUTHORIZER)
                .map { AuthorizerConverter.revert(it) }
            val customizeAuthorizerFlow = appDataStore.getString(AppDataStore.CUSTOMIZE_AUTHORIZER)
            val installModeFlow = appDataStore.getString(AppDataStore.INSTALL_MODE)
                .map { InstallModeConverter.revert(it) }
            val showDialogInstallExtendedMenuFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU)
            val showIntelligentSuggestionFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, true)
            val showNotificationForDialogInstallFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, false)
            val showDialogWhenPressingNotificationFlow =
                appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true)
            val dhizukuAutoCloseCountDownFlow =
                appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3)
            val showRefreshedUIFlow = appDataStore.getBoolean(AppDataStore.UI_FRESH_SWITCH, true)
            val managedPackagesFlow = appDataStore.getNamedPackageList()
            val adbVerifyEnabledFlow = getSettingsGlobalIntAsFlow(
                context.contentResolver,
                "verifier_verify_adb_installs",
                1
            ).map { it != 0 }

            combine(
                authorizerFlow,
                customizeAuthorizerFlow,
                installModeFlow,
                showDialogInstallExtendedMenuFlow,
                showIntelligentSuggestionFlow,
                showNotificationForDialogInstallFlow,
                showDialogWhenPressingNotificationFlow,
                dhizukuAutoCloseCountDownFlow,
                showRefreshedUIFlow,
                managedPackagesFlow,
                adbVerifyEnabledFlow
            ) { values: Array<Any?> ->
                val authorizer = values[0] as ConfigEntity.Authorizer
                val customize = values[1] as String
                val installMode = values[2] as ConfigEntity.InstallMode
                val showMenu = values[3] as Boolean
                val showIntelligentSuggestion = values[4] as Boolean
                val showNotification = values[5] as Boolean
                val showDialog = values[6] as Boolean
                val countDown = values[7] as Int
                val showRefreshedUI = values[8] as Boolean
                val managedPackages = (values[9] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val adbVerifyEnabled = values[10] as Boolean
                val customizeAuthorizer =
                    if (authorizer == ConfigEntity.Authorizer.Customize) customize else ""
                PreferredViewState(
                    progress = PreferredViewState.Progress.Loaded,
                    authorizer = authorizer,
                    customizeAuthorizer = customizeAuthorizer,
                    installMode = installMode,
                    showDialogInstallExtendedMenu = showMenu,
                    showIntelligentSuggestion = showIntelligentSuggestion,
                    disableNotificationForDialogInstall = showNotification,
                    showDialogWhenPressingNotification = showDialog,
                    dhizukuAutoCloseCountDown = countDown,
                    showRefreshedUI = showRefreshedUI,
                    managedPackages = managedPackages,
                    adbVerifyEnabled = adbVerifyEnabled
                )
            }.collectLatest { state = it }
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) {
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.AUTHORIZER, AuthorizerConverter.convert(authorizer))
        }
    }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) {
        viewModelScope.launch {
            if (state.authorizerCustomize) {
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, customizeAuthorizer)
            } else {
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, "")
            }
        }
    }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) {
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.INSTALL_MODE, InstallModeConverter.convert(installMode))
        }
    }

    private fun changeShowDialogInstallExtendedMenu(installExtendedMenu: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, installExtendedMenu)
        }
    }

    private fun changeShowSuggestionState(showIntelligentSuggestion: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, showIntelligentSuggestion)
        }
    }

    private fun changeDisableNotificationState(showDisableNotification: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, showDisableNotification)
        }
    }

    private fun changeShowDialogWhenPressingNotification(showDialog: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, showDialog)
        }
    }

    private fun changeDhizukuAutoCloseCountDown(countDown: Int) {
        viewModelScope.launch {
            // Ensure countDown is within the valid range
            if (countDown in 1..10) {
                appDataStore.putInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, countDown)
            }
        }
    }

    private fun changeRefreshedUI(showRefreshedUI: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.UI_FRESH_SWITCH, showRefreshedUI)
        }
    }

    private fun addManagedPackage(item: NamedPackage) {
        viewModelScope.launch {
            // Create a new list from the current state
            val currentList = state.managedPackages.toMutableList()
            // Add the new item if it's not already in the list
            if (!currentList.contains(item)) {
                currentList.add(item)
                // Save the updated list back to DataStore
                appDataStore.putNamedPackageList(currentList)
            }
        }
    }

    private fun removeManagedPackage(item: NamedPackage) {
        viewModelScope.launch {
            // Create a new list from the current state
            val currentList = state.managedPackages.toMutableList()
            // Remove the item
            currentList.remove(item)
            // Save the updated list back to DataStore
            appDataStore.putNamedPackageList(currentList)
        }
    }

    /**
     * A reusable helper function to get a Settings.Global integer value as a Flow,
     * ensuring the blocking call is always on a background thread.
     */
    private fun getSettingsGlobalIntAsFlow(cr: ContentResolver, name: String, defaultValue: Int): Flow<Int> = flow {
        emit(Settings.Global.getInt(cr, name, defaultValue))
    }.flowOn(Dispatchers.IO)

    private fun changeAdbVerifyEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Changing ADB Verify Enabled to: $enabled")
            useUserService(state.authorizer) { userService ->
                val isPermissionGranted = userService.privileged.isPermissionGranted(
                    context.packageName, "android.permission.WRITE_SECURE_SETTINGS"
                )
                if (!isPermissionGranted) {
                    Timber.w("WRITE_SECURE_SETTINGS permission not granted, attempting to grant it...")
                    userService.privileged.grantRuntimePermission(
                        context.packageName,
                        "android.permission.WRITE_SECURE_SETTINGS"
                    )
                }
            }
            // This need android.permission.WRITE_SECURE_SETTINGS, thus cannot be called directly
            Settings.Global.putInt(context.contentResolver, "verifier_verify_adb_installs", if (enabled) 1 else 0)
            // Optimistically update the UI state
            state = state.copy(adbVerifyEnabled = enabled)
        }
    }
}