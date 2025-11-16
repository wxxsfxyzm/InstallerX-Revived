package com.rosan.installer.ui.page.main.installer.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyant.m3color.quantize.QuantizerCelebi
import com.kyant.m3color.score.Score
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.exception.ModuleInstallExitCodeNonZeroException
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.repo.PARepo
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil.Companion.readGlobal
import com.rosan.installer.ui.page.main.installer.dialog.inner.UiText
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class InstallerViewModel(
    private var repo: InstallerRepo,
    private val appDataStore: AppDataStore,
    private val appIconRepo: AppIconRepo,
    private val paRepo: PARepo
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf<InstallerViewState>(InstallerViewState.Ready)
        private set

    /**
     * Checks if the current selection for installation contains at least one module.
     * This is determined by checking if any selected entity is of type ModuleEntity.
     */
    val isInstallingModule: Boolean
        get() = repo.analysisResults.any { result ->
            result.appEntities.any { entity -> entity.selected && entity.app is AppEntity.ModuleEntity }
        }

    // Hold the original, complete analysis results for multi-install scenarios.
    private var originalAnalysisResults: List<PackageAnalysisResult> = emptyList()

    var showMiuixSheetRightActionSettings by mutableStateOf(false)
        private set
    var showMiuixPermissionList by mutableStateOf(false)
        private set
    var navigatedFromPrepareToChoice by mutableStateOf(false)
        private set
    var autoCloseCountDown by mutableIntStateOf(3)
        private set
    var showExtendedMenu by mutableStateOf(false)
        private set
    var showSmartSuggestion by mutableStateOf(true)
        private set
    var disableNotificationOnDismiss by mutableStateOf(false)
        private set
    var versionCompareInSingleLine by mutableStateOf(false)
        private set
    var sdkCompareInMultiLine by mutableStateOf(false)
        private set
    var useDynColorFollowPkgIcon by mutableStateOf(false)
        private set
    var showOPPOSpecial by mutableStateOf(false)
    private var autoSilentInstall by mutableStateOf(false)
    var enableModuleInstall by mutableStateOf(false)

    // Text to show in the progress bar
    private val _installProgressText = MutableStateFlow<UiText?>(null)
    val installProgressText: StateFlow<UiText?> = _installProgressText.asStateFlow()

    // Progress to drive the progress bar
    private val _installProgress = MutableStateFlow<Float?>(null)
    val installProgress: StateFlow<Float?> = _installProgress.asStateFlow()

    // Queue and result for multi-install scenarios
    private var multiInstallQueue: List<SelectInstallEntity> = emptyList()
    private val multiInstallResults = mutableListOf<InstallResult>()
    private var currentMultiInstallIndex = 0

    /**
     * Determines if the dialog can be dismissed by tapping the scrim.
     * Dismissal is disallowed during ongoing operations like installing.
     */
    val isDismissible
        get() = when (state) {
            is InstallerViewState.Analysing,
            is InstallerViewState.Resolving,
            is InstallerViewState.Preparing,
            is InstallerViewState.InstallExtendedMenu,
            is InstallerViewState.InstallChoice -> false

            is InstallerViewState.InstallingModule -> (state as InstallerViewState.InstallingModule).isFinished

            is InstallerViewState.InstallPrepare -> !(showMiuixSheetRightActionSettings || showMiuixPermissionList)
            is InstallerViewState.Installing -> !disableNotificationOnDismiss
            else -> true
        }

    private val _currentPackageName = MutableStateFlow<String?>(null)
    val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    private val _displayIcons = MutableStateFlow<Map<String, Drawable?>>(emptyMap())
    val displayIcons: StateFlow<Map<String, Drawable?>> = _displayIcons.asStateFlow()

    var preferSystemIconForUpdates by mutableStateOf(false)
        private set

    // --- StateFlow to hold the seed color extracted from the icon ---
    private val _seedColor = MutableStateFlow<Color?>(null)
    val seedColor: StateFlow<Color?> = _seedColor.asStateFlow()

    // StateFlow to manage `install flags`
    // An Int value formed by combining all options using bitwise operations.
    private val _installFlags = MutableStateFlow(0) // 默认值为0，表示没有开启任何选项
    val installFlags: StateFlow<Int> = _installFlags.asStateFlow()

    // StateFlow to hold the default installer package name from global settings.
    private val _defaultInstallerFromSettings = MutableStateFlow(repo.config.installer)
    val defaultInstallerFromSettings: StateFlow<String?> = _defaultInstallerFromSettings.asStateFlow()

    // StateFlow to hold the list of managed installer packages.
    private val _managedInstallerPackages = MutableStateFlow<List<NamedPackage>>(emptyList())
    val managedInstallerPackages: StateFlow<List<NamedPackage>> = _managedInstallerPackages.asStateFlow()

    // StateFlow to hold the currently selected installer package name.
    private val _selectedInstaller = MutableStateFlow(repo.config.installer)
    val selectedInstaller: StateFlow<String?> = _selectedInstaller.asStateFlow()

    // StateFlow to hold the list of available users.
    private val _availableUsers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val availableUsers: StateFlow<Map<Int, String>> = _availableUsers.asStateFlow()

    // StateFlow to hold the currently selected user ID.
    private val _selectedUserId = MutableStateFlow(0)
    val selectedUserId: StateFlow<Int> = _selectedUserId.asStateFlow()

    /**
     * Holds information about the app being uninstalled for UI display.
     * This is separate from repo.uninstallInfo to prevent info from being cleared.
     */
    private val _uiUninstallInfo = MutableStateFlow<UninstallInfo?>(null)
    val uiUninstallInfo: StateFlow<UninstallInfo?> = _uiUninstallInfo.asStateFlow()

    /**
     * Holds the bitmask for uninstall flags (e.g., KEEP_DATA).
     */
    private val _uninstallFlags = MutableStateFlow(0)
    val uninstallFlags: StateFlow<Int> = _uninstallFlags.asStateFlow()

    /**
     * Flag to track if the current operation is an uninstall-and-retry flow.
     * This helps the progress collector know when to trigger a reinstall.
     */
    private var isRetryingInstall = false

    private var loadingStateJob: Job? = null
    private val iconJobs = mutableMapOf<String, Job>()
    private var autoInstallJob: Job? = null
    private var collectRepoJob: Job? = null

    init {
        Timber.d("DialogViewModel init")
        viewModelScope.launch {
            preferSystemIconForUpdates =
                appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false).first()
            autoCloseCountDown =
                appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3).first()
            showExtendedMenu =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, false).first()
            showSmartSuggestion =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, true).first()
            disableNotificationOnDismiss =
                appDataStore.getBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, false).first()
            versionCompareInSingleLine =
                appDataStore.getBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, false).first()
            sdkCompareInMultiLine =
                appDataStore.getBoolean(AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE, false).first()
            showOPPOSpecial =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_OPPO_SPECIAL, false).first()
            autoSilentInstall =
                appDataStore.getBoolean(AppDataStore.DIALOG_AUTO_SILENT_INSTALL, false).first()
            enableModuleInstall =
                appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false).first()
            useDynColorFollowPkgIcon =
                appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
            // Load managed packages for installer selection.
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST).collect { packages ->
                _managedInstallerPackages.value = packages
            }
        }
    }

    fun dispatch(action: InstallerViewAction) {
        when (action) {
            is InstallerViewAction.CollectRepo -> collectRepo(action.repo)
            is InstallerViewAction.Close -> close()
            is InstallerViewAction.Analyse -> analyse()
            is InstallerViewAction.InstallChoice -> {
                // Check if navigating from*InstallPrepare
                navigatedFromPrepareToChoice = state is InstallerViewState.InstallPrepare
                installChoice()
            }

            is InstallerViewAction.InstallPrepare -> installPrepare()
            is InstallerViewAction.InstallExtendedMenu -> installExtendedMenu()
            is InstallerViewAction.InstallExtendedSubMenu -> installExtendedSubMenu()
            is InstallerViewAction.InstallMultiple -> installMultiple()
            is InstallerViewAction.Install -> install()
            is InstallerViewAction.Background -> background()
            is InstallerViewAction.UninstallAndRetryInstall -> uninstallAndRetryInstall(action.keepData)
            is InstallerViewAction.Uninstall -> {
                // Trigger uninstall using the package name from the collected info
                repo.uninstallInfo.value?.packageName?.let { repo.uninstall(it) }
            }

            is InstallerViewAction.ShowMiuixSheetRightActionSettings -> showMiuixSheetRightActionSettings = true
            is InstallerViewAction.HideMiuixSheetRightActionSettings -> showMiuixSheetRightActionSettings = false
            is InstallerViewAction.ShowMiuixPermissionList -> showMiuixPermissionList = true
            is InstallerViewAction.HideMiuixPermissionList -> showMiuixPermissionList = false

            is InstallerViewAction.ToggleSelection -> toggleSelection(
                action.packageName,
                action.entity,
                action.isMultiSelect
            )

            is InstallerViewAction.ToggleUninstallFlag -> {
                // Update the uninstall flags bitmask
                val currentFlags = _uninstallFlags.value
                _uninstallFlags.value = if (action.enable) {
                    currentFlags or action.flag
                } else {
                    currentFlags and action.flag.inv()
                }
                // Sync to the repo config so the backend uses the flags
                repo.config.uninstallFlags = _uninstallFlags.value
            }

            is InstallerViewAction.SetInstaller -> selectInstaller(action.installer)
            is InstallerViewAction.SetTargetUser -> selectTargetUser(action.userId)
        }
    }

    private fun collectRepo(repo: InstallerRepo) {
        this.repo = repo
        // Load/reload available users based on the new repo's config
        if (repo.config.enableCustomizeUser)
            loadAvailableUsers(repo.config.authorizer)
        // initialize install flags based on repo.config
        _installFlags.value = listOfNotNull(
            repo.config.allowTestOnly.takeIf { it }
                ?.let { InstallOption.AllowTest.value },
            repo.config.allowDowngrade.takeIf { it }
                ?.let { InstallOption.AllowDowngrade.value },
            repo.config.forAllUser.takeIf { it }
                ?.let { InstallOption.AllUsers.value },
            repo.config.allowRestrictedPermissions.takeIf { it }
                ?.let { InstallOption.AllWhitelistRestrictedPermissions.value },
            repo.config.bypassLowTargetSdk.takeIf { it }
                ?.let { InstallOption.BypassLowTargetSdkBlock.value },
            repo.config.allowAllRequestedPermissions.takeIf { it }
                ?.let { InstallOption.GrantAllRequestedPermissions.value }
        ).fold(0) { acc, flag -> acc or flag }
        // sync to repo.config
        repo.config.installFlags = _installFlags.value
        _currentPackageName.value = null
        val newPackageNames = repo.analysisResults.map { it.packageName }.toSet()
        _displayIcons.update { old -> old.filterKeys { it in newPackageNames } }
        collectRepoJob?.cancel()
        autoInstallJob?.cancel()

        collectRepoJob = viewModelScope.launch {
            repo.progress.collect { progress ->
                // 如果正在进行批量安装，则由专门的逻辑处理
                if (multiInstallQueue.isNotEmpty()) {
                    handleMultiInstallProgress(progress)
                    return@collect
                }
                if (progress is ProgressEntity.InstallAnalysedSuccess || progress is ProgressEntity.InstallAnalysedFailed) {
                    loadingStateJob?.cancel()
                    loadingStateJob = null
                }
                when (progress) {
                    is ProgressEntity.InstallResolving,
                    is ProgressEntity.InstallPreparing,
                    is ProgressEntity.InstallAnalysing -> {
                        if (loadingStateJob == null || loadingStateJob?.isActive == false) {
                            loadingStateJob = viewModelScope.launch {
                                delay(200L)
                                state = if (progress is ProgressEntity.InstallPreparing) {
                                    InstallerViewState.Preparing(progress.progress)
                                } else {
                                    InstallerViewState.Analysing
                                }
                            }
                        }
                        return@collect
                    }

                    else -> {}
                }

                val previousState = state
                var newState: InstallerViewState
                var newPackageNameFromProgress: String? = _currentPackageName.value

                when (progress) {
                    is ProgressEntity.Ready -> {
                        newState = InstallerViewState.Ready
                        newPackageNameFromProgress = null
                        _seedColor.value = null
                    }

                    is ProgressEntity.UninstallResolveFailed,
                    is ProgressEntity.InstallResolvedFailed -> newState = InstallerViewState.ResolveFailed

                    is ProgressEntity.InstallAnalysedFailed -> newState = InstallerViewState.AnalyseFailed
                    is ProgressEntity.InstallAnalysedSuccess -> {
                        // When analysis is successful, this is the first moment we have the full, original list.
                        // This is the correct time to back it up.
                        if (originalAnalysisResults.isEmpty()) {
                            originalAnalysisResults = repo.analysisResults
                        }
                        val analysisResults = repo.analysisResults

                        // The decision to show the choice screen should not only depend on the number of packages,
                        // but also on the container type determined by the analyser.
                        // If the analyser found a ZIP with multiple APKs for the SAME package,
                        // analysisResults.size would be 1, but we still need to show the choice screen.
                        val containerType = analysisResults.firstOrNull()
                            ?.appEntities?.firstOrNull()
                            ?.app?.containerType

                        val isMultiAppMode = analysisResults.size > 1 ||
                                containerType == DataType.MULTI_APK ||
                                containerType == DataType.MULTI_APK_ZIP ||
                                containerType == DataType.MIXED_MODULE_APK ||
                                containerType == DataType.MIXED_MODULE_ZIP

                        if (isMultiAppMode) {
                            // If the backend (ActionHandler) determined it's a multi-app scenario,
                            // ALWAYS go to the choice screen, regardless of package names.
                            Timber.d("ViewModel: Multi-app mode detected. Forcing InstallChoice state.")
                            newState = InstallerViewState.InstallChoice
                            newPackageNameFromProgress = null // No single package is the focus.

                            // Trigger icon loading for all apps in the list.
                            analysisResults.forEach { result ->
                                loadDisplayIcon(result.packageName)
                            }

                            if (useDynColorFollowPkgIcon) {
                                val colorInt = repo.analysisResults.firstNotNullOfOrNull { it.seedColor }
                                _seedColor.value = colorInt?.let { Color(it) }
                            }
                        } else {
                            // If it's not a multi-app scenario (e.g., single APK with splits),
                            // it's safe to proceed directly to the prepare screen for the single app.
                            Timber.d("ViewModel: Single-app mode detected. Proceeding to InstallPrepare.")
                            newState = InstallerViewState.InstallPrepare
                            newPackageNameFromProgress = analysisResults.firstOrNull()?.packageName

                            if (useDynColorFollowPkgIcon) {
                                val colorInt = analysisResults.firstOrNull()?.seedColor
                                _seedColor.value = colorInt?.let { Color(it) }
                            }
                        }
                    }

                    is ProgressEntity.Installing -> {
                        newState = InstallerViewState.Installing
                        autoInstallJob?.cancel()
                        if (newPackageNameFromProgress == null && repo.analysisResults.size == 1) {
                            newPackageNameFromProgress = repo.analysisResults.first().packageName
                        }
                    }

                    is ProgressEntity.InstallFailed -> {
                        autoInstallJob?.cancel()
                        // If we were installing a module, just mark it as finished instead of switching state.
                        if (state is InstallerViewState.InstallingModule && repo.error is ModuleInstallExitCodeNonZeroException) {
                            // Get the current list of output lines from the UI state.
                            val currentOutput = (state as InstallerViewState.InstallingModule).output.toMutableList()

                            // Get the error message from the repository, which ActionHandler has set.
                            val errorMessage = repo.error.message
                            if (!errorMessage.isNullOrBlank()) {
                                currentOutput.add("ERROR: $errorMessage")
                            }

                            // Create the new, final state with the updated output and the finished flag.
                            newState = (state as InstallerViewState.InstallingModule).copy(
                                output = currentOutput,
                                isFinished = true
                            )
                        } else {
                            newState = InstallerViewState.InstallFailed
                            if (newPackageNameFromProgress == null && repo.analysisResults.size == 1) {
                                newPackageNameFromProgress = repo.analysisResults.first().packageName
                            }
                        }
                    }

                    is ProgressEntity.InstallSuccess -> {
                        autoInstallJob?.cancel()
                        // If a module installation succeeded, just mark it as finished.
                        if (state is InstallerViewState.InstallingModule) {
                            newState = (state as InstallerViewState.InstallingModule).copy(isFinished = true)
                        } else {
                            newState = InstallerViewState.InstallSuccess
                            if (newPackageNameFromProgress == null && repo.analysisResults.size == 1) {
                                newPackageNameFromProgress = repo.analysisResults.first().packageName
                            }
                        }
                    }

                    is ProgressEntity.InstallingModule -> {
                        newState = InstallerViewState.InstallingModule(progress.output)
                    }

                    is ProgressEntity.Uninstalling -> {
                        newState = if (isRetryingInstall) {
                            //isRetryingInstall = false
                            InstallerViewState.InstallRetryDowngradeUsingUninstall
                        } else {
                            InstallerViewState.Uninstalling
                        }
                    }

                    is ProgressEntity.UninstallFailed -> {
                        // If uninstall fails during retry, revert to install failed state
                        if (isRetryingInstall) {
                            isRetryingInstall = false
                            newState = InstallerViewState.InstallFailed
                        } else {
                            newState = InstallerViewState.UninstallFailed
                        }
                    }

                    is ProgressEntity.UninstallSuccess -> {
                        // If uninstall succeeded as part of a retry, trigger the install
                        if (isRetryingInstall) {
                            isRetryingInstall = false
                            repo.install()
                            // Stay in a transitional state until Install starts
                            newState = InstallerViewState.InstallRetryDowngradeUsingUninstall
                        } else {
                            // now it has a meaning of normal uninstall success
                            newState = InstallerViewState.UninstallSuccess
                        }
                    }

                    is ProgressEntity.UninstallReady -> {
                        _uiUninstallInfo.value = repo.uninstallInfo.value
                        _uninstallFlags.value = 0 // Reset flags for new session
                        repo.config.uninstallFlags = 0
                        newState = InstallerViewState.UninstallReady

                        if (useDynColorFollowPkgIcon) {
                            val icon = repo.uninstallInfo.value?.appIcon
                            if (icon != null) {
                                viewModelScope.launch(Dispatchers.Default) {
                                    try {
                                        val bitmap = drawableToBitmap(icon)
                                        val colorInt = extractSeedColorFromBitmap(bitmap)
                                        _seedColor.value = Color(colorInt)
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to extract color from uninstall icon")
                                        _seedColor.value = null
                                    }
                                }
                            } else {
                                // Reset Colors if icon is null
                                _seedColor.value = null
                            }
                        }
                    }

                    else -> newState = InstallerViewState.Ready
                }

                if (newPackageNameFromProgress != _currentPackageName.value) {
                    if (newPackageNameFromProgress != null) {
                        if (_currentPackageName.value != newPackageNameFromProgress) {
                            _currentPackageName.value = newPackageNameFromProgress
                            loadDisplayIcon(newPackageNameFromProgress)
                        }

                        // --- Update color when the focused package name changes ---
                        // This handles the transition from choice screen to prepare screen.
                        if (useDynColorFollowPkgIcon) {
                            val colorInt = repo.analysisResults.find { it.packageName == newPackageNameFromProgress }?.seedColor
                            _seedColor.value = colorInt?.let { Color(it) }
                        }
                    } else {
                        if (_currentPackageName.value != null) _currentPackageName.value = null
                        if (useDynColorFollowPkgIcon) {
                            val colorInt = repo.analysisResults.firstNotNullOfOrNull { it.seedColor }
                            _seedColor.value = colorInt?.let { Color(it) }
                        }
                    }
                } else if (newPackageNameFromProgress == null && _currentPackageName.value != null) {
                    _currentPackageName.value = null
                    // Set the color back to the first available one for choice screen consistency
                    if (useDynColorFollowPkgIcon) {
                        val colorInt = repo.analysisResults.firstNotNullOfOrNull { it.seedColor }
                        _seedColor.value = colorInt?.let { Color(it) }
                    }
                }

                if (newState !is InstallerViewState.InstallPrepare && autoInstallJob?.isActive == true) {
                    autoInstallJob?.cancel()
                }

                if (newState is InstallerViewState.InstallPrepare && previousState !is InstallerViewState.InstallPrepare) {
                    if (repo.config.installMode == ConfigEntity.InstallMode.AutoDialog) {
                        autoInstallJob?.cancel()
                        autoInstallJob = viewModelScope.launch {
                            delay(500)
                            if (state is InstallerViewState.InstallPrepare && repo.config.installMode == ConfigEntity.InstallMode.AutoDialog) {
                                install()
                            }
                        }
                    }
                }

                if (newState != state) {
                    Timber.d("State transition: ${state::class.simpleName} -> ${newState::class.simpleName}")
                    state = newState
                }
            }
        }
    }

    /**
     * Toggle (enable/disable) an installation flag
     * @param flag The flag to operate on (from InstallOption.value)
     * @param enable true to add the flag, false to remove the flag
     */
    fun toggleInstallFlag(flag: Int, enable: Boolean) {
        val currentFlags = _installFlags.value
        if (enable) {
            // Add flag using bitwise OR
            _installFlags.value = currentFlags or flag
        } else {
            // Remove flag using bitwise AND and bitwise NOT (inv)
            _installFlags.value = currentFlags and flag.inv()
        }
        repo.config.installFlags = _installFlags.value // 同步到 repo.config
    }

    fun toggleBypassBlacklist(enable: Boolean) {
        repo.config.bypassBlacklistInstallSetByUser = enable
    }

    private fun selectInstaller(packageName: String?) {
        repo.config.installer = packageName // Update the repository
        _selectedInstaller.value = packageName // Update the StateFlow
    }

    private fun selectTargetUser(userId: Int) {
        repo.config.targetUserId = userId
        _selectedUserId.value = userId
    }

    /**
     * Loads available users for the selected authorizer.
     * @param authorizer The authorizer to use for the check.
     */
    private fun loadAvailableUsers(authorizer: ConfigEntity.Authorizer) {
        viewModelScope.launch {
            // getAuthorizer() is a suspend function that correctly resolves 'Global' to the actual authorizer.
            val effectiveAuthorizer = authorizer.readGlobal()

            // If the effective authorizer is Dhizuku, disable the feature and do not proceed.
            if (effectiveAuthorizer == ConfigEntity.Authorizer.Dhizuku) {
                _availableUsers.value = emptyMap()
                if (_selectedUserId.value != 0) {
                    selectTargetUser(0)
                }
                return@launch
            }

            // Proceed with fetching users for other authorizers.
            runCatching {
                withContext(Dispatchers.IO) {
                    paRepo.getUsers(effectiveAuthorizer)
                }
            }.onSuccess { users ->
                _availableUsers.value = users
                // Validate if the currently selected user still exists.
                if (!users.containsKey(_selectedUserId.value)) {
                    selectTargetUser(0)
                }
            }.onFailure { error ->
                // Check if the error is caused by coroutine cancellation.
                if (error is kotlinx.coroutines.CancellationException) {
                    Timber.d("User loading job was cancelled as expected.")
                    throw error
                }
                Timber.e(error, "Failed to load available users.")
                toast(error.getErrorMessage(context))
                _availableUsers.value = emptyMap()
                // Also reset selected user on failure.
                if (_selectedUserId.value != 0) {
                    selectTargetUser(0)
                }
            }
        }
    }

    /**
     * Helper function to safely convert a Drawable to a Bitmap.
     * @param drawable The drawable to convert.
     * @return A Bitmap representation of the drawable.
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        // If the drawable is already a BitmapDrawable, just return its bitmap.
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        // For other drawable types, we need to draw it onto a new bitmap.
        // Create a bitmap with the drawable's dimensions.
        // If dimensions are invalid, create a 1x1 pixel bitmap as a fallback.
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            // Use ARGB_8888 for high quality, matches createBitmap overload
            createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        // Create a canvas to draw on the bitmap.
        val canvas = Canvas(bitmap) // Pass bitmap to constructor
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Loads the display icon for the given package name and updates the StateFlow.
     * @param packageName The package name of the app to load the icon for.
     */
    private fun loadDisplayIcon(packageName: String) {
        if (packageName.isBlank() || _displayIcons.value.containsKey(packageName))
        // Do not reload if blank or already loading/loaded.
            return

        // Add a placeholder to prevent re-triggering while loading
        // This safely adds the placeholder without risking a lost update.
        _displayIcons.update { currentMap ->
            if (currentMap.containsKey(packageName)) {
                currentMap // Already contains a placeholder or icon, do nothing.
            } else {
                currentMap + (packageName to null)
            }
        }

        iconJobs[packageName]?.cancel() // Cancel any existing job for this package name
        iconJobs[packageName] = viewModelScope.launch {
            // Find the entity from the repo to pass to the repository method
            val entityToInstall = repo.analysisResults
                .find { it.packageName == packageName }
                ?.appEntities
                ?.map { it.app }
                ?.filterIsInstance<AppEntity.BaseEntity>()
                ?.firstOrNull()

            // Define a generic icon size, could also be passed as a parameter if needed
            val iconSizePx = 256 // A reasonably high resolution

            val loadedIcon = try {
                Timber.d("Prefer system icon: $preferSystemIconForUpdates")
                appIconRepo.getIcon(
                    sessionId = repo.id,
                    packageName = packageName,
                    entityToInstall = entityToInstall,
                    iconSizePx = iconSizePx,
                    preferSystemIcon = preferSystemIconForUpdates
                )
            } catch (e: Exception) {
                // Log the error and return null if icon loading fails
                Timber.d("Failed to load icon for package $packageName: ${e.message}")
                null
            }
            // Update the map with the loaded icon, or the fallback icon if it failed.
            val finalIcon = loadedIcon ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)

            // Create a new map with the updated value
            // This guarantees that setting the final icon won't overwrite other concurrent updates.
            _displayIcons.update { currentMap ->
                if (currentMap[packageName] == null) {
                    currentMap + (packageName to finalIcon)
                } else currentMap
            }
        }
    }

    /**
     * Extracts the Material 3 seed color from a Bitmap asynchronously.
     *
     * This function performs CPU-intensive quantization and scoring,
     * so it must be called from a coroutine and will run on Dispatchers.Default.
     *
     * @param bitmap The source image.
     * @param maxColors The maximum number of colors to quantize.
     * A lower number (e.g., 128) is faster.
     * @param fallbackColorArgb The ARGB Int color to return if scoring fails.
     * @return ARGB formatted seed color (Int).
     */
    private suspend fun extractSeedColorFromBitmap(
        bitmap: Bitmap,
        maxColors: Int = 128, // Lowered for potentially better performance
        fallbackColorArgb: Int = -12417548 // 0xFF3F51B5 - Indigo 500 from Score.java
    ): Int {
        // Run the heavy computation on the default dispatcher
        return withContext(Dispatchers.Default) {

            // Get pixels from Bitmap
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)

            bitmap.getPixels(
                pixels,
                0,      // offset
                width,  // stride
                0,      // x
                0,      // y
                width,
                height
            )

            // Quantize: Get the map of prominent colors to their count
            val colorToCountMap: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)

            // Score: Get the sorted list of best colors
            val sortedColors: List<Int> = Score.score(
                colorToCountMap,
                1, // desired: We only need the top 1 color
                fallbackColorArgb,
                true // filter: Apply default filtering rules
            )

            // Return the best color (first in the list)
            // Score.score ensures the fallback is present if the list would be empty.
            sortedColors.first()
        }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun toast(@StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    private fun close() {
        autoInstallJob?.cancel()
        collectRepoJob?.cancel()
        _currentPackageName.value = null
        iconJobs.values.forEach { it.cancel() }
        iconJobs.clear()
        repo.close()
        state = InstallerViewState.Ready
    }

    private fun analyse() {
        repo.analyse()
    }

    private fun installChoice() {
        autoInstallJob?.cancel()
        if (_currentPackageName.value != null) _currentPackageName.value = null
        val containerType = repo.analysisResults.firstOrNull()
            ?.appEntities?.firstOrNull()
            ?.app?.containerType

        if (containerType == DataType.MIXED_MODULE_APK) {
            val currentResults = repo.analysisResults.toMutableList()
            val updatedResults = currentResults.map { packageResult ->
                val deselectedEntities = packageResult.appEntities.map { it.copy(selected = false) }
                packageResult.copy(appEntities = deselectedEntities)
            }
            repo.analysisResults = updatedResults
        }
        state = InstallerViewState.InstallChoice
    }

    private fun installPrepare() {
        val selectedEntities = repo.analysisResults.flatMap { it.appEntities }.filter { it.selected }
        val uniquePackages = selectedEntities.groupBy { it.app.packageName }

        if (uniquePackages.size == 1) {
            val targetPackageName = selectedEntities.first().app.packageName
            _currentPackageName.value = targetPackageName
            if (useDynColorFollowPkgIcon) {
                val colorInt = repo.analysisResults.find { it.packageName == targetPackageName }?.seedColor
                _seedColor.value = colorInt?.let { Color(it) }
            }
            state = InstallerViewState.InstallPrepare
        } else {
            // Handle case where multiple packages are selected, maybe go back to choice.
            state = InstallerViewState.InstallChoice
        }
    }

    private fun installExtendedMenu() {
        when (state) {
            is InstallerViewState.InstallPrepare,
            InstallerViewState.InstallExtendedSubMenu,
            InstallerViewState.InstallFailed -> {
                state = InstallerViewState.InstallExtendedMenu
            }

            else -> {
                toast("dialog_install_extended_menu_not_available"/*R.string.dialog_install_extended_menu_not_available*/)
            }
        }
    }

    private fun installExtendedSubMenu() {
        if (state is InstallerViewState.InstallExtendedMenu) {
            state = InstallerViewState.InstallExtendedSubMenu
        } else {
            toast("dialog_install_extended_sub_menu_not_available"/*R.string.dialog_install_extended_sub_menu_not_available*/)
        }
    }

    private fun install() {
        autoInstallJob?.cancel()

        if (autoSilentInstall && !isInstallingModule && (state is InstallerViewState.InstallPrepare || state is InstallerViewState.InstallFailed)) {
            Timber.d("Auto-install triggered for APK-only installation. Going to background.")
            repo.install()
            repo.background(true)
        } else {
            Timber.d("Standard foreground installation triggered. Contains Module: $isInstallingModule")
            repo.install()
        }
    }

    private fun background() {
        repo.background(true)
    }

    /**
     * Toggles the selection state of a specific entity within a package.
     * This method handles the complexity of updating the immutable state.
     */
    fun toggleSelection(packageName: String, entityToToggle: SelectInstallEntity, isMultiSelect: Boolean) {
        val currentResults = repo.analysisResults.toMutableList()
        val packageIndex = currentResults.indexOfFirst { it.packageName == packageName }

        if (packageIndex != -1) {
            val packageToUpdate = currentResults[packageIndex]
            val updatedEntities = packageToUpdate.appEntities.map { currentEntity ->
                // Use object reference for precise matching
                if (currentEntity === entityToToggle) {
                    currentEntity.copy(selected = !currentEntity.selected)
                } else if (!isMultiSelect) {
                    // For single-select (radio button) behavior, deselect others
                    currentEntity.copy(selected = false)
                } else {
                    currentEntity
                }
            }.toMutableList()

            // In multi-select mode (radio buttons), if the user clicks an already selected pkg,
            // we should deselect everything in that group.
            if (!isMultiSelect && entityToToggle.selected) {
                updatedEntities.replaceAll { it.copy(selected = false) }
            }

            currentResults[packageIndex] = packageToUpdate.copy(appEntities = updatedEntities)
            repo.analysisResults = currentResults
        }
    }

    private fun uninstallAndRetryInstall(keepData: Boolean) {
        val packageName = _currentPackageName.value
        if (packageName == null) {
            toast("R.string.error_no_package_to_uninstall")
            return
        }
        repo.config.uninstallFlags = if (keepData)
            PackageManagerUtil.DELETE_KEEP_DATA
        else 0 // Default flags (complete removal)

        // Set the flag before starting the operation
        isRetryingInstall = true
        repo.uninstall(packageName)
    }

    /**
     * 处理批量安装过程中的进度更新
     *
     * @param progress 当前进度
     */
    private fun handleMultiInstallProgress(progress: ProgressEntity) {
        when (progress) {
            is ProgressEntity.InstallSuccess -> {
                val currentEntity = multiInstallQueue[currentMultiInstallIndex]
                // Clear the icon cache for the installed package
                // appIconRepo.clearCacheForPackage(currentEntity.app.packageName)
                multiInstallResults.add(InstallResult(entity = currentEntity, success = true))
                currentMultiInstallIndex++
                triggerNextMultiInstall()
            }

            is ProgressEntity.InstallFailed -> {
                val currentEntity = multiInstallQueue[currentMultiInstallIndex]
                // 捕获 repo 中当前的错误信息
                multiInstallResults.add(InstallResult(entity = currentEntity, success = false, error = repo.error))
                currentMultiInstallIndex++
                triggerNextMultiInstall()
            }
            // 在批量安装过程中，我们只关心最终的成功或失败状态，其他状态可以忽略
            else -> {}
        }
    }

    /**
     * Starts the multi-package installation process.
     */
    private fun installMultiple() {
        multiInstallQueue = repo.analysisResults.flatMap { it.appEntities }.filter { it.selected }
        multiInstallResults.clear()
        currentMultiInstallIndex = 0
        _installProgress.value = 0f // Initialize progress to 0 at the start of multi-install.
        triggerNextMultiInstall()
    }

    /**
     * Triggers the next installation task in the queue.
     * This method temporarily modifies the repo's state to isolate the single app
     * being installed, then triggers the install action.
     */
    private fun triggerNextMultiInstall() {
        if (currentMultiInstallIndex < multiInstallQueue.size) {
            val entityToInstall = multiInstallQueue[currentMultiInstallIndex]

            // Find the original PackageAnalysisResult that the current entity belongs to.
            // We search in 'originalAnalysisResults' which holds the complete, unmodified analysis.
            val originalPackageResult =
                originalAnalysisResults.find { it.packageName == entityToInstall.app.packageName }

            if (originalPackageResult == null) {
                // This is a safeguard. If the original package can't be found, skip to the next.
                Timber.e("Could not find original package for ${entityToInstall.app.packageName}. Skipping.")
                multiInstallResults.add(
                    InstallResult(
                        entity = entityToInstall,
                        success = false,
                        error = IllegalStateException("Original package info not found.")
                    )
                )
                currentMultiInstallIndex++
                triggerNextMultiInstall()
                return
            }

            // Create a new, temporary PackageAnalysisResult containing ONLY the current entity to be installed.
            // We ensure it's marked as 'selected = true'.
            val tempPackageResult = originalPackageResult.copy(
                appEntities = listOf(entityToInstall.copy(selected = true))
            )

            // Set the repository's state to this temporary, single-pkg state.
            // The 'ActionHandler.install()' method will read this state.
            repo.analysisResults = listOf(tempPackageResult)

            val appLabel = (entityToInstall.app as? AppEntity.BaseEntity)?.label ?: entityToInstall.app.packageName

            // Update progress text (this logic remains the same)
            _installProgressText.value = UiText(
                id = R.string.installing_progress_text,
                formatArgs = listOf(appLabel, currentMultiInstallIndex + 1, multiInstallQueue.size)
            )
            _currentPackageName.value = entityToInstall.app.packageName // Update current package name for the UI

            // Update numeric progress (this logic remains the same)
            _installProgress.value = currentMultiInstallIndex.toFloat() / multiInstallQueue.size.toFloat()

            // Switch to the 'Installing' state
            state = InstallerViewState.Installing

            // Call the repo's install method. It will now operate on the temporary state we just set.
            repo.install()
        } else {
            // All installation tasks are complete.
            state = InstallerViewState.InstallCompleted(multiInstallResults.toList())

            // Clean up and restore the original state.
            multiInstallQueue = emptyList()
            _installProgressText.value = null
            _currentPackageName.value = null

            // Restore the repo's original, full analysis results.
            repo.analysisResults = originalAnalysisResults
            originalAnalysisResults = emptyList()
        }
    }
}