package com.rosan.installer.ui.page.main.settings.preferred

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.updater.repo.UpdateChecker
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.NamedPackage
import com.rosan.installer.domain.settings.model.SharedUid
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.NamedPackageListSetting
import com.rosan.installer.domain.settings.repository.SharedUidListSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.usecase.settings.PerformAppUpdateUseCase
import com.rosan.installer.domain.settings.usecase.settings.SetLauncherIconUseCase
import com.rosan.installer.domain.settings.usecase.settings.ToggleUninstallFlagUseCase
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.theme.material.RawColor
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

class PreferredViewModel(
    private val appSettingsRepo: AppSettingsRepo,
    private val updateChecker: UpdateChecker,
    private val systemEnvProvider: SystemEnvProvider,
    private val privilegedProvider: PrivilegedProvider,
    private val toggleUninstallFlagUseCase: ToggleUninstallFlagUseCase,
    private val performAppUpdateUseCase: PerformAppUpdateUseCase,
    private val setLauncherIconUseCase: SetLauncherIconUseCase
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<PreferredViewEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents = _uiEvents.asSharedFlow()

    var pendingNavigateToTheme by mutableStateOf(false)
        private set

    // --- External Environment State Flows ---
    private val updateResultFlow = MutableStateFlow<UpdateChecker.CheckResult?>(null)
    private val adbVerifyEnabledFlow = MutableStateFlow(true)
    private val isIgnoringBatteryOptFlow = MutableStateFlow(true)

    // 1. Intermediate data class to hold all external environment states
    private data class EnvState(
        val adbVerifyEnabled: Boolean = true,
        val isIgnoringBatteryOpt: Boolean = true,
        val wallpaperColors: List<Int>? = null,
        val updateResult: UpdateChecker.CheckResult? = null
    )

    // 2. Aggregate all environment flows into a single flow
    private val envStateFlow = combine(
        adbVerifyEnabledFlow,
        isIgnoringBatteryOptFlow,
        systemEnvProvider.getWallpaperColorsFlow(),
        updateResultFlow
    ) { adbVerify, batteryOpt, wallpaperColors, updateResult ->
        EnvState(
            adbVerifyEnabled = adbVerify,
            isIgnoringBatteryOpt = batteryOpt,
            wallpaperColors = wallpaperColors,
            updateResult = updateResult
        )
    }

    // 3. The final UI StateFlow, combining domain preferences and environment states
    val state: StateFlow<PreferredViewState> = combine(
        appSettingsRepo.preferencesFlow,
        envStateFlow
    ) { prefs, env ->
        // Calculate effective colors based on OS version and user selection
        val manualSeedColor = Color(prefs.seedColorInt)
        val effectiveSeedColor: Color = if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (!env.wallpaperColors.isNullOrEmpty()) {
                if (env.wallpaperColors.contains(manualSeedColor.toArgb())) {
                    manualSeedColor
                } else Color(env.wallpaperColors[0])
            } else manualSeedColor
        } else {
            if (PresetColors.any { it.color == manualSeedColor }) manualSeedColor else PresetColors[0].color
        }

        val availableColors: List<RawColor> = if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (!env.wallpaperColors.isNullOrEmpty()) {
                env.wallpaperColors.map { colorInt ->
                    RawColor(key = colorInt.toHexString(), color = Color(colorInt))
                }
            } else PresetColors
        } else PresetColors

        val customizeAuthorizer = if (prefs.authorizer == Authorizer.Customize) prefs.customizeAuthorizer else ""

        // Map AppPreferences and EnvState to UI State
        PreferredViewState(
            progress = PreferredViewState.Progress.Loaded,
            authorizer = prefs.authorizer,
            customizeAuthorizer = customizeAuthorizer,
            installMode = prefs.installMode,
            showDialogInstallExtendedMenu = prefs.showDialogInstallExtendedMenu,
            showSmartSuggestion = prefs.showSmartSuggestion,
            disableNotificationForDialogInstall = prefs.disableNotificationForDialogInstall,
            showDialogWhenPressingNotification = prefs.showDialogWhenPressingNotification,
            dhizukuAutoCloseCountDown = prefs.dhizukuAutoCloseCountDown,
            notificationSuccessAutoClearSeconds = prefs.notificationSuccessAutoClearSeconds,
            versionCompareInSingleLine = prefs.versionCompareInSingleLine,
            sdkCompareInMultiLine = prefs.sdkCompareInMultiLine,
            showOPPOSpecial = prefs.showOPPOSpecial,
            showExpressiveUI = prefs.showExpressiveUI,
            installerRequireBiometricAuth = prefs.installerRequireBiometricAuth,
            uninstallerRequireBiometricAuth = prefs.uninstallerRequireBiometricAuth,
            showLiveActivity = prefs.showLiveActivity,
            autoLockInstaller = prefs.autoLockInstaller,
            autoSilentInstall = prefs.autoSilentInstall,
            showMiuixUI = prefs.showMiuixUI,
            preferSystemIcon = prefs.preferSystemIcon,
            showLauncherIcon = prefs.showLauncherIcon,
            managedInstallerPackages = prefs.managedInstallerPackages,
            managedBlacklistPackages = prefs.managedBlacklistPackages,
            managedSharedUserIdBlacklist = prefs.managedSharedUserIdBlacklist,
            managedSharedUserIdExemptedPackages = prefs.managedSharedUserIdExemptedPackages,
            labRootEnableModuleFlash = prefs.labRootEnableModuleFlash,
            labRootShowModuleArt = prefs.labRootShowModuleArt,
            labRootModuleAlwaysUseRoot = prefs.labRootModuleAlwaysUseRoot,
            labRootImplementation = prefs.labRootImplementation,
            labUseMiIsland = prefs.labUseMiIsland,
            labHttpProfile = prefs.labHttpProfile,
            labHttpSaveFile = prefs.labHttpSaveFile,
            labSetInstallRequester = prefs.labSetInstallRequester,
            themeMode = prefs.themeMode,
            paletteStyle = prefs.paletteStyle,
            colorSpec = prefs.colorSpec,
            useDynamicColor = prefs.useDynamicColor,
            useMiuixMonet = prefs.useMiuixMonet,
            seedColor = effectiveSeedColor,
            availableColors = availableColors,
            useDynColorFollowPkgIcon = prefs.useDynColorFollowPkgIcon,
            useDynColorFollowPkgIconForLiveActivity = prefs.useDynColorFollowPkgIconForLiveActivity,
            useBlur = prefs.useBlur,
            uninstallFlags = prefs.uninstallFlags,
            enableFileLogging = prefs.enableFileLogging,

            // Extract external states from the aggregated EnvState
            adbVerifyEnabled = env.adbVerifyEnabled,
            isIgnoringBatteryOptimizations = env.isIgnoringBatteryOpt,
            hasUpdate = env.updateResult?.hasUpdate ?: false,
            remoteVersion = env.updateResult?.remoteVersion ?: ""
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PreferredViewState(progress = PreferredViewState.Progress.Loading)
    )

    init {
        // Automatically fetch external statuses when ViewModel is created
        refreshIgnoreBatteryOptStatus()
        refreshAdbVerifyStatus()
        checkUpdate()
    }

    fun markPendingNavigateToTheme(pending: Boolean) {
        pendingNavigateToTheme = pending
    }

    fun dispatch(action: PreferredViewAction) {
        when (action) {
            is PreferredViewAction.Update -> performInAppUpdate()

            // --- Unified Setting Dispatchers ---
            is PreferredViewAction.ChangeGlobalAuthorizer -> updateSetting(StringSetting.Authorizer, action.authorizer.value)
            is PreferredViewAction.ChangeGlobalCustomizeAuthorizer -> updateSetting(
                StringSetting.CustomizeAuthorizer,
                action.customizeAuthorizer.takeIf { state.value.authorizerCustomize } ?: "")

            is PreferredViewAction.ChangeGlobalInstallMode -> updateSetting(StringSetting.InstallMode, action.installMode.value)

            is PreferredViewAction.ChangeShowDialogInstallExtendedMenu -> updateSetting(BooleanSetting.DialogShowExtendedMenu, action.showMenu)
            is PreferredViewAction.ChangeShowSuggestion -> updateSetting(BooleanSetting.DialogShowIntelligentSuggestion, action.showSuggestion)
            is PreferredViewAction.ChangeShowDisableNotification -> updateSetting(
                BooleanSetting.DialogDisableNotificationOnDismiss,
                action.showDisableNotification
            )

            is PreferredViewAction.ChangeShowDialogWhenPressingNotification -> updateSetting(
                BooleanSetting.ShowDialogWhenPressingNotification,
                action.showDialog
            )

            is PreferredViewAction.ChangeDhizukuAutoCloseCountDown -> {
                if (action.countDown in 1..10) updateSetting(IntSetting.DialogAutoCloseCountdown, action.countDown)
            }

            is PreferredViewAction.ChangeNotificationSuccessAutoClearSeconds -> updateSetting(
                IntSetting.NotificationSuccessAutoClearSeconds,
                action.seconds
            )

            is PreferredViewAction.ChangeShowExpressiveUI -> updateSetting(BooleanSetting.UiExpressiveSwitch, action.showRefreshedUI)
            is PreferredViewAction.ChangeShowLiveActivity -> updateSetting(BooleanSetting.ShowLiveActivity, action.showLiveActivity)

            is PreferredViewAction.ChangeBiometricAuth -> changeBiometricAuth(action.biometricAuth, action.isInstaller)

            is PreferredViewAction.ChangeUseMiuix -> updateSetting(BooleanSetting.UiUseMiuix, action.useMiuix)
            is PreferredViewAction.ChangePreferSystemIcon -> updateSetting(BooleanSetting.PreferSystemIconForInstall, action.preferSystemIcon)

            is PreferredViewAction.ChangeShowLauncherIcon -> viewModelScope.launch { setLauncherIconUseCase(action.showLauncherIcon) }

            is PreferredViewAction.ChangeVersionCompareInSingleLine -> updateSetting(
                BooleanSetting.DialogVersionCompareSingleLine,
                action.versionCompareInSingleLine
            )

            is PreferredViewAction.ChangeSdkCompareInMultiLine -> updateSetting(
                BooleanSetting.DialogSdkCompareMultiLine,
                action.sdkCompareInMultiLine
            )

            is PreferredViewAction.ChangeShowOPPOSpecial -> updateSetting(BooleanSetting.DialogShowOppoSpecial, action.showOPPOSpecial)
            is PreferredViewAction.ChangeAutoLockInstaller -> updateSetting(BooleanSetting.AutoLockInstaller, action.autoLockInstaller)
            is PreferredViewAction.ChangeAutoSilentInstall -> updateSetting(BooleanSetting.DialogAutoSilentInstall, action.autoSilentInstall)

            // --- Collection Management ---
            is PreferredViewAction.AddManagedInstallerPackage -> addManagedPackage(
                state.value.managedInstallerPackages,
                NamedPackageListSetting.ManagedInstallerPackages,
                action.pkg
            )

            is PreferredViewAction.RemoveManagedInstallerPackage -> removeManagedPackage(
                state.value.managedInstallerPackages,
                NamedPackageListSetting.ManagedInstallerPackages,
                action.pkg
            )

            is PreferredViewAction.AddManagedBlacklistPackage -> addManagedPackage(
                state.value.managedBlacklistPackages,
                NamedPackageListSetting.ManagedBlacklistPackages,
                action.pkg
            )

            is PreferredViewAction.RemoveManagedBlacklistPackage -> removeManagedPackage(
                state.value.managedBlacklistPackages,
                NamedPackageListSetting.ManagedBlacklistPackages,
                action.pkg
            )

            is PreferredViewAction.AddManagedSharedUserIdBlacklist -> addSharedUserIdToBlacklist(action.uid)
            is PreferredViewAction.RemoveManagedSharedUserIdBlacklist -> removeSharedUserIdFromBlacklist(action.uid)
            is PreferredViewAction.AddManagedSharedUserIdExemptedPackages -> addManagedPackage(
                state.value.managedSharedUserIdExemptedPackages,
                NamedPackageListSetting.ManagedSharedUserIdExemptedPackages,
                action.pkg
            )

            is PreferredViewAction.RemoveManagedSharedUserIdExemptedPackages -> removeManagedPackage(
                state.value.managedSharedUserIdExemptedPackages,
                NamedPackageListSetting.ManagedSharedUserIdExemptedPackages,
                action.pkg
            )

            is PreferredViewAction.ToggleGlobalUninstallFlag -> toggleGlobalUninstallFlag(action.flag, action.enable)
            is PreferredViewAction.SetAdbVerifyEnabledState -> setAdbVerifyEnabled(action.enabled, action)
            is PreferredViewAction.RequestIgnoreBatteryOptimization -> requestIgnoreBatteryOptimization()
            is PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus -> refreshIgnoreBatteryOptStatus()
            is PreferredViewAction.SetDefaultInstaller -> setDefaultInstaller(action.lock, action)

            // --- Lab Settings ---
            is PreferredViewAction.LabChangeRootModuleFlash -> updateSetting(BooleanSetting.LabEnableModuleFlash, action.enable)
            is PreferredViewAction.LabChangeRootShowModuleArt -> updateSetting(BooleanSetting.LabModuleFlashShowArt, action.enable)
            is PreferredViewAction.LabChangeRootModuleAlwaysUseRoot -> updateSetting(BooleanSetting.LabModuleAlwaysRoot, action.enable)
            is PreferredViewAction.LabChangeRootImplementation -> updateSetting(StringSetting.LabRootImplementation, action.implementation.name)
            is PreferredViewAction.LabChangeUseMiIsland -> updateSetting(BooleanSetting.ShowMiIsland, action.enable)
            is PreferredViewAction.LabChangeHttpProfile -> updateSetting(StringSetting.LabHttpProfile, action.profile.name)
            is PreferredViewAction.LabChangeHttpSaveFile -> updateSetting(BooleanSetting.LabHttpSaveFile, action.enable)
            is PreferredViewAction.LabChangeSetInstallRequester -> updateSetting(BooleanSetting.LabSetInstallRequester, action.enable)

            // --- Theme Settings ---
            is PreferredViewAction.SetThemeMode -> updateSetting(StringSetting.ThemeMode, action.mode.name)
            is PreferredViewAction.SetPaletteStyle -> updateSetting(StringSetting.ThemePaletteStyle, action.style.name)
            is PreferredViewAction.SetColorSpec -> updateSetting(StringSetting.ThemeColorSpec, action.spec.name)
            is PreferredViewAction.SetUseDynamicColor -> updateSetting(BooleanSetting.ThemeUseDynamicColor, action.use)
            is PreferredViewAction.SetUseMiuixMonet -> updateSetting(BooleanSetting.UiUseMiuixMonet, action.use)
            is PreferredViewAction.SetSeedColor -> updateSetting(IntSetting.ThemeSeedColor, action.color.toArgb())
            is PreferredViewAction.SetDynColorFollowPkgIcon -> updateSetting(BooleanSetting.UiDynColorFollowPkgIcon, action.follow)
            is PreferredViewAction.SetDynColorFollowPkgIconForLiveActivity -> updateSetting(
                BooleanSetting.LiveActivityDynColorFollowPkgIcon,
                action.follow
            )

            is PreferredViewAction.SetUseBlur -> updateSetting(BooleanSetting.UiUseBlur, action.enable)

            is PreferredViewAction.SetEnableFileLogging -> updateSetting(BooleanSetting.EnableFileLogging, action.enable)
            is PreferredViewAction.ShareLog -> shareLog()
        }
    }

    // --- DataStore Helpers ---
    private fun updateSetting(setting: StringSetting, value: String) = viewModelScope.launch { appSettingsRepo.putString(setting, value) }
    private fun updateSetting(setting: BooleanSetting, value: Boolean) = viewModelScope.launch { appSettingsRepo.putBoolean(setting, value) }
    private fun updateSetting(setting: IntSetting, value: Int) = viewModelScope.launch { appSettingsRepo.putInt(setting, value) }

    private fun performInAppUpdate() = viewModelScope.launch {
        _uiEvents.emit(PreferredViewEvent.ShowUpdateLoading)
        runCatching {
            performAppUpdateUseCase(updateResultFlow.value, state.value.authorizer)
        }.onFailure { e ->
            Timber.e(e, "In-app update failed")
            _uiEvents.emit(PreferredViewEvent.ShowInAppUpdateErrorDetail("Update Failed", e))
        }
        _uiEvents.emit(PreferredViewEvent.HideUpdateLoading)
    }

    private fun changeBiometricAuth(biometricAuth: Boolean, isInstaller: Boolean) = viewModelScope.launch {
        if (systemEnvProvider.authenticateBiometric(isInstaller)) {
            val setting = if (isInstaller) BooleanSetting.InstallerRequireBiometricAuth else BooleanSetting.UninstallerRequireBiometricAuth
            updateSetting(setting, biometricAuth)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            systemEnvProvider.requestIgnoreBatteryOptimization()
        } catch (e: Exception) {
            viewModelScope.launch { _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerResult("Could not start activity")) }
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
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail("Failed to change ADB verify", e, action))
        }
    }

    private fun refreshAdbVerifyStatus() = viewModelScope.launch {
        systemEnvProvider.isAdbVerifyEnabledFlow().collect { enabled ->
            adbVerifyEnabledFlow.value = enabled
        }
    }

    private fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        val result = updateChecker.check()
        if (result != null) updateResultFlow.value = result
    }

    private fun setDefaultInstaller(lock: Boolean, action: PreferredViewAction) = viewModelScope.launch {
        runCatching {
            privilegedProvider.setDefaultInstaller(state.value.authorizer, lock)
        }.onSuccess {
            val successMsg = if (lock) "Lock Success" else "Unlock Success"
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerResult(successMsg))
        }.onFailure { e ->
            val errorMsg = if (lock) "Lock Failed" else "Unlock Failed"
            _uiEvents.emit(PreferredViewEvent.ShowDefaultInstallerErrorDetail(errorMsg, e, action))
        }
    }

    private fun shareLog() = viewModelScope.launch {
        try {
            val uriStr = systemEnvProvider.getLatestLogUri()
            if (uriStr == null) {
                _uiEvents.emit(PreferredViewEvent.ShareLogFailed("Log file is missing or empty"))
            } else {
                // UI layer can parse the string back to Uri for intent sharing
                // _uiEvents.emit(PreferredViewEvent.OpenLogShare(android.net.Uri.parse(uriStr)))
            }
        } catch (e: Exception) {
            _uiEvents.emit(PreferredViewEvent.ShareLogFailed(e.message ?: "Failed"))
        }
    }

    private fun toggleGlobalUninstallFlag(flag: Int, enable: Boolean) = viewModelScope.launch {
        val disabledFlag = toggleUninstallFlagUseCase(flag, enable)
        if (disabledFlag != null) {
            val resId = if (disabledFlag == PackageManagerUtil.DELETE_SYSTEM_APP)
                R.string.uninstall_system_app_disabled
            else
                R.string.uninstall_all_users_disabled
            _uiEvents.tryEmit(PreferredViewEvent.ShowMessage(resId))
        }
    }

    private fun addManagedPackage(list: List<NamedPackage>, key: NamedPackageListSetting, pkg: NamedPackage) = viewModelScope.launch {
        val currentList = list.toMutableList()
        if (!currentList.contains(pkg)) {
            currentList.add(pkg)
            appSettingsRepo.putNamedPackageList(key, currentList)
        }
    }

    private fun removeManagedPackage(list: List<NamedPackage>, key: NamedPackageListSetting, pkg: NamedPackage) = viewModelScope.launch {
        val currentList = list.toMutableList()
        currentList.remove(pkg)
        appSettingsRepo.putNamedPackageList(key, currentList)
    }

    private fun addSharedUserIdToBlacklist(uid: SharedUid) = viewModelScope.launch {
        val currentList = state.value.managedSharedUserIdBlacklist
        if (uid !in currentList) appSettingsRepo.putSharedUidList(SharedUidListSetting.ManagedSharedUserIdBlacklist, currentList + uid)
    }

    private fun removeSharedUserIdFromBlacklist(uid: SharedUid) = viewModelScope.launch {
        val currentList = state.value.managedSharedUserIdBlacklist
        if (uid in currentList) appSettingsRepo.putSharedUidList(
            SharedUidListSetting.ManagedSharedUserIdBlacklist,
            currentList.filter { it != uid })
    }
}