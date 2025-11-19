package com.rosan.installer.ui.page.main.installer.dialog

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

    var viewSettings by mutableStateOf(InstallerViewSettings())
        private set

    var showMiuixSheetRightActionSettings by mutableStateOf(false)
        private set
    var showMiuixPermissionList by mutableStateOf(false)
        private set
    var navigatedFromPrepareToChoice by mutableStateOf(false)
        private set

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
    private var collectRepoJob: Job? = null

    init {
        Timber.d("DialogViewModel init")
        loadInitialSettings()
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

    private fun loadInitialSettings() =
        viewModelScope.launch {
            viewSettings = viewSettings.copy(
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
                val analysisResults = repo.analysisResults
                val containerType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.containerType

                val isMultiAppMode = analysisResults.size > 1 ||
                        containerType == DataType.MULTI_APK ||
                        containerType == DataType.MULTI_APK_ZIP ||
                        containerType == DataType.MIXED_MODULE_APK ||
                        containerType == DataType.MIXED_MODULE_ZIP

                if (isMultiAppMode) InstallerViewState.InstallChoice else InstallerViewState.InstallPrepare
            }

            is ProgressEntity.Installing -> InstallerViewState.Installing

            is ProgressEntity.InstallFailed -> {
                if (state is InstallerViewState.InstallingModule && repo.error is ModuleInstallExitCodeNonZeroException) {
                    // If a module install fails with a specific error, update the existing state instead of replacing it.
                    val currentOutput = (state as InstallerViewState.InstallingModule).output.toMutableList()
                    repo.error.message?.let { currentOutput.add("ERROR: $it") }
                    (state as InstallerViewState.InstallingModule).copy(
                        output = currentOutput,
                        isFinished = true
                    )
                } else {
                    InstallerViewState.InstallFailed
                }
            }

            is ProgressEntity.InstallSuccess -> {
                // If a module install succeeds, just mark it as finished.
                if (state is InstallerViewState.InstallingModule) {
                    (state as InstallerViewState.InstallingModule).copy(isFinished = true)
                } else {
                    InstallerViewState.InstallSuccess
                }
            }

            is ProgressEntity.InstallingModule -> InstallerViewState.InstallingModule(progress.output)

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
                    repo.install() // Trigger reinstall
                    InstallerViewState.InstallRetryDowngradeUsingUninstall
                } else {
                    InstallerViewState.UninstallSuccess
                }
            }

            is ProgressEntity.UninstallReady -> {
                // This state has side effects (updating UI-specific state), so they are handled here.
                _uiUninstallInfo.value = repo.uninstallInfo.value
                _uninstallFlags.value = 0
                repo.config.uninstallFlags = 0
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
     * @param progress The original ProgressEntity that triggered the update.
     */
    private fun handleStateSideEffects(newPackageName: String?, newState: InstallerViewState, progress: ProgressEntity) {
        // --- 1. Manage loading indicator job ---
        if (progress is ProgressEntity.InstallAnalysedSuccess || progress is ProgressEntity.InstallAnalysedFailed) {
            loadingStateJob?.cancel()
            loadingStateJob = null
        }

        // --- 2. Update current package name ---
        if (newPackageName != _currentPackageName.value) {
            _currentPackageName.value = newPackageName
            if (newPackageName != null) {
                loadDisplayIcon(newPackageName)
            }
        }

        // --- 3. UNIFIED DYNAMIC COLOR LOGIC ---
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
                is InstallerViewState.UninstallReady -> repo.uninstallInfo.value?.seedColor

                // For all other states, we can clear the color
                else -> null
            }
            _seedColor.value =
                colorInt?.let { Color(it) } ?: _seedColor.value.takeIf { newState is InstallerViewState.Ready }?.let { null }

        } else if (_seedColor.value != null) {
            // If the feature is disabled, ensure the color is cleared.
            _seedColor.value = null
        }

        // --- 4. Manage auto-install job ---
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

    // Replace your existing collectRepo with this refactored version
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
            repo.config.allowRestrictedPermissions.takeIf { it }?.let { InstallOption.AllWhitelistRestrictedPermissions.value },
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
            repo.progress.collect { progress ->
                // --- Stage 1: Handle high-priority, blocking states first ---
                if (multiInstallQueue.isNotEmpty()) {
                    handleMultiInstallProgress(progress)
                    return@collect // Multi-install has its own state machine
                }

                // Handle transient "loading" states separately, as they don't always cause a full state change.
                if (progress is ProgressEntity.InstallResolving || progress is ProgressEntity.InstallPreparing || progress is ProgressEntity.InstallAnalysing) {
                    if (loadingStateJob == null || !loadingStateJob!!.isActive) {
                        loadingStateJob = viewModelScope.launch {
                            delay(200L) // Show loading indicator only if the operation is not instant
                            state = if (progress is ProgressEntity.InstallPreparing) {
                                InstallerViewState.Preparing(progress.progress)
                            } else {
                                InstallerViewState.Analysing
                            }
                        }
                    }
                    return@collect // Don't proceed to full state-machine for these transient states.
                }

                // --- Stage 2: Map the progress to a new view state ---
                val newState = mapProgressToViewState(progress)

                // --- Stage 3: Determine context (like the current package name) from the new state ---
                val newPackageName = when (newState) {
                    is InstallerViewState.InstallPrepare,
                    is InstallerViewState.Installing,
                    is InstallerViewState.InstallFailed,
                    is InstallerViewState.InstallSuccess -> repo.analysisResults.firstOrNull()?.packageName
                    // For choice/multi-app states, there is no single focused package.
                    is InstallerViewState.InstallChoice, is InstallerViewState.Ready -> null
                    // For other states, keep the current package name unless explicitly cleared.
                    else -> _currentPackageName.value
                }

                // --- Stage 4: Handle all side effects based on the new state and progress ---
                handleStateSideEffects(newPackageName, newState, progress)

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
                Timber.d("Prefer system icon: $viewSettings.preferSystemIconForUpdates")
                appIconRepo.getIcon(
                    sessionId = repo.id,
                    packageName = packageName,
                    entityToInstall = entityToInstall,
                    iconSizePx = iconSizePx,
                    preferSystemIcon = viewSettings.preferSystemIconForUpdates
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

        if (viewSettings.autoSilentInstall && !isInstallingModule && (state is InstallerViewState.InstallPrepare || state is InstallerViewState.InstallFailed)) {
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