package com.rosan.installer.ui.page.main.settings.preferred

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieronquinn.monetcompat.core.MonetCompat
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.data.app.model.enums.HttpProfile
import com.rosan.installer.data.app.model.enums.RootImplementation
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.recycle.model.impl.PrivilegedManager
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.datastore.entity.SharedUid
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import com.rosan.installer.data.updater.repo.AppUpdater
import com.rosan.installer.data.updater.repo.UpdateChecker
import com.rosan.installer.ui.activity.InstallerActivity
import com.rosan.installer.ui.theme.m3color.PaletteStyle
import com.rosan.installer.ui.theme.m3color.PresetColors
import com.rosan.installer.ui.theme.m3color.RawColor
import com.rosan.installer.ui.theme.m3color.ThemeMode
import com.rosan.installer.ui.util.doBiometricAuth
import com.rosan.installer.util.addFlag
import com.rosan.installer.util.removeFlag
import com.rosan.installer.util.timber.FileLoggingTree.Companion.LOG_DIR_NAME
import com.rosan.installer.util.timber.FileLoggingTree.Companion.LOG_SUFFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.io.File

class PreferredViewModel(
    private val context: Application,
    private val appDataStore: AppDataStore,
    private val updateChecker: UpdateChecker,
    private val appUpdater: AppUpdater
) : ViewModel(), KoinComponent {
    var state by mutableStateOf(PreferredViewState())
        private set

    private val updateResultFlow = MutableStateFlow<UpdateChecker.CheckResult?>(null)
    private val adbVerifyEnabledFlow = MutableStateFlow(true) // refresh default value at init
    private val isIgnoringBatteryOptFlow = MutableStateFlow(true)

    private val _uiEvents = MutableSharedFlow<PreferredViewEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    private var initialized = false

    fun dispatch(action: PreferredViewAction) =
        when (action) {
            is PreferredViewAction.Init -> init()
            is PreferredViewAction.Update -> performInAppUpdate()
            is PreferredViewAction.ChangeGlobalAuthorizer -> changeGlobalAuthorizer(action.authorizer)
            is PreferredViewAction.ChangeGlobalCustomizeAuthorizer -> changeGlobalCustomizeAuthorizer(action.customizeAuthorizer)
            is PreferredViewAction.ChangeGlobalInstallMode -> changeGlobalInstallMode(action.installMode)
            is PreferredViewAction.ChangeShowDialogInstallExtendedMenu -> changeShowDialogInstallExtendedMenu(action.showMenu)
            is PreferredViewAction.ChangeShowSuggestion -> changeShowSuggestionState(action.showSuggestion)
            is PreferredViewAction.ChangeShowDisableNotification -> changeDisableNotificationState(action.showDisableNotification)
            is PreferredViewAction.ChangeShowDialogWhenPressingNotification -> changeShowDialog(action.showDialog)
            is PreferredViewAction.ChangeDhizukuAutoCloseCountDown -> changeDhizukuAutoCloseCountDown(action.countDown)
            is PreferredViewAction.ChangeNotificationSuccessAutoClearSeconds -> changeNotificationSuccessAutoClearSeconds(action.seconds)
            is PreferredViewAction.ChangeShowExpressiveUI -> changeUseExpressiveUI(action.showRefreshedUI)
            is PreferredViewAction.ChangeShowLiveActivity -> changeUseLiveActivity(action.showLiveActivity)
            is PreferredViewAction.ChangeBiometricAuth -> changeBiometricAuth(action.biometricAuth, action.isInstaller)
            is PreferredViewAction.ChangeUseMiuix -> changeUseMiuix(action.useMiuix)
            is PreferredViewAction.ChangePreferSystemIcon -> changePreferSystemIcon(action.preferSystemIcon)
            is PreferredViewAction.ChangeShowLauncherIcon -> changeShowLauncherIcon(action.showLauncherIcon)
            is PreferredViewAction.ChangeVersionCompareInSingleLine -> changeVersionCompareInSingleLine(action.versionCompareInSingleLine)
            is PreferredViewAction.ChangeSdkCompareInMultiLine -> changeSdkCompareInMultiLine(action.sdkCompareInMultiLine)
            is PreferredViewAction.ChangeShowOPPOSpecial -> changeShowOPPOSpecial(action.showOPPOSpecial)
            is PreferredViewAction.ChangeAutoLockInstaller -> changeAutoLockInstaller(action.autoLockInstaller)
            is PreferredViewAction.ChangeAutoSilentInstall -> changeAutoSilentInstall(action.autoSilentInstall)

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

            is PreferredViewAction.AddManagedSharedUserIdBlacklist -> addSharedUserIdToBlacklist(
                action.uid
            )

            is PreferredViewAction.RemoveManagedSharedUserIdBlacklist -> removeSharedUserIdFromBlacklist(
                action.uid
            )

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

            is PreferredViewAction.ToggleGlobalUninstallFlag -> toggleGlobalUninstallFlag(action.flag, action.enable)
            is PreferredViewAction.SetAdbVerifyEnabledState -> viewModelScope.launch {
                setAdbVerifyEnabled(
                    action.enabled,
                    action
                )
            }

            is PreferredViewAction.RequestIgnoreBatteryOptimization -> requestIgnoreBatteryOptimization()
            is PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus -> refreshIgnoreBatteryOptStatus()
            is PreferredViewAction.SetDefaultInstaller -> viewModelScope.launch {
                setDefaultInstaller(action.lock, action)
            }

            is PreferredViewAction.LabChangeRootModuleFlash -> labChangeRootModuleFlash(action.enable)
            is PreferredViewAction.LabChangeRootShowModuleArt -> labChangeRootShowModuleArt(action.enable)
            is PreferredViewAction.LabChangeRootModuleAlwaysUseRoot -> labChangeRootModuleAlwaysUseRoot(action.enable)
            is PreferredViewAction.LabChangeRootImplementation -> labChangeRootImplementation(action.implementation)
            is PreferredViewAction.LabChangeHttpProfile -> labChangeHttpProfile(action.profile)
            is PreferredViewAction.LabChangeHttpSaveFile -> labChangeHttpSaveFile(action.enable)
            is PreferredViewAction.LabChangeSetInstallRequester -> labChangeSetInstallRequester(action.enable)

            is PreferredViewAction.SetThemeMode -> setThemeMode(action.mode)
            is PreferredViewAction.SetPaletteStyle -> setPaletteStyle(action.style)
            is PreferredViewAction.SetUseDynamicColor -> setUseDynamicColor(action.use)
            is PreferredViewAction.SetUseMiuixMonet -> setUseMiuixMonet(action.use)
            is PreferredViewAction.SetSeedColor -> setSeedColor(action.color)
            is PreferredViewAction.SetDynColorFollowPkgIcon -> setDynColorFollowPkgIcon(action.follow)
            is PreferredViewAction.SetDynColorFollowPkgIconForLiveActivity -> setDynColorFollowPkgIconForLiveActivity(action.follow)
            is PreferredViewAction.SetUseBlur -> setUseBlur(action.enable)

            is PreferredViewAction.SetEnableFileLogging -> setEnableFileLogging(action.enable)
            is PreferredViewAction.ShareLog -> shareLog()
        }

    private fun init() {
        // DataStore async initialization
        if (initialized) return
        initialized = true

        refreshIgnoreBatteryOptStatus()
        refreshAdbVerifyStatus()
        checkUpdate()
        getAndCombineState()
    }

    private fun performInAppUpdate() = viewModelScope.launch(Dispatchers.IO) {
        val updateInfo = updateResultFlow.value
        if (updateInfo == null || !updateInfo.hasUpdate || updateInfo.downloadUrl.isEmpty()) {
            Timber.w("No valid update info found when performing in-app update.")
            return@launch
        }

        _uiEvents.emit(PreferredViewEvent.ShowUpdateLoading)

        try {
            val config = ConfigEntity.default.copy(authorizer = state.authorizer)

            Timber.d("Starting in-app update using cached URL: ${updateInfo.downloadUrl}")

            appUpdater.performInAppUpdate(updateInfo.downloadUrl, config)
        } catch (e: Exception) {
            Timber.e(e, "In-app update failed")
            _uiEvents.emit(PreferredViewEvent.ShowInAppUpdateErrorDetail("Update Failed", e))
        } finally {
            // Hide loading indicator regardless of success or failure
            _uiEvents.emit(PreferredViewEvent.HideUpdateLoading)
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) =
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.AUTHORIZER, AuthorizerConverter.convert(authorizer))
        }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) =
        viewModelScope.launch {
            if (state.authorizerCustomize)
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, customizeAuthorizer)
            else
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, "")
        }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) =
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.INSTALL_MODE, InstallModeConverter.convert(installMode))
        }

    private fun changeShowDialogInstallExtendedMenu(installExtendedMenu: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, installExtendedMenu)
        }

    private fun changeShowSuggestionState(showSmartSuggestion: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, showSmartSuggestion)
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

    private fun changeNotificationSuccessAutoClearSeconds(seconds: Int) =
        viewModelScope.launch {
            appDataStore.putInt(AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS, seconds)
        }

    private fun changeUseExpressiveUI(showRefreshedUI: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.UI_EXPRESSIVE_SWITCH, showRefreshedUI)
        }

    private fun changeUseLiveActivity(showLiveActivity: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, showLiveActivity)
        }

    private fun changeBiometricAuth(biometricAuth: Boolean, installer: Boolean) {
        viewModelScope.launch {
            if (!context.doBiometricAuth(
                    title = context.getString(R.string.use_biometric_confirm_change_auth_settings),
                    subTitle = context.getString(R.string.use_biometric_confirm_change_auth_settings_desc)
                )
            ) return@launch

            appDataStore.putBoolean(
                key = if (installer) AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH else AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH,
                value = biometricAuth
            )
        }
    }

    private fun changeUseMiuix(useMiuix: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.UI_USE_MIUIX, useMiuix)
        }

    private fun changePreferSystemIcon(preferSystemIcon: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, preferSystemIcon)
        }

    private fun changeShowLauncherIcon(show: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.SHOW_LAUNCHER_ICON, show)
            val componentName = ComponentName(context, "com.rosan.installer.ui.activity.LauncherAlias")
            val newState = if (show) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        }

    private fun changeVersionCompareInSingleLine(singleLine: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, singleLine)
        }

    private fun changeSdkCompareInMultiLine(singleLine: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE, singleLine)
        }

    private fun changeShowOPPOSpecial(show: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_OPPO_SPECIAL, show)
        }

    private fun changeAutoLockInstaller(autoLockInstaller: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.AUTO_LOCK_INSTALLER, autoLockInstaller)
        }

    private fun changeAutoSilentInstall(enabled: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_AUTO_SILENT_INSTALL, enabled)
        }

    private fun addManagedPackage(
        list: List<NamedPackage>,
        key: Preferences.Key<String>,
        pkg: NamedPackage
    ) = viewModelScope.launch {
        // Create a new list from the current state
        val currentList = list.toMutableList()
        // Add the new pkg if it's not already in the list
        if (!currentList.contains(pkg)) {
            currentList.add(pkg)
            // Save the updated list back to DataStore
            appDataStore.putNamedPackageList(key, currentList)
        }
    }

    private fun removeManagedPackage(
        list: List<NamedPackage>,
        key: Preferences.Key<String>,
        pkg: NamedPackage
    ) = viewModelScope.launch {
        // Create a new list from the current state
        val currentList = list.toMutableList()
        // Remove the pkg
        currentList.remove(pkg)
        // Save the updated list back to DataStore
        appDataStore.putNamedPackageList(key, currentList)
    }

    private fun addSharedUserIdToBlacklist(uid: SharedUid) =
        viewModelScope.launch {
            val currentList = state.managedSharedUserIdBlacklist
            if (uid in currentList) return@launch

            val newList = currentList + uid
            appDataStore.putSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST, newList)
        }


    private fun removeSharedUserIdFromBlacklist(uid: SharedUid) =
        viewModelScope.launch {
            val currentList = state.managedSharedUserIdBlacklist
            if (uid !in currentList) return@launch

            val newList = currentList.toMutableList().apply { remove(uid) }
            appDataStore.putSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST, newList)
        }

    private fun toggleGlobalUninstallFlag(flag: Int, enable: Boolean) = viewModelScope.launch {
        appDataStore.updateUninstallFlags { currentFlags ->
            var newFlags = currentFlags

            if (enable) {
                // 1. Add the flag being enabled
                newFlags = newFlags.addFlag(flag)

                // 2. Handle mutual exclusivity
                if (flag == PackageManagerUtil.DELETE_ALL_USERS) {
                    if (currentFlags and PackageManagerUtil.DELETE_SYSTEM_APP != 0) {
                        // Notify user about forced change
                        notifyMutualExclusion(flag)
                        newFlags = newFlags.removeFlag(PackageManagerUtil.DELETE_SYSTEM_APP)
                    }
                } else if (flag == PackageManagerUtil.DELETE_SYSTEM_APP) {
                    if (currentFlags and PackageManagerUtil.DELETE_ALL_USERS != 0) {
                        // Notify user about forced change
                        notifyMutualExclusion(flag)
                        newFlags = newFlags.removeFlag(PackageManagerUtil.DELETE_ALL_USERS)
                    }
                }
            } else {
                // Disable: just remove the flag
                newFlags = newFlags.removeFlag(flag)
            }

            newFlags
        }
    }

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
                _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerResult("Could not start activity to request ignore battery optimizations"))
            }
        }

    /**
     * Re-checks the battery optimization status and updates the UI state.
     * This is called when the user returns to the preferred screen.
     */
    private fun refreshIgnoreBatteryOptStatus() =
        viewModelScope.launch {
            // Check once immediately for response speed
            val firstCheck = getIsIgnoreBatteryOptAsFlow().first()
            isIgnoringBatteryOptFlow.value = firstCheck

            // Check again to compat for Xiaomi Devices
            delay(500)

            val secondCheck = getIsIgnoreBatteryOptAsFlow().first()
            // Only update flow when the status has changed
            if (firstCheck != secondCheck) {
                isIgnoringBatteryOptFlow.value = secondCheck
                Timber.d("Battery optimization status updated after delay: $secondCheck")
            }
        }

    private suspend fun setAdbVerifyEnabled(enabled: Boolean, action: PreferredViewAction) =
        runPrivilegedAction(
            action = action,
            titleForError = context.getString(R.string.disable_adb_install_verify_failed),
            successMessage = null, // No success snackbar message for this action type
            block = {
                Timber.d("Changing ADB Verify Enabled to: $enabled")
                // [New Logic] Delegate to PrivilegedManager (Service)
                // The Service handles the Permission/Hook logic internally via Binder Proxy
                PrivilegedManager.setAdbVerify(
                    authorizer = state.authorizer,
                    customizeAuthorizer = state.customizeAuthorizer,
                    enabled = enabled
                )
                adbVerifyEnabledFlow.value = enabled
            }
        )

    private fun refreshAdbVerifyStatus() =
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = Settings.Global.getInt(context.contentResolver, "verifier_verify_adb_installs", 1) != 0
            adbVerifyEnabledFlow.value = enabled
        }

    private fun checkUpdate() =
        viewModelScope.launch(Dispatchers.IO) {
            val result = updateChecker.check()
            if (result != null) {
                updateResultFlow.value = result
            }
        }

    private suspend fun setDefaultInstaller(lock: Boolean, action: PreferredViewAction) {
        val authorizer = state.authorizer
        val component = ComponentName(context, InstallerActivity::class.java)
        runPrivilegedAction(
            action = action,
            titleForError = context.getString(if (lock) R.string.lock_default_installer_failed else R.string.unlock_default_installer_failed),
            successMessage = context.getString(if (lock) R.string.lock_default_installer_success else R.string.unlock_default_installer_success),
            block = { PrivilegedManager.setDefaultInstaller(authorizer, component, lock) }
        )
    }

    private fun labChangeRootModuleFlash(enabled: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, enabled)
        }

    private fun labChangeRootShowModuleArt(enabled: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.LAB_MODULE_FLASH_SHOW_ART, enabled)
        }

    private fun labChangeRootModuleAlwaysUseRoot(enabled: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.LAB_MODULE_ALWAYS_ROOT, enabled)
        }

    private fun labChangeRootImplementation(implementation: RootImplementation) =
        viewModelScope.launch {
            appDataStore.putString(
                AppDataStore.LAB_ROOT_IMPLEMENTATION,
                implementation.name
            )
        }

    private fun labChangeHttpProfile(profile: HttpProfile) =
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.LAB_HTTP_PROFILE, profile.name)
        }

    private fun labChangeHttpSaveFile(enable: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.LAB_HTTP_SAVE_FILE, enable)
        }

    private fun labChangeSetInstallRequester(enable: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.LAB_SET_INSTALL_REQUESTER, enable)
        }

    private fun setEnableFileLogging(enable: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.ENABLE_FILE_LOGGING, enable)
        }

    private fun shareLog() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val logDir = File(context.cacheDir, LOG_DIR_NAME)
            if (!logDir.exists() || !logDir.isDirectory) {
                _uiEvents.emit(PreferredViewEvent.ShareLogFailed("Log directory missing"))
                return@launch
            }

            val latestLogFile = logDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(LOG_SUFFIX) }
                ?.maxByOrNull { it.lastModified() }

            if (latestLogFile == null) {
                _uiEvents.emit(PreferredViewEvent.ShareLogFailed("No logs found"))
                return@launch
            }

            // Check if the log file is empty before sharing
            if (latestLogFile.length() == 0L) {
                _uiEvents.emit(PreferredViewEvent.ShareLogFailed("Log file is empty"))
                return@launch
            }

            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, latestLogFile)

            _uiEvents.emit(PreferredViewEvent.OpenLogShare(uri))

        } catch (e: Exception) {
            Timber.e(e, "Failed to share log")
            _uiEvents.emit(PreferredViewEvent.ShareLogFailed(e.message ?: "Failed to share log"))
        }
    }

    private fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        appDataStore.putString(AppDataStore.THEME_MODE, mode.name)
    }

    private fun setPaletteStyle(style: PaletteStyle) = viewModelScope.launch {
        appDataStore.putString(AppDataStore.THEME_PALETTE_STYLE, style.name)
    }

    private fun setUseDynamicColor(use: Boolean) = viewModelScope.launch {
        appDataStore.putBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, use)
    }

    private fun setUseMiuixMonet(use: Boolean) = viewModelScope.launch {
        appDataStore.putBoolean(AppDataStore.UI_USE_MIUIX_MONET, use)
    }

    private fun setSeedColor(color: Color) = viewModelScope.launch {
        appDataStore.putInt(AppDataStore.THEME_SEED_COLOR, color.toArgb())
    }

    private fun setDynColorFollowPkgIcon(enable: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, enable)
        }

    private fun setDynColorFollowPkgIconForLiveActivity(enable: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON, enable)
        }

    private fun setUseBlur(enable: Boolean) =
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.UI_USE_BLUR, enable)
        }

    private suspend fun runPrivilegedAction(
        action: PreferredViewAction,
        titleForError: String,
        successMessage: String?,
        block: suspend () -> Unit
    ) = runCatching {
        withContext(Dispatchers.IO) { // Ensure privileged actions run on IO dispatcher
            block()
        }
    }.onSuccess {
        Timber.d("Privileged action succeeded")
        if (successMessage != null)
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerResult(successMessage))
    }.onFailure { exception ->
        Timber.e(exception, "Privileged action failed")
        _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail(titleForError, exception, action))
    }

    private fun getWallpaperColorsFlow(): Flow<List<Int>?> = flow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // MonetCompat operations should be done safely
            val colors = try {
                MonetCompat.getInstance().getAvailableWallpaperColors()
            } catch (e: Exception) {
                Timber.e(e, "Failed to get Monet colors")
                null
            }
            emit(colors)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    private fun notifyMutualExclusion(disabledFlag: Int) {
        val resId = when (disabledFlag) {
            PackageManagerUtil.DELETE_ALL_USERS ->
                R.string.uninstall_system_app_disabled

            PackageManagerUtil.DELETE_SYSTEM_APP ->
                R.string.uninstall_all_users_disabled

            else -> return
        }

        _uiEvents.tryEmit(
            PreferredViewEvent.ShowMessage(resId)
        )
    }

    private fun getAndCombineState() =
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
            val notificationSuccessAutoClearSecondsFlow =
                appDataStore.getInt(AppDataStore.NOTIFICATION_SUCCESS_AUTO_CLEAR_SECONDS, 0)
            val versionCompareInSingleLineFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, false)
            val sdkCompareInSingleLineFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE, false)
            val showOPPOSpecialFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_OPPO_SPECIAL, false)
            val showExpressiveUIFlow =
                appDataStore.getBoolean(AppDataStore.UI_EXPRESSIVE_SWITCH, true)
            val installerRequireBiometricAuthFlow =
                appDataStore.getBoolean(AppDataStore.INSTALLER_REQUIRE_BIOMETRIC_AUTH, false)
            val uninstallerRequireBiometricAuthFlow =
                appDataStore.getBoolean(AppDataStore.UNINSTALLER_REQUIRE_BIOMETRIC_AUTH, false)
            val showLiveActivityFlow =
                appDataStore.getBoolean(AppDataStore.SHOW_LIVE_ACTIVITY, false)
            val autoLockInstallerFlow =
                appDataStore.getBoolean(AppDataStore.AUTO_LOCK_INSTALLER, false)
            val autoSilentInstallFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_AUTO_SILENT_INSTALL, false)
            val showMiuixUIFlow =
                appDataStore.getBoolean(AppDataStore.UI_USE_MIUIX, false)
            val preferSystemIconFlow =
                appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false)
            val showLauncherIconFlow =
                appDataStore.getBoolean(AppDataStore.SHOW_LAUNCHER_ICON, true)
            val managedInstallerPackagesFlow =
                appDataStore.getNamedPackageList(AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST)
            val managedBlacklistPackagesFlow =
                appDataStore.getNamedPackageList(AppDataStore.MANAGED_BLACKLIST_PACKAGES_LIST)
            val managedSharedUserIdBlacklistFlow =
                appDataStore.getSharedUidList(AppDataStore.MANAGED_SHARED_USER_ID_BLACKLIST)
            val managedSharedUserIdExemptPkgFlow =
                appDataStore.getNamedPackageList(AppDataStore.MANAGED_SHARED_USER_ID_EXEMPTED_PACKAGES_LIST)
            val uninstallFlagsFlow =
                appDataStore.getInt(AppDataStore.UNINSTALL_FLAGS, 0)
            val labRootModuleFlashFlow =
                appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false)
            val labRootShowModuleArtFlow =
                appDataStore.getBoolean(AppDataStore.LAB_MODULE_FLASH_SHOW_ART, true)
            val labRootModuleAlwaysUseRootFlow =
                appDataStore.getBoolean(AppDataStore.LAB_MODULE_ALWAYS_ROOT, false)
            val enableFileLoggingFlow =
                appDataStore.getBoolean(AppDataStore.ENABLE_FILE_LOGGING, true)
            val labRootImplementationFlow =
                appDataStore.getString(AppDataStore.LAB_ROOT_IMPLEMENTATION)
                    .map { RootImplementation.fromString(it) }
            val labHttpProfileFlow =
                appDataStore.getString(AppDataStore.LAB_HTTP_PROFILE)
                    .map { HttpProfile.fromString(it) }
            val labHttpSaveFileFlow =
                appDataStore.getBoolean(AppDataStore.LAB_HTTP_SAVE_FILE, false)
            val labSetInstallRequesterFlow =
                appDataStore.getBoolean(AppDataStore.LAB_SET_INSTALL_REQUESTER, false)
            val themeModeFlow =
                appDataStore.getString(AppDataStore.THEME_MODE, ThemeMode.SYSTEM.name)
                    .map { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
            val paletteStyleFlow =
                appDataStore.getString(AppDataStore.THEME_PALETTE_STYLE, PaletteStyle.TonalSpot.name)
                    .map { runCatching { PaletteStyle.valueOf(it) }.getOrDefault(PaletteStyle.TonalSpot) }
            val useDynamicColorFlow =
                appDataStore.getBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            val useMiuixMonetFlow =
                appDataStore.getBoolean(AppDataStore.UI_USE_MIUIX_MONET, false)
            val seedColorFlow =
                appDataStore.getInt(AppDataStore.THEME_SEED_COLOR, PresetColors.first().color.toArgb())
                    .map { Color(it) }
            val wallpaperColorsFlow = getWallpaperColorsFlow()
            val useDynColorFollowPkgIconFlow =
                appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false)
            val useDynColorFollowPkgIconForLiveActivityFlow =
                appDataStore.getBoolean(AppDataStore.LIVE_ACTIVITY_DYN_COLOR_FOLLOW_PKG_ICON, false)
            val useBlurFlow =
                appDataStore.getBoolean(AppDataStore.UI_USE_BLUR, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

            combine(
                authorizerFlow,
                customizeAuthorizerFlow,
                installModeFlow,
                showDialogInstallExtendedMenuFlow,
                showIntelligentSuggestionFlow,
                showNotificationForDialogInstallFlow,
                showDialogWhenPressingNotificationFlow,
                dhizukuAutoCloseCountDownFlow,
                notificationSuccessAutoClearSecondsFlow,
                versionCompareInSingleLineFlow,
                sdkCompareInSingleLineFlow,
                showOPPOSpecialFlow,
                showExpressiveUIFlow,
                installerRequireBiometricAuthFlow,
                uninstallerRequireBiometricAuthFlow,
                showLiveActivityFlow,
                autoLockInstallerFlow,
                autoSilentInstallFlow,
                showMiuixUIFlow,
                preferSystemIconFlow,
                showLauncherIconFlow,
                managedInstallerPackagesFlow,
                managedBlacklistPackagesFlow,
                managedSharedUserIdBlacklistFlow,
                managedSharedUserIdExemptPkgFlow,
                uninstallFlagsFlow,
                adbVerifyEnabledFlow,
                isIgnoringBatteryOptFlow,
                labRootModuleFlashFlow,
                labRootShowModuleArtFlow,
                labRootModuleAlwaysUseRootFlow,
                labRootImplementationFlow,
                labHttpProfileFlow,
                labHttpSaveFileFlow,
                labSetInstallRequesterFlow,
                enableFileLoggingFlow,
                themeModeFlow,
                paletteStyleFlow,
                useDynamicColorFlow,
                useMiuixMonetFlow,
                seedColorFlow,
                wallpaperColorsFlow,
                useDynColorFollowPkgIconFlow,
                useDynColorFollowPkgIconForLiveActivityFlow,
                useBlurFlow,
                updateResultFlow
            ) { values: Array<Any?> ->
                var idx = 0
                val authorizer = values[idx++] as ConfigEntity.Authorizer
                val customize = values[idx++] as String
                val installMode = values[idx++] as ConfigEntity.InstallMode
                val showMenu = values[idx++] as Boolean
                val showSuggestion = values[idx++] as Boolean
                val showNotification = values[idx++] as Boolean
                val showDialog = values[idx++] as Boolean
                val countDown = values[idx++] as Int
                val notificationSuccessAutoClearSeconds = values[idx++] as Int
                val versionCompareInMultiLine = values[idx++] as Boolean
                val sdkCompareInSingleLine = values[idx++] as Boolean
                val showOPPOSpecial = values[idx++] as Boolean
                val showExpressiveUI = values[idx++] as Boolean
                val installerRequireBiometricAuth = values[idx++] as Boolean
                val uninstallerRequireBiometricAuth = values[idx++] as Boolean
                val showLiveActivity = values[idx++] as Boolean
                val autoLockInstaller = values[idx++] as Boolean
                val autoSilentInstall = values[idx++] as Boolean
                val showMiuixUI = values[idx++] as Boolean
                val preferSystemIcon = values[idx++] as Boolean
                val showLauncherIcon = values[idx++] as Boolean
                val managedInstallerPackages =
                    (values[idx++] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val managedBlacklistPackages =
                    (values[idx++] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val managedSharedUserIdBlacklist =
                    (values[idx++] as? List<*>)?.filterIsInstance<SharedUid>() ?: emptyList()
                val managedSharedUserIdExemptPkg =
                    (values[idx++] as? List<*>)?.filterIsInstance<NamedPackage>() ?: emptyList()
                val uninstallFlags = values[idx++] as Int
                val adbVerifyEnabled = values[idx++] as Boolean
                val isIgnoringBatteryOptimizations = values[idx++] as Boolean
                val labRootModuleFlash = values[idx++] as Boolean
                val labRootShowModuleArt = values[idx++] as Boolean
                val labRootModuleAlwaysUseRoot = values[idx++] as Boolean
                val labRootImplementation = values[idx++] as RootImplementation
                val labHttpProfile = values[idx++] as HttpProfile
                val labHttpSaveFile = values[idx++] as Boolean
                val labSetInstallRequester = values[idx++] as Boolean
                val enableFileLogging = values[idx++] as Boolean
                val themeMode = values[idx++] as ThemeMode
                val paletteStyle = values[idx++] as PaletteStyle
                val useDynamicColor = values[idx++] as Boolean
                val useMiuixMonet = values[idx++] as Boolean
                val manualSeedColor = values[idx++] as Color
                @Suppress("UNCHECKED_CAST") val wallpaperColors = values[idx++] as? List<Int>
                val useDynColorFollowPkgIcon = values[idx++] as Boolean
                val useDynColorFollowPkgIconForLiveActivity = values[idx++] as Boolean
                val useBlur = values[idx++] as Boolean
                val updateResult = values[idx] as UpdateChecker.CheckResult?

                val effectiveSeedColor: Color = if (useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    if (!wallpaperColors.isNullOrEmpty()) {
                        if (wallpaperColors.contains(manualSeedColor.toArgb())) {
                            manualSeedColor
                        } else Color(wallpaperColors[0])
                    } else manualSeedColor
                } else {
                    if (PresetColors.any { it.color == manualSeedColor }) {
                        manualSeedColor
                    } else PresetColors[0].color
                }

                val availableColors: List<RawColor> = if (useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    if (!wallpaperColors.isNullOrEmpty()) {
                        wallpaperColors.map { colorInt ->
                            RawColor(
                                key = colorInt.toHexString(),
                                color = Color(colorInt)
                            )
                        }
                    } else PresetColors
                else PresetColors
                val hasUpdate = updateResult?.hasUpdate ?: false
                val remoteVersion = updateResult?.remoteVersion ?: ""
                val customizeAuthorizer =
                    if (authorizer == ConfigEntity.Authorizer.Customize) customize else ""
                PreferredViewState(
                    progress = PreferredViewState.Progress.Loaded,
                    authorizer = authorizer,
                    customizeAuthorizer = customizeAuthorizer,
                    installMode = installMode,
                    showDialogInstallExtendedMenu = showMenu,
                    showSmartSuggestion = showSuggestion,
                    disableNotificationForDialogInstall = showNotification,
                    showDialogWhenPressingNotification = showDialog,
                    dhizukuAutoCloseCountDown = countDown,
                    notificationSuccessAutoClearSeconds = notificationSuccessAutoClearSeconds,
                    versionCompareInSingleLine = versionCompareInMultiLine,
                    sdkCompareInMultiLine = sdkCompareInSingleLine,
                    showOPPOSpecial = showOPPOSpecial,
                    showExpressiveUI = showExpressiveUI,
                    installerRequireBiometricAuth = installerRequireBiometricAuth,
                    uninstallerRequireBiometricAuth = uninstallerRequireBiometricAuth,
                    showLiveActivity = showLiveActivity,
                    autoLockInstaller = autoLockInstaller,
                    autoSilentInstall = autoSilentInstall,
                    showMiuixUI = showMiuixUI,
                    preferSystemIcon = preferSystemIcon,
                    showLauncherIcon = showLauncherIcon,
                    managedInstallerPackages = managedInstallerPackages,
                    managedBlacklistPackages = managedBlacklistPackages,
                    managedSharedUserIdBlacklist = managedSharedUserIdBlacklist,
                    managedSharedUserIdExemptedPackages = managedSharedUserIdExemptPkg,
                    adbVerifyEnabled = adbVerifyEnabled,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    labRootEnableModuleFlash = labRootModuleFlash,
                    labRootShowModuleArt = labRootShowModuleArt,
                    labRootModuleAlwaysUseRoot = labRootModuleAlwaysUseRoot,
                    labRootImplementation = labRootImplementation,
                    labHttpProfile = labHttpProfile,
                    labHttpSaveFile = labHttpSaveFile,
                    labSetInstallRequester = labSetInstallRequester,
                    themeMode = themeMode,
                    paletteStyle = paletteStyle,
                    useDynamicColor = useDynamicColor,
                    useMiuixMonet = useMiuixMonet,
                    seedColor = effectiveSeedColor,
                    availableColors = availableColors,
                    useDynColorFollowPkgIcon = useDynColorFollowPkgIcon,
                    useDynColorFollowPkgIconForLiveActivity = useDynColorFollowPkgIconForLiveActivity,
                    useBlur = useBlur,
                    hasUpdate = hasUpdate,
                    remoteVersion = remoteVersion,
                    uninstallFlags = uninstallFlags,
                    enableFileLogging = enableFileLogging
                )
            }.collectLatest { state = it }
        }
}