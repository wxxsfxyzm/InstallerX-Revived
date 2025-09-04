package com.rosan.installer.ui.page.settings.preferred

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.app.repo.PrivilegedActionRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.ui.activity.InstallerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PreferredViewModel(
    private val appDataStore: AppDataStore,
    private val paRepo: PrivilegedActionRepo
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf(PreferredViewState())
        private set

    private val _uiEvents = Channel<PreferredViewEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private var initialized = false

    fun dispatch(action: PreferredViewAction) =
        when (action) {
            is PreferredViewAction.Init -> init()
            is PreferredViewAction.ChangeGlobalAuthorizer -> changeGlobalAuthorizer(action.authorizer)
            is PreferredViewAction.ChangeGlobalCustomizeAuthorizer -> changeGlobalCustomizeAuthorizer(action.customizeAuthorizer)
            is PreferredViewAction.ChangeGlobalInstallMode -> changeGlobalInstallMode(action.installMode)
            is PreferredViewAction.ChangeShowDialogInstallExtendedMenu -> changeShowDialogInstallExtendedMenu(action.showMenu)
            is PreferredViewAction.ChangeShowSuggestion -> changeShowSuggestionState(action.showIntelligentSuggestion)
            is PreferredViewAction.ChangeShowDisableNotification -> changeDisableNotificationState(action.showDisableNotification)
            is PreferredViewAction.ChangeShowDialogWhenPressingNotification -> changeShowDialog(action.showDialog)
            is PreferredViewAction.ChangeDhizukuAutoCloseCountDown -> changeDhizukuAutoCloseCountDown(action.countDown)
            is PreferredViewAction.ChangeShowRefreshedUI -> changeRefreshedUI(action.showRefreshedUI)
            is PreferredViewAction.ChangeVersionCompareInSingleLine -> changeVersionCompareInSingleLine(action.versionCompareInSingleLine)
            is PreferredViewAction.AddManagedInstallerPackage -> addManagedPackage(
                state.managedInstallerPackages,
                AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST,
                action.pkg
            )

            is PreferredViewAction.RemoveManagedInstallerPackage -> removeManagedPackage(
                state.managedInstallerPackages,
                AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST,
                action.pkg
            )

            is PreferredViewAction.AddManagedBlacklistPackage -> addManagedPackage(
                state.managedBlacklistPackages,
                AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST,
                action.pkg
            )

            is PreferredViewAction.RemoveManagedBlacklistPackage -> removeManagedPackage(
                state.managedBlacklistPackages,
                AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST,
                action.pkg
            )

            is PreferredViewAction.AddManagedSharedUserIdBlacklist -> addSharedUserIdToBlacklist(action.uid)
            is PreferredViewAction.RemoveManagedSharedUserIdBlacklist -> removeSharedUserIdFromBlacklist(action.uid)
            is PreferredViewAction.AddManagedSharedUserIdExemptedPackages -> addManagedPackage(
                state.managedSharedUserIdExemptedPackages,
                AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST,
                action.pkg
            )

            is PreferredViewAction.RemoveManagedSharedUserIdExemptedPackages -> removeManagedPackage(
                state.managedSharedUserIdExemptedPackages,
                AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST,
                action.pkg
            )

            is PreferredViewAction.SetAdbVerifyEnabledState -> viewModelScope.launch {
                setAdbVerifyEnabled(
                    action.enabled,
                    action
                )
            }

            is PreferredViewAction.RequestIgnoreBatteryOptimization -> requestIgnoreBatteryOptimization()
            is PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus -> refreshIgnoreBatteryOptStatus()
            is PreferredViewAction.SetDefaultInstaller -> viewModelScope.launch {
                setDefaultInstaller(
                    action.lock,
                    action
                )
            }
        }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun init() {
        // DataStore async initialization
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val authorizerFlow = appDataStore.getString(AppDataStore.AUTHORIZER).map { AuthorizerConverter.revert(it) }
            val customizeAuthorizerFlow = appDataStore.getString(AppDataStore.CUSTOMIZE_AUTHORIZER)
            val installModeFlow = appDataStore.getString(AppDataStore.INSTALL_MODE).map { InstallModeConverter.revert(it) }
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
            val versionCompareInSingleLineFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, false)
            val showRefreshedUIFlow = appDataStore.getBoolean(AppDataStore.UI_FRESH_SWITCH, true)
            val managedInstallerPackagesFlow =
                appDataStore.getNamedPackageList(AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST)
            val managedBlacklistPackagesFlow =
                appDataStore.getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST)
            val managedSharedUserIdBlacklistFlow =
                appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST)
            val managedSharedUserIdExemptPkgFlow =
                appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST)

            val adbVerifyEnabledFlow = getSettingsGlobalIntAsFlow(
                context.contentResolver,
                "verifier_verify_adb_installs",
                1
            ).map { it != 0 }
            val isIgnoringBatteryOptFlow = getIsIgnoreBatteryOptAsFlow()

            combine(
                authorizerFlow,
                customizeAuthorizerFlow,
                installModeFlow,
                showDialogInstallExtendedMenuFlow,
                showIntelligentSuggestionFlow,
                showNotificationForDialogInstallFlow,
                showDialogWhenPressingNotificationFlow,
                dhizukuAutoCloseCountDownFlow,
                versionCompareInSingleLineFlow,
                showRefreshedUIFlow,
                managedInstallerPackagesFlow,
                managedBlacklistPackagesFlow,
                managedSharedUserIdBlacklistFlow,
                managedSharedUserIdExemptPkgFlow,
                adbVerifyEnabledFlow,
                isIgnoringBatteryOptFlow
            ) { values: Array<Any?> ->
                val authorizer = values[0] as ConfigEntity.Authorizer
                val customize = values[1] as String
                val installMode = values[2] as ConfigEntity.InstallMode
                val showMenu = values[3] as Boolean
                val showIntelligentSuggestion = values[4] as Boolean
                val showNotification = values[5] as Boolean
                val showDialog = values[6] as Boolean
                val countDown = values[7] as Int
                val versionCompareInSingleLine = values[8] as Boolean
                val showRefreshedUI = values[9] as Boolean
                val managedInstallerPackages = (values[10] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val managedBlacklistPackages = (values[11] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val managedSharedUserIdBlacklist = (values[12] as? List<*>)?.filterIsInstance<SharedUid>() ?: emptyList()
                val managedSharedUserIdExemptPkg = (values[13] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val adbVerifyEnabled = values[14] as Boolean
                val isIgnoringBatteryOptimizations = values[15] as Boolean
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
                    versionCompareInSingleLine = versionCompareInSingleLine,
                    showRefreshedUI = showRefreshedUI,
                    managedInstallerPackages = managedInstallerPackages,
                    managedBlacklistPackages = managedBlacklistPackages,
                    managedSharedUserIdBlacklist = managedSharedUserIdBlacklist,
                    managedSharedUserIdExemptedPackages = managedSharedUserIdExemptPkg,
                    adbVerifyEnabled = adbVerifyEnabled,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations
                )
            }.collectLatest { state = it }
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) =
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.AUTHORIZER, AuthorizerConverter.convert(authorizer))
        }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) =
        viewModelScope.launch {
            if (state.authorizerCustomize) {
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, customizeAuthorizer)
            } else {
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, "")
            }
        }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) =
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.INSTALL_MODE, InstallModeConverter.convert(installMode))
        }

    private fun changeShowDialogInstallExtendedMenu(installExtendedMenu: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, installExtendedMenu)
        }

    private fun changeShowSuggestionState(showIntelligentSuggestion: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, showIntelligentSuggestion)
        }

    private fun changeDisableNotificationState(showDisableNotification: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, showDisableNotification)
        }

    private fun changeShowDialog(showDialog: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, showDialog)
        }

    private fun changeDhizukuAutoCloseCountDown(countDown: Int) =
        viewModelScope.launch {
            // Ensure countDown is within the valid range
            if (countDown in 1..10) {
                appDataStore.putInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, countDown)
            }
        }

    private fun changeRefreshedUI(showRefreshedUI: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.UI_FRESH_SWITCH, showRefreshedUI)
        }

    private fun changeVersionCompareInSingleLine(singleLine: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, singleLine)
        }

    private fun addManagedPackage(list: List<NamedPackage>, key: Preferences.Key<String>, pkg: NamedPackage) =
        viewModelScope.launch {
            // Create a new list from the current state
            val currentList = list.toMutableList()
            // Add the new pkg if it's not already in the list
            if (!currentList.contains(pkg)) {
                currentList.add(pkg)
                // Save the updated list back to DataStore
                appDataStore.putNamedPackageList(key, currentList)
            }
        }

    private fun removeManagedPackage(list: List<NamedPackage>, key: Preferences.Key<String>, pkg: NamedPackage) =
        viewModelScope.launch {
            // Create a new list from the current state
            val currentList = list.toMutableList()
            // Remove the pkg
            currentList.remove(pkg)
            // Save the updated list back to DataStore
            appDataStore.putNamedPackageList(key, currentList)
        }

    private fun addSharedUserIdToBlacklist(uid: SharedUid) {
        viewModelScope.launch {
            val currentList = state.managedSharedUserIdBlacklist
            if (uid in currentList) return@launch

            val newList = currentList + uid
            appDataStore.putSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST, newList)
        }
    }

    private fun removeSharedUserIdFromBlacklist(uid: SharedUid) {
        viewModelScope.launch {
            val currentList = state.managedSharedUserIdBlacklist
            if (uid !in currentList) return@launch

            val newList = currentList.toMutableList().apply { remove(uid) }
            appDataStore.putSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST, newList)
        }
    }

    /**
     * A reusable helper function to get a Settings.Global integer value as a Flow,
     * ensuring the blocking call is always on a background thread.
     */
    private fun getSettingsGlobalIntAsFlow(cr: ContentResolver, name: String, defaultValue: Int): Flow<Int> = flow {
        emit(Settings.Global.getInt(cr, name, defaultValue))
    }.flowOn(Dispatchers.IO)

    private fun getIsIgnoreBatteryOptAsFlow(): Flow<Boolean> = flow {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        emit(pm.isIgnoringBatteryOptimizations(context.packageName))
    }.flowOn(Dispatchers.IO)

    /**
     * Creates and starts an Intent to navigate the user to the system's battery optimization settings page for this app.
     */
    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimization() =
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
                // The intent needs to be started from a non-activity context (ViewModel),
                // so FLAG_ACTIVITY_NEW_TASK is required.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not start activity to request ignore battery optimizations")
            viewModelScope.launch {
                _uiEvents.send(PreferredViewEvent.ShowSnackbar("无法打开电池优化设置"))
            }
        }

    /**
     * Re-checks the battery optimization status and updates the UI state.
     * This is called when the user returns to the preferred screen.
     */
    private fun refreshIgnoreBatteryOptStatus() =
        viewModelScope.launch {
            // Use .first() to get a single, up-to-date value from the flow.
            val isIgnoring = getIsIgnoreBatteryOptAsFlow().first()
            // After refreshing, update the state.
            state = state.copy(isIgnoringBatteryOptimizations = isIgnoring)
        }

    private suspend fun setAdbVerifyEnabled(enabled: Boolean, action: PreferredViewAction) =
        runPrivilegedAction(
            action = action,
            titleForError = context.getString(R.string.disable_adb_install_verify_failed),
            successMessage = null, // No success snackbar message for this action type
            block = {
                Timber.d("Changing ADB Verify Enabled to: $enabled")
                val isPermissionGranted = paRepo.isPermissionGranted(
                    state.authorizer,
                    context.packageName, "android.permission.WRITE_SECURE_SETTINGS"
                )
                if (!isPermissionGranted) {
                    Timber.w("WRITE_SECURE_SETTINGS permission not granted, attempting to grant it...")
                    paRepo.grantRuntimePermission(
                        state.authorizer,
                        context.packageName,
                        "android.permission.WRITE_SECURE_SETTINGS"
                    )
                }

                // This need android.permission.WRITE_SECURE_SETTINGS, thus cannot be called directly
                Settings.Global.putInt(context.contentResolver, "verifier_verify_adb_installs", if (enabled) 1 else 0)
                state = state.copy(adbVerifyEnabled = enabled)
            }
        )

    private suspend fun setDefaultInstaller(lock: Boolean, action: PreferredViewAction) {
        val config = ConfigUtil.getByPackageName(null)
        val component = ComponentName(context, InstallerActivity::class.java)
        runPrivilegedAction(
            action = action,
            titleForError = context.getString(if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed),
            successMessage = context.getString(if (lock) R.string.lock_default_installer_success else R.string.unlock_default_installer_success),
            block = { paRepo.setDefaultInstaller(config, component, lock) }
        )
    }

    private suspend fun runPrivilegedAction(
        action: PreferredViewAction,
        titleForError: String,
        successMessage: String?,
        block: suspend () -> Unit
    ) =
        runCatching {
            withContext(Dispatchers.IO) { // Ensure privileged actions run on IO dispatcher
                block()
            }
        }.onSuccess {
            Timber.d("Privileged action succeeded")
            if (successMessage != null)
                _uiEvents.send(PreferredViewEvent.ShowSnackbar(successMessage))
        }.onFailure { exception ->
            Timber.e(exception, "Privileged action failed")
            _uiEvents.send(PreferredViewEvent.ShowErrorDialog(titleForError, exception, action))
        }

}