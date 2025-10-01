package com.rosan.installer.ui.page.main.installer.dialog

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.model.entity.DataType
import com.rosan.installer.data.app.model.entity.PackageAnalysisResult
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

class DialogViewModel(
    private var repo: InstallerRepo,
    private val appDataStore: AppDataStore,
    private val appIconRepo: AppIconRepo,
    private val paRepo: PARepo
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf<DialogViewState>(DialogViewState.Ready)
        private set

    // Hold the original, complete analysis results for multi-install scenarios.
    private var originalAnalysisResults: List<PackageAnalysisResult> = emptyList()

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
            is DialogViewState.Analysing,
            is DialogViewState.Resolving -> false

            is DialogViewState.Installing -> !disableNotificationOnDismiss
            else -> true
        }

    private val _currentPackageName = MutableStateFlow<String?>(null)
    val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    private val _displayIcons = MutableStateFlow<Map<String, Drawable?>>(emptyMap())
    val displayIcons: StateFlow<Map<String, Drawable?>> = _displayIcons.asStateFlow()

    var preferSystemIconForUpdates by mutableStateOf(false)
        private set

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

    private val iconJobs = mutableMapOf<String, Job>()
    private var autoInstallJob: Job? = null
    private var collectRepoJob: Job? = null

    init {
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
            // Load managed packages for installer selection.
            appDataStore.getNamedPackageList(AppDataStore.MANAGED_INSTALLER_PACKAGES_LIST).collect { packages ->
                _managedInstallerPackages.value = packages
            }
        }
    }

    fun dispatch(action: DialogViewAction) {
        when (action) {
            is DialogViewAction.CollectRepo -> collectRepo(action.repo)
            is DialogViewAction.Close -> close()
            is DialogViewAction.Analyse -> analyse()
            is DialogViewAction.InstallChoice -> installChoice()
            is DialogViewAction.InstallPrepare -> installPrepare()
            is DialogViewAction.InstallExtendedMenu -> installExtendedMenu()
            is DialogViewAction.InstallExtendedSubMenu -> installExtendedSubMenu()
            is DialogViewAction.InstallMultiple -> installMultiple()
            is DialogViewAction.Install -> install()
            is DialogViewAction.Background -> background()
            is DialogViewAction.UninstallAndRetryInstall -> uninstallAndRetryInstall(action.keepData)
            is DialogViewAction.Uninstall -> {
                // Trigger uninstall using the package name from the collected info
                repo.uninstallInfo.value?.packageName?.let { repo.uninstall(it) }
            }

            is DialogViewAction.ToggleSelection -> toggleSelection(
                action.packageName,
                action.entity,
                action.isMultiSelect
            )

            is DialogViewAction.ToggleUninstallFlag -> {
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

            is DialogViewAction.SetInstaller -> selectInstaller(action.installer)
            is DialogViewAction.SetTargetUser -> selectTargetUser(action.userId)
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

                val previousState = state
                var newState: DialogViewState
                var newPackageNameFromProgress: String? = _currentPackageName.value

                when (progress) {
                    is ProgressEntity.Ready -> {
                        newState = DialogViewState.Ready
                        newPackageNameFromProgress = null
                    }

                    is ProgressEntity.InstallPreparing -> newState = DialogViewState.Preparing(progress.progress)
                    is ProgressEntity.InstallResolving -> newState = DialogViewState.Resolving
                    is ProgressEntity.InstallResolvedFailed -> newState = DialogViewState.ResolveFailed
                    is ProgressEntity.InstallAnalysing -> newState = DialogViewState.Analysing
                    is ProgressEntity.InstallAnalysedFailed -> newState = DialogViewState.AnalyseFailed
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
                                containerType == DataType.MULTI_APK_ZIP

                        if (isMultiAppMode) {
                            // If the backend (ActionHandler) determined it's a multi-app scenario,
                            // ALWAYS go to the choice screen, regardless of package names.
                            Timber.d("ViewModel: Multi-app mode detected. Forcing InstallChoice state.")
                            newState = DialogViewState.InstallChoice
                            newPackageNameFromProgress = null // No single package is the focus.

                            // Trigger icon loading for all apps in the list.
                            analysisResults.forEach { result ->
                                loadDisplayIcon(result.packageName)
                            }
                        } else {
                            // If it's not a multi-app scenario (e.g., single APK with splits),
                            // it's safe to proceed directly to the prepare screen for the single app.
                            Timber.d("ViewModel: Single-app mode detected. Proceeding to InstallPrepare.")
                            newState = DialogViewState.InstallPrepare
                            newPackageNameFromProgress = analysisResults.firstOrNull()?.packageName
                        }
                    }

                    is ProgressEntity.Installing -> {
                        newState = DialogViewState.Installing
                        autoInstallJob?.cancel()
                        if (newPackageNameFromProgress == null && repo.analysisResults.size == 1) {
                            newPackageNameFromProgress = repo.analysisResults.first().packageName
                        }
                    }

                    is ProgressEntity.InstallFailed -> {
                        newState = DialogViewState.InstallFailed
                        autoInstallJob?.cancel()
                        if (newPackageNameFromProgress == null && repo.analysisResults.size == 1) {
                            newPackageNameFromProgress = repo.analysisResults.first().packageName
                        }
                    }

                    is ProgressEntity.InstallSuccess -> {
                        newState = DialogViewState.InstallSuccess
                        autoInstallJob?.cancel()
                        if (newPackageNameFromProgress == null && repo.analysisResults.size == 1) {
                            newPackageNameFromProgress = repo.analysisResults.first().packageName
                        }
                    }

                    is ProgressEntity.Uninstalling -> {
                        newState = if (isRetryingInstall) {
                            //isRetryingInstall = false
                            DialogViewState.InstallRetryDowngradeUsingUninstall
                        } else {
                            DialogViewState.Uninstalling
                        }
                    }

                    is ProgressEntity.UninstallFailed -> {
                        // If uninstall fails during retry, revert to install failed state
                        if (isRetryingInstall) {
                            isRetryingInstall = false
                            newState = DialogViewState.InstallFailed
                        } else {
                            newState = DialogViewState.UninstallFailed
                        }
                    }

                    is ProgressEntity.UninstallSuccess -> {
                        // If uninstall succeeded as part of a retry, trigger the install
                        if (isRetryingInstall) {
                            isRetryingInstall = false
                            repo.install()
                            // Stay in a transitional state until Install starts
                            newState = DialogViewState.InstallRetryDowngradeUsingUninstall
                        } else {
                            // now it has a meaning of normal uninstall success
                            newState = DialogViewState.UninstallSuccess
                        }
                    }

                    is ProgressEntity.UninstallReady -> {
                        _uiUninstallInfo.value = repo.uninstallInfo.value
                        _uninstallFlags.value = 0 // Reset flags for new session
                        repo.config.uninstallFlags = 0
                        newState = DialogViewState.UninstallReady
                    }

                    else -> newState = DialogViewState.Ready
                }

                // Simplified package name handling. No more fetching is required.
                if (newPackageNameFromProgress != null) {
                    if (_currentPackageName.value != newPackageNameFromProgress) {
                        _currentPackageName.value = newPackageNameFromProgress
                        loadDisplayIcon(newPackageNameFromProgress)
                    }
                } else {
                    if (_currentPackageName.value != null) _currentPackageName.value = null
                }

                if (newState !is DialogViewState.InstallPrepare && autoInstallJob?.isActive == true) {
                    autoInstallJob?.cancel()
                }

                if (newState is DialogViewState.InstallPrepare && previousState !is DialogViewState.InstallPrepare) {
                    if (repo.config.installMode == ConfigEntity.InstallMode.AutoDialog) {
                        autoInstallJob?.cancel()
                        autoInstallJob = viewModelScope.launch {
                            delay(500)
                            if (state is DialogViewState.InstallPrepare && repo.config.installMode == ConfigEntity.InstallMode.AutoDialog) {
                                install()
                            }
                        }
                    }
                }

                if (newState != state) {
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
            // 使用位或运算 (or) 来添加一个 flag
            _installFlags.value = currentFlags or flag
        } else {
            // 使用位与 (and) 和 按位取反 (inv) 来移除一个 flag
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
                Timber.e(error, "Failed to load available users.")
                toast(error.message ?: "Failed to load users")
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
        state = DialogViewState.Ready
    }

    private fun analyse() {
        repo.analyse()
    }

    private fun installChoice() {
        autoInstallJob?.cancel()
        if (_currentPackageName.value != null) _currentPackageName.value = null
        state = DialogViewState.InstallChoice
    }

    private fun installPrepare() {
        val selectedEntities = repo.analysisResults.flatMap { it.appEntities }.filter { it.selected }
        val uniquePackages = selectedEntities.groupBy { it.app.packageName }

        if (uniquePackages.size == 1) {
            val targetPackageName = selectedEntities.first().app.packageName
            _currentPackageName.value = targetPackageName
            state = DialogViewState.InstallPrepare
        } else {
            // Handle case where multiple packages are selected, maybe go back to choice.
            state = DialogViewState.InstallChoice
        }
    }

    private fun installExtendedMenu() {
        when (state) {
            is DialogViewState.InstallPrepare,
            DialogViewState.InstallExtendedSubMenu,
            DialogViewState.InstallFailed -> {
                state = DialogViewState.InstallExtendedMenu
            }

            else -> {
                toast("dialog_install_extended_menu_not_available"/*R.string.dialog_install_extended_menu_not_available*/)
            }
        }
    }

    private fun installExtendedSubMenu() {
        if (state is DialogViewState.InstallExtendedMenu) {
            state = DialogViewState.InstallExtendedSubMenu
        } else {
            toast("dialog_install_extended_sub_menu_not_available"/*R.string.dialog_install_extended_sub_menu_not_available*/)
        }
    }

    private fun install() {
        autoInstallJob?.cancel()
        repo.install()
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

            // --- MODIFIED LOGIC TO PREPARE REPO STATE ---
            // Instead of modifying 'repo.entities', we now temporarily modify 'repo.analysisResults'.

            // 1. Find the original PackageAnalysisResult that the current entity belongs to.
            //    We search in 'originalAnalysisResults' which holds the complete, unmodified analysis.
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

            // 2. Create a new, temporary PackageAnalysisResult containing ONLY the current entity to be installed.
            //    We ensure it's marked as 'selected = true'.
            val tempPackageResult = originalPackageResult.copy(
                appEntities = listOf(entityToInstall.copy(selected = true))
            )

            // 3. Set the repository's state to this temporary, single-pkg state.
            //    The 'ActionHandler.install()' method will read this state.
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
            state = DialogViewState.Installing

            // Call the repo's install method. It will now operate on the temporary state we just set.
            repo.install()
        } else {
            // All installation tasks are complete.
            state = DialogViewState.InstallCompleted(multiInstallResults.toList())

            // Clean up and restore the original state.
            multiInstallQueue = emptyList()
            _installProgressText.value = null
            _currentPackageName.value = null

            // MODIFICATION: Restore the repo's original, full analysis results.
            repo.analysisResults = originalAnalysisResults
            originalAnalysisResults = emptyList()
        }
    }
}
