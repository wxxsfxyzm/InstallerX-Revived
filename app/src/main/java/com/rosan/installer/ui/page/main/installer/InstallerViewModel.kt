package com.rosan.installer.ui.page.main.installer

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
import com.rosan.installer.data.app.model.enums.DataType
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.app.util.PackageManagerUtil
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.recycle.model.impl.PrivilegedManager
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.util.ConfigUtil.readGlobal
import com.rosan.installer.util.getErrorMessage
import kotlinx.coroutines.CancellationException
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
    private val appIconRepo: AppIconRepo
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

    var viewSettings by mutableStateOf(InstallerViewSettings())
        private set

    var showMiuixSheetRightActionSettings by mutableStateOf(false)
        private set
    var showMiuixPermissionList by mutableStateOf(false)
        private set
    var navigatedFromPrepareToChoice by mutableStateOf(false)
        private set

    // Progress to drive the progress bar
    private val _installProgress = MutableStateFlow<Float?>(null)
    val installProgress: StateFlow<Float?> = _installProgress.asStateFlow()

    /**
     * Determines if the dialog can be dismissed by tapping the scrim.
     * Dismissal is disallowed during ongoing operations like installing.
     */
    val isDismissible
        get() = when (state) {
            is InstallerViewState.Analysing,
            is InstallerViewState.Resolving,
            is InstallerViewState.InstallExtendedMenu,
            is InstallerViewState.InstallChoice,
            is InstallerViewState.Uninstalling -> false

            is InstallerViewState.InstallingModule -> (state as InstallerViewState.InstallingModule).isFinished
            is InstallerViewState.InstallPrepare -> !(showMiuixSheetRightActionSettings || showMiuixPermissionList)
            is InstallerViewState.Preparing,
            is InstallerViewState.Installing -> !viewSettings.disableNotificationOnDismiss

            else -> true
        }

    private val _currentPackageName = MutableStateFlow<String?>(null)
    val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    private val _displayIcons = MutableStateFlow<Map<String, Drawable?>>(emptyMap())
    val displayIcons: StateFlow<Map<String, Drawable?>> = _displayIcons.asStateFlow()

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
    private val settingsLoadingJob: Job
    private var collectRepoJob: Job? = null

    init {
        Timber.d("InstallerViewModel init")
        settingsLoadingJob = loadInitialSettings()
        viewModelScope.launch {
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
            is InstallerViewAction.Cancel -> cancel()
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
            is InstallerViewAction.Reboot -> repo.reboot(action.reason)
            is InstallerViewAction.UninstallAndRetryInstall -> uninstallAndRetryInstall(
                action.keepData,
                action.conflictingPackage
            )

            is InstallerViewAction.Uninstall -> {
                // Trigger uninstall using the package name from the collected info
                repo.uninstallInfo.value?.packageName?.let { repo.uninstall(it) }
            }

            is InstallerViewAction.ShowMiuixSheetRightActionSettings -> showMiuixSheetRightActionSettings = true
            is InstallerViewAction.HideMiuixSheetRightActionSettings -> showMiuixSheetRightActionSettings = false
            is InstallerViewAction.ShowMiuixPermissionList -> showMiuixPermissionList = true
            is InstallerViewAction.HideMiuixPermissionList -> showMiuixPermissionList = false

            is InstallerViewAction.ToggleSelection -> toggleSelection(action.packageName, action.entity, action.isMultiSelect)
            is InstallerViewAction.ToggleUninstallFlag -> toggleUninstallFlag(action.flag, action.enable)
            is InstallerViewAction.SetInstaller -> selectInstaller(action.installer)
            is InstallerViewAction.SetTargetUser -> selectTargetUser(action.userId)
            is InstallerViewAction.ApproveSession -> repo.approveConfirmation(action.sessionId, action.granted)
        }
    }

    private fun loadInitialSettings() =
        viewModelScope.launch {
            viewSettings = viewSettings.copy(
                uiExpressive =
                    appDataStore.getBoolean(AppDataStore.UI_EXPRESSIVE_SWITCH, true).first(),
                preferSystemIconForUpdates =
                    appDataStore.getBoolean(AppDataStore.PREFER_SYSTEM_ICON_FOR_INSTALL, false).first(),
                autoCloseCountDown =
                    appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3).first(),
                showExtendedMenu =
                    appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, false).first(),
                showSmartSuggestion =
                    appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, true).first(),
                disableNotificationOnDismiss =
                    appDataStore.getBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, false).first(),
                versionCompareInSingleLine =
                    appDataStore.getBoolean(AppDataStore.DIALOG_VERSION_COMPARE_SINGLE_LINE, false).first(),
                sdkCompareInMultiLine = appDataStore.getBoolean(AppDataStore.DIALOG_SDK_COMPARE_MULTI_LINE, false).first(),
                showOPPOSpecial = appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_OPPO_SPECIAL, false).first(),
                autoSilentInstall = appDataStore.getBoolean(AppDataStore.DIALOG_AUTO_SILENT_INSTALL, false).first(),
                enableModuleInstall = appDataStore.getBoolean(AppDataStore.LAB_ENABLE_MODULE_FLASH, false).first(),
                useDynColorFollowPkgIcon = appDataStore.getBoolean(AppDataStore.UI_DYN_COLOR_FOLLOW_PKG_ICON, false).first()
            )
        }

    /**
     * Maps a ProgressEntity from the repository to the corresponding InstallerViewState.
     * This function centralizes the logic for state transitions based on progress updates.
     *
     * @param progress The latest ProgressEntity from the repository.
     * @return The calculated InstallerViewState.
     */
    private fun mapProgressToViewState(progress: ProgressEntity): InstallerViewState {
        return when (progress) {
            is ProgressEntity.Ready -> InstallerViewState.Ready

            is ProgressEntity.UninstallResolveFailed,
            is ProgressEntity.InstallResolvedFailed -> InstallerViewState.ResolveFailed

            is ProgressEntity.InstallAnalysedFailed -> InstallerViewState.AnalyseFailed

            is ProgressEntity.InstallAnalysedSuccess -> {
                // Backup original results on first successful analysis.
                if (originalAnalysisResults.isEmpty()) {
                    originalAnalysisResults = repo.analysisResults
                }
                repo.analysisResults.forEach { result ->
                    loadDisplayIcon(result.packageName)
                }
                val analysisResults = repo.analysisResults
                val containerType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType

                val isMultiAppMode = analysisResults.size > 1 ||
                        containerType == DataType.MULTI_APK ||
                        containerType == DataType.MULTI_APK_ZIP ||
                        containerType == DataType.MIXED_MODULE_APK ||
                        containerType == DataType.MIXED_MODULE_ZIP

                if (isMultiAppMode) InstallerViewState.InstallChoice else InstallerViewState.InstallPrepare
            }

            is ProgressEntity.Installing -> {
                // Calculate float progress for ProgressBar
                val floatProgress = if (progress.total > 1) {
                    progress.current.toFloat() / progress.total.toFloat()
                } else 0f

                InstallerViewState.Installing(
                    progress = floatProgress,
                    current = progress.current,
                    total = progress.total,
                    appLabel = progress.appLabel
                )
            }

            // Explicit mapping for batch completion
            is ProgressEntity.InstallCompleted -> {
                InstallerViewState.InstallCompleted(progress.results)
            }

            is ProgressEntity.InstallFailed -> {
                //  Check if it's a module install using the persistent Repo data
                if (isInstallingModule) {
                    // Load the full log history from the Repo, ensuring data persists across ViewModel recreations
                    val currentOutput = repo.moduleLog.toMutableList()

                    // Append the error message to the terminal output for visibility
                    repo.error.message?.let { msg ->
                        val errorLine = "ERROR: $msg"
                        // Prevent duplicate error lines
                        if (currentOutput.lastOrNull() != errorLine) {
                            currentOutput.add(errorLine)
                        }
                    }

                    // Return the terminal view state with isFinished = true
                    InstallerViewState.InstallingModule(
                        output = currentOutput,
                        isFinished = true
                    )
                } else {
                    InstallerViewState.InstallFailed
                }
            }

            is ProgressEntity.InstallSuccess -> {
                // Check if it's a module install
                if (isInstallingModule) {
                    // Load the full log history from the Repo and keep the terminal view open
                    InstallerViewState.InstallingModule(
                        output = repo.moduleLog,
                        isFinished = true
                    )
                } else {
                    InstallerViewState.InstallSuccess
                }
            }

            is ProgressEntity.InstallingModule -> InstallerViewState.InstallingModule(progress.output)

            is ProgressEntity.InstallConfirming -> {
                val details = repo.confirmationDetails.value
                if (details != null) {
                    InstallerViewState.InstallConfirm(
                        appLabel = details.appLabel,
                        appIcon = details.appIcon,
                        sessionId = details.sessionId
                    )
                } else {
                    InstallerViewState.ResolveFailed // Fallback if details are missing
                }
            }

            is ProgressEntity.Uninstalling -> {
                if (isRetryingInstall) InstallerViewState.InstallRetryDowngradeUsingUninstall else InstallerViewState.Uninstalling
            }

            is ProgressEntity.UninstallFailed -> {
                if (isRetryingInstall) {
                    isRetryingInstall = false
                    InstallerViewState.InstallFailed
                } else {
                    InstallerViewState.UninstallFailed
                }
            }

            is ProgressEntity.UninstallSuccess -> {
                if (isRetryingInstall) {
                    isRetryingInstall = false
                    repo.install(false) // Trigger reinstall
                    InstallerViewState.InstallRetryDowngradeUsingUninstall
                } else {
                    InstallerViewState.UninstallSuccess
                }
            }

            is ProgressEntity.UninstallReady -> {
                // This state has side effects (updating UI-specific state), so they are handled here.
                _uiUninstallInfo.value = repo.uninstallInfo.value
                _uninstallFlags.value = repo.config.uninstallFlags
                InstallerViewState.UninstallReady
            }

            // For states that are handled specially (like loading), or have no UI change, return the current state.
            is ProgressEntity.InstallResolving, is ProgressEntity.InstallAnalysing, is ProgressEntity.InstallPreparing -> state

            // Fallback for any other unhandled progress types.
            else -> InstallerViewState.Ready
        }
    }

    /**
     * Handles all side effects related to a progress update, such as managing jobs,
     * updating focused package name, and handling dynamic colors.
     *
     * @param newPackageName The package name derived from the new state, if any.
     * @param newState The newly calculated InstallerViewState.
     */
    private fun handleStateSideEffects(newPackageName: String?, newState: InstallerViewState) {
        // --- Update current package name ---
        if (newPackageName != _currentPackageName.value) {
            _currentPackageName.value = newPackageName
            if (newPackageName != null) {
                loadDisplayIcon(newPackageName)
            }
        }

        // --- UNIFIED DYNAMIC COLOR LOGIC ---
        if (viewSettings.useDynColorFollowPkgIcon) {
            val colorInt: Int? = when (newState) {
                // For install states, get color from analysis results
                is InstallerViewState.InstallPrepare,
                is InstallerViewState.Installing,
                is InstallerViewState.InstallFailed,
                is InstallerViewState.InstallSuccess -> repo.analysisResults.find { it.packageName == newPackageName }?.seedColor

                // For choice screen, get the first available color
                is InstallerViewState.InstallChoice -> repo.analysisResults.firstNotNullOfOrNull { it.seedColor }

                // For uninstall state, get pre-calculated color from uninstall info
                is InstallerViewState.UninstallReady,
                is InstallerViewState.Uninstalling,
                is InstallerViewState.UninstallSuccess,
                is InstallerViewState.UninstallFailed -> repo.uninstallInfo.value?.seedColor

                // For all other states, we can clear the color
                else -> null
            }
            if (colorInt != null) {
                _seedColor.value = Color(colorInt)
            } else if (newState is InstallerViewState.Ready) {
                _seedColor.value = null
            }

        } else if (_seedColor.value != null) {
            // If the feature is disabled, ensure the color is cleared.
            _seedColor.value = null
        }

        // --- Manage auto-install job ---
        autoInstallJob?.cancel() // Cancel any previous auto-install job by default.
        if (newState is InstallerViewState.InstallPrepare && repo.config.installMode == ConfigEntity.InstallMode.AutoDialog) {
            autoInstallJob = viewModelScope.launch {
                delay(500)
                if (state is InstallerViewState.InstallPrepare) {
                    install()
                }
            }
        }
    }

    private fun collectRepo(repo: InstallerRepo) {
        this.repo = repo
        if (repo.config.enableCustomizeUser) {
            loadAvailableUsers(repo.config.authorizer)
        }

        // Initialize install flags from repo config
        _installFlags.value = listOfNotNull(
            repo.config.allowTestOnly.takeIf { it }?.let { InstallOption.AllowTest.value },
            repo.config.allowDowngrade.takeIf { it }?.let { InstallOption.AllowDowngrade.value },
            repo.config.forAllUser.takeIf { it }?.let { InstallOption.AllUsers.value },
            repo.config.bypassLowTargetSdk.takeIf { it }?.let { InstallOption.BypassLowTargetSdkBlock.value },
            repo.config.allowAllRequestedPermissions.takeIf { it }?.let { InstallOption.GrantAllRequestedPermissions.value }
        ).fold(0) { acc, flag -> acc or flag }
        repo.config.installFlags = _installFlags.value

        _currentPackageName.value = null
        val newPackageNames = repo.analysisResults.map { it.packageName }.toSet()
        _displayIcons.update { old -> old.filterKeys { it in newPackageNames } }

        collectRepoJob?.cancel()
        autoInstallJob?.cancel()

        collectRepoJob = viewModelScope.launch {
            settingsLoadingJob.join()
            repo.progress.collect { progress ->
                // Handle transient "loading" states separately, as they don't always cause a full state change.
                if (progress is ProgressEntity.InstallResolving || progress is ProgressEntity.InstallPreparing || progress is ProgressEntity.InstallAnalysing) {
                    // If installing a module, skip the anti-flicker delay to show the sheet immediately.
                    if (isInstallingModule) {
                        loadingStateJob?.cancel()
                        state = if (progress is ProgressEntity.InstallPreparing) {
                            InstallerViewState.Preparing(progress.progress)
                        } else {
                            InstallerViewState.Analysing
                        }
                    }
                    // For regular app installs, keep the 200ms delay to prevent UI flickering.
                    else if (loadingStateJob == null || !loadingStateJob!!.isActive) {
                        loadingStateJob = viewModelScope.launch {
                            delay(200L)
                            state = if (progress is ProgressEntity.InstallPreparing) {
                                InstallerViewState.Preparing(progress.progress)
                            } else {
                                InstallerViewState.Analysing
                            }
                        }
                    }
                    return@collect // Don't proceed to full state-machine for these transient states.
                }

                loadingStateJob?.cancel()
                loadingStateJob = null

                // --- Stage 2: Map the progress to a new view state ---
                val newState = mapProgressToViewState(progress)

                // --- Stage 3: Determine context (like the current package name) from the new state ---
                val newPackageName = when (newState) {
                    // For the Installing state, we need to differentiate between single and batch install
                    is InstallerViewState.Installing -> {
                        if (newState.total > 1) {
                            val selectedEntities = repo.analysisResults.flatMap { it.appEntities }.filter { it.selected }
                            val groupedApps = selectedEntities.groupBy { it.app.packageName }.values.toList()

                            val index = newState.current - 1
                            val currentAppGroup = groupedApps.getOrNull(index)

                            currentAppGroup?.firstOrNull()?.app?.packageName
                                ?: _currentPackageName.value // Fallback
                        } else {
                            // For single install, use the selected entity.
                            _currentPackageName.value
                                ?: repo.analysisResults.firstNotNullOfOrNull { result ->
                                    if (result.appEntities.any { it.selected }) result.packageName else null
                                }
                                ?: repo.analysisResults.firstOrNull()?.packageName
                        }
                    }

                    // For these states, we need to ensure a specific package name is focused.
                    is InstallerViewState.InstallPrepare,
                    is InstallerViewState.InstallFailed,
                    is InstallerViewState.InstallSuccess -> {
                        // Prioritize the existing currentPackageName (correctly set by installPrepare).
                        // This ensures that in Mixed Module/APK scenarios, the entity selected by the user
                        // (e.g., the APK) remains focused instead of reverting to the first item (e.g., the Module).
                        // Fallback to the first result only if currentPackageName is null.
                        _currentPackageName.value ?: repo.analysisResults.firstOrNull()?.packageName
                    }
                    // For choice/multi-app states, there is no single focused package.
                    is InstallerViewState.InstallChoice, is InstallerViewState.Ready -> null
                    // For other states, keep the current package name unless explicitly cleared.
                    else -> _currentPackageName.value
                }

                // --- Stage 4: Handle all side effects based on the new state and progress ---
                handleStateSideEffects(newPackageName, newState)

                // --- Stage 5: Apply the final state change if necessary ---
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
                    PrivilegedManager.getUsers(effectiveAuthorizer)
                }
            }.onSuccess { users ->
                _availableUsers.value = users
                // Validate if the currently selected user still exists.
                if (!users.containsKey(_selectedUserId.value)) {
                    selectTargetUser(0)
                }
            }.onFailure { error ->
                // Check if the error is caused by coroutine cancellation.
                if (error is CancellationException) {
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
     * Loads the display icon for the given package name and updates the StateFlow.
     * @param packageName The package name of the app to load the icon for.
     */
    private fun loadDisplayIcon(packageName: String) {
        if (packageName.isBlank()) return

        val mapSnapshot = _displayIcons.value
        val currentValue = mapSnapshot[packageName]
        val existingJob = iconJobs[packageName]
        val isJobActive = existingJob?.isActive == true

        if (currentValue != null || isJobActive) {
            return
        }

        _displayIcons.update { currentMap ->
            if (currentMap.containsKey(packageName)) {
                currentMap
            } else {
                currentMap + (packageName to null)
            }
        }

        existingJob?.cancel()
        iconJobs[packageName] = viewModelScope.launch {
            val rawEntities = repo.analysisResults
                .find { it.packageName == packageName }
                ?.appEntities
                ?.map { it.app }

            val entityToInstall =
                rawEntities?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
                    ?: rawEntities?.filterIsInstance<AppEntity.ModuleEntity>()?.firstOrNull()

            val loadedIcon = try {
                appIconRepo.getIcon(
                    sessionId = repo.id,
                    packageName = packageName,
                    entityToInstall = entityToInstall,
                    iconSizePx = 256,
                    preferSystemIcon = viewSettings.preferSystemIconForUpdates
                )
            } catch (_: Exception) {
                null
            }

            val finalIcon =
                loadedIcon ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)

            _displayIcons.update { currentMap ->
                if (currentMap[packageName] == null) {
                    currentMap + (packageName to finalIcon)
                } else {
                    currentMap
                }
            }
        }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun toast(@StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
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

    private fun cancel() {
        // 2. 取消自动安装任务（如果存在）
        autoInstallJob?.cancel()

        // 3. 取消图标加载任务（可选，优化性能）
        iconJobs.values.forEach { it.cancel() }

        // 4. 调用 Repo 的取消。
        // 根据之前的后端修改，ActionHandler 会捕获 Cancel 动作，取消协程，并发送 ProgressEntity.Ready。
        // ViewModel 监听到 Ready 后，会自动将 state 切换回 InstallerViewState.Ready。
        repo.cancel()
    }

    private fun analyse() {
        repo.analyse()
    }

    private fun installChoice() {
        autoInstallJob?.cancel()
        if (_currentPackageName.value != null) _currentPackageName.value = null
        val containerType = repo.analysisResults.firstOrNull()
            ?.appEntities?.firstOrNull()
            ?.app?.sourceType

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
            if (viewSettings.useDynColorFollowPkgIcon) {
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
        Timber.d("Standard foreground installation triggered. Contains Module: $isInstallingModule")
        repo.install(true)
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

    private fun toggleUninstallFlag(flag: Int, enable: Boolean) {
        val currentFlags = _uninstallFlags.value
        var newFlags = currentFlags

        if (enable) {
            newFlags = newFlags or flag

            if (flag == PackageManagerUtil.DELETE_ALL_USERS) {
                if ((currentFlags and PackageManagerUtil.DELETE_SYSTEM_APP) != 0) {
                    newFlags = newFlags and PackageManagerUtil.DELETE_SYSTEM_APP.inv()
                    toast(R.string.uninstall_system_app_disabled)
                }
            } else if (flag == PackageManagerUtil.DELETE_SYSTEM_APP) {
                if ((currentFlags and PackageManagerUtil.DELETE_ALL_USERS) != 0) {
                    newFlags = newFlags and PackageManagerUtil.DELETE_ALL_USERS.inv()
                    toast(R.string.uninstall_all_users_disabled)
                }
            }
        } else newFlags = newFlags and flag.inv()

        if (newFlags != currentFlags) {
            _uninstallFlags.value = newFlags
            repo.config.uninstallFlags = newFlags
        }
    }

    private fun uninstallAndRetryInstall(keepData: Boolean, conflictingPackage: String?) {
        val targetPackageName = conflictingPackage ?: _currentPackageName.value
        if (targetPackageName == null) {
            toast("R.string.error_no_package_to_uninstall")
            return
        }
        repo.config.uninstallFlags = if (keepData)
            PackageManagerUtil.DELETE_KEEP_DATA
        else 0 // Default flags (complete removal)

        // Set the flag before starting the operation
        isRetryingInstall = true
        Timber.d("Uninstalling conflicting/old package: $targetPackageName for retry")
        repo.uninstall(targetPackageName)
    }

    /**
     * Starts the multi-package installation process.
     */
    private fun installMultiple() {
        val selectedEntities = repo.analysisResults.flatMap { it.appEntities }.filter { it.selected }
        repo.installMultiple(selectedEntities)
    }
}