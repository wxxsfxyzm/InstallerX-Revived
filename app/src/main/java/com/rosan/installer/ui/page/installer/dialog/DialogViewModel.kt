package com.rosan.installer.ui.page.installer.dialog

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
import com.rosan.installer.data.app.repo.AppIconRepo
import com.rosan.installer.data.app.util.InstallOption
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.app.util.PackageInstallerUtil
import com.rosan.installer.data.installer.model.entity.InstallResult
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.datastore.entity.NamedPackage
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.ui.page.installer.dialog.inner.UiText
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
    private val appIconRepo: AppIconRepo
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf<DialogViewState>(DialogViewState.Ready)
        private set

    // 持有原始的、完整的实体列表，用于批量安装
    private var originalEntities: List<SelectInstallEntity> = emptyList()

    var autoCloseCountDown by mutableIntStateOf(3)
        private set

    var showExtendedMenu by mutableStateOf(false)
        private set

    var showIntelligentSuggestion by mutableStateOf(true)
        private set

    var disableNotificationOnDismiss by mutableStateOf(false)
        private set

    var versionCompareInSingleLine by mutableStateOf(false)
        private set

    // 用于显示批量安装的进度文本
    private val _installProgressText = MutableStateFlow<UiText?>(null)
    val installProgressText: StateFlow<UiText?> = _installProgressText.asStateFlow()

    // 用于驱动进度条的数值进度 (0.0f to 1.0f)
    private val _installProgress = MutableStateFlow<Float?>(null)
    val installProgress: StateFlow<Float?> = _installProgress.asStateFlow()

    // 批量安装队列和结果
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

    private val _preInstallAppInfo = MutableStateFlow<InstalledAppInfo?>(null)
    val preInstallAppInfo: StateFlow<InstalledAppInfo?> = _preInstallAppInfo.asStateFlow()

    private val _currentPackageName = MutableStateFlow<String?>(null)
    val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    private val _displayIcons = MutableStateFlow<Map<String, Drawable?>>(emptyMap())
    val displayIcons: StateFlow<Map<String, Drawable?>> = _displayIcons.asStateFlow()

    // 新增一个 StateFlow 来管理安装标志位 (install flags)
    // 它的值是一个整数，通过位运算来组合所有选项
    private val _installFlags = MutableStateFlow(0) // 默认值为0，表示没有开启任何选项
    val installFlags: StateFlow<Int> = _installFlags.asStateFlow()

    // StateFlow to hold the default installer package name from global settings.
    private val _defaultInstallerFromSettings = MutableStateFlow<String?>(repo.config.installer)
    val defaultInstallerFromSettings: StateFlow<String?> = _defaultInstallerFromSettings.asStateFlow()

    // StateFlow to hold the list of managed installer packages.
    private val _managedInstallerPackages = MutableStateFlow<List<NamedPackage>>(emptyList())
    val managedInstallerPackages: StateFlow<List<NamedPackage>> = _managedInstallerPackages.asStateFlow()

    // StateFlow to hold the currently selected installer package name.
    private val _selectedInstaller = MutableStateFlow(repo.config.installer)
    val selectedInstaller: StateFlow<String?> = _selectedInstaller.asStateFlow()

    /**
     * Flag to track if the current operation is an uninstall-and-retry flow.
     * This helps the progress collector know when to trigger a reinstall.
     */
    private var isRetryingInstall = false

    private var fetchPreInstallInfoJob: Job? = null
    private val iconJobs = mutableMapOf<String, Job>()
    private var autoInstallJob: Job? = null
    private var collectRepoJob: Job? = null

    init {
        viewModelScope.launch {
            autoCloseCountDown =
                appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3).first()
            showExtendedMenu =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, false).first()
            showIntelligentSuggestion =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, false).first()
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
        }
    }

    private fun collectRepo(repo: InstallerRepo) {
        this.repo = repo
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
        // 第一次收集时，保存原始列表
        if (originalEntities.isEmpty()) {
            originalEntities = repo.entities
        }
        _preInstallAppInfo.value = null
        _currentPackageName.value = null
        val newPackageNames = repo.entities.map { it.app.packageName }.toSet()
        _displayIcons.update { old -> old.filterKeys { it in newPackageNames } }
        collectRepoJob?.cancel()
        autoInstallJob?.cancel()
        fetchPreInstallInfoJob?.cancel()

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

                    is ProgressEntity.Preparing -> newState = DialogViewState.Preparing(progress.progress)
                    is ProgressEntity.Resolving -> newState = DialogViewState.Resolving
                    is ProgressEntity.ResolvedFailed -> newState = DialogViewState.ResolveFailed
                    is ProgressEntity.Analysing -> newState = DialogViewState.Analysing
                    is ProgressEntity.AnalysedFailed -> newState = DialogViewState.AnalyseFailed
                    is ProgressEntity.AnalysedSuccess -> {
                        val containerType = repo.entities.firstOrNull()?.app?.containerType
                        val isMultiAppMode =
                            containerType == DataType.MULTI_APK || containerType == DataType.MULTI_APK_ZIP

                        if (isMultiAppMode) {
                            // If the backend (ActionHandler) determined it's a multi-app scenario,
                            // ALWAYS go to the choice screen, regardless of package names.
                            Timber.d("ViewModel: Multi-app mode detected (type: $containerType). Forcing InstallChoice state.")
                            newState = DialogViewState.InstallChoice
                            newPackageNameFromProgress = null // No single package is the focus.

                            // Trigger icon loading for all apps in the list.
                            repo.entities.forEach { entity ->
                                loadDisplayIcon(entity.app.packageName)
                            }
                        } else {
                            // If it's not a multi-app scenario (e.g., single APK with splits),
                            // it's safe to proceed directly to the prepare screen for the single app.
                            Timber.d("ViewModel: Single-app mode detected. Proceeding to InstallPrepare.")
                            newState = DialogViewState.InstallPrepare
                            newPackageNameFromProgress = repo.entities.firstOrNull()?.app?.packageName
                        }
                    }

                    is ProgressEntity.Installing -> {
                        newState = DialogViewState.Installing
                        autoInstallJob?.cancel()
                        if (newPackageNameFromProgress == null && repo.entities.isNotEmpty()) {
                            val selectedEntities = repo.entities.filter { it.selected }
                            val mappedApps = selectedEntities.map { it.app }
                            val uniquePackages =
                                mappedApps.groupBy { appEntity: AppEntity -> appEntity.packageName }
                            if (uniquePackages.size == 1) {
                                newPackageNameFromProgress =
                                    selectedEntities.first().app.packageName
                            }
                        }
                    }

                    is ProgressEntity.InstallFailed -> {
                        newState = DialogViewState.InstallFailed
                        autoInstallJob?.cancel()
                        // [FIX] Add logic to ensure the package name is identified
                        // when the dialog opens directly into this state.
                        if (newPackageNameFromProgress == null && repo.entities.isNotEmpty()) {
                            val selectedEntities = repo.entities.filter { it.selected }
                            if (selectedEntities.isNotEmpty()) {
                                newPackageNameFromProgress = selectedEntities.first().app.packageName
                            }
                        }
                    }

                    is ProgressEntity.InstallSuccess -> {
                        newState = DialogViewState.InstallSuccess
                        autoInstallJob?.cancel()
                        // [FIX] Add logic to ensure the package name is identified
                        // when the dialog opens directly into this state.
                        if (newPackageNameFromProgress == null && repo.entities.isNotEmpty()) {
                            val selectedEntities = repo.entities.filter { it.selected }
                            if (selectedEntities.isNotEmpty()) {
                                newPackageNameFromProgress = selectedEntities.first().app.packageName
                            }
                        }
                    }

                    is ProgressEntity.Uninstalling -> {
                        newState = DialogViewState.Uninstalling
                    }

                    is ProgressEntity.UninstallFailed -> {
                        // If uninstall fails during retry, revert to install failed state
                        isRetryingInstall = false
                        newState = DialogViewState.InstallFailed
                    }

                    is ProgressEntity.UninstallSuccess -> {
                        // If uninstall succeeded as part of a retry, trigger the install
                        if (isRetryingInstall) {
                            isRetryingInstall = false
                            repo.install()
                            // Stay in a transitional state until Install starts
                            newState = DialogViewState.Uninstalling
                        } else {
                            // This case shouldn't be hit in normal flow, but as a fallback:
                            newState = DialogViewState.Ready
                        }
                    }

                    else -> newState = DialogViewState.Ready
                }

                if (newPackageNameFromProgress != null) {
                    if (_currentPackageName.value != newPackageNameFromProgress) {
                        _currentPackageName.value = newPackageNameFromProgress
                        _preInstallAppInfo.value = null
                        fetchPreInstallAppInfo(newPackageNameFromProgress)
                        loadDisplayIcon(newPackageNameFromProgress)
                    } else if (_preInstallAppInfo.value == null) {
                        fetchPreInstallAppInfo(newPackageNameFromProgress)
                    }
                } else {
                    if (_currentPackageName.value != null) _currentPackageName.value = null
                    if (_preInstallAppInfo.value != null) _preInstallAppInfo.value = null
                }

                if (newState !is DialogViewState.InstallPrepare && autoInstallJob?.isActive == true) {
                    autoInstallJob?.cancel()
                }

                if (newState is DialogViewState.InstallPrepare && previousState !is DialogViewState.InstallPrepare) {
                    if (_currentPackageName.value != null && (_preInstallAppInfo.value == null || _preInstallAppInfo.value?.packageName != _currentPackageName.value)) {
                        fetchPreInstallAppInfo(_currentPackageName.value!!)
                    }

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
     * 切换（启用/禁用）一个安装标志位
     * @param flag 要操作的标志位 (来自 InstallOption.value)
     * @param enable true 表示添加该标志位, false 表示移除该标志位
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

    fun selectInstaller(packageName: String?) {
        repo.config.installer = packageName // Update the repository
        _selectedInstaller.value = packageName // Update the StateFlow
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
            val entityToInstall = repo.entities
                .filter { it.selected && it.app.packageName == packageName }
                .map { it.app }
                .filterIsInstance<AppEntity.BaseEntity>()
                .firstOrNull()

            // Define a generic icon size, could also be passed as a parameter if needed
            val iconSizePx = 256 // A reasonably high resolution

            val loadedIcon = try {
                appIconRepo.getIcon(
                    packageName = packageName,
                    entityToInstall = entityToInstall,
                    iconSizePx = iconSizePx
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

    private fun fetchPreInstallAppInfo(packageName: String) {
        if (packageName.isBlank()) {
            _preInstallAppInfo.value = null
            return
        }
        if (fetchPreInstallInfoJob?.isActive == true && _currentPackageName.value == packageName) {
            return
        }
        if (_preInstallAppInfo.value != null && _preInstallAppInfo.value?.packageName == packageName) {
            return
        }

        fetchPreInstallInfoJob?.cancel()
        fetchPreInstallInfoJob = viewModelScope.launch {
            val packageNameAtFetchStart = packageName
            val info = try {
                withContext(Dispatchers.IO) {
                    InstalledAppInfo.buildByPackageName(packageNameAtFetchStart)
                }
            } catch (e: Exception) {
                null
            }

            // 只在状态不是最终状态（成功/失败）时更新 preInstallAppInfo
            // 并且包名仍然匹配
            if (state !is DialogViewState.InstallSuccess &&
                state !is DialogViewState.InstallFailed &&
                _currentPackageName.value == packageNameAtFetchStart
            ) {
                _preInstallAppInfo.value = info
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
        fetchPreInstallInfoJob?.cancel()
        collectRepoJob?.cancel()
        _preInstallAppInfo.value = null
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
        fetchPreInstallInfoJob?.cancel()
        if (_currentPackageName.value != null) _currentPackageName.value = null
        if (_preInstallAppInfo.value != null) _preInstallAppInfo.value = null
        state = DialogViewState.InstallChoice
    }

    private fun installPrepare() {
        val targetStateIsPrepare = state is DialogViewState.InstallPrepare
        val selectedEntities = repo.entities.filter { it.selected }
        val mappedApps = selectedEntities.map { it.app }
        val uniquePackages = mappedApps.groupBy { appEntity: AppEntity -> appEntity.packageName }
        var targetPackageName: String? = null
        if (uniquePackages.size == 1) {
            targetPackageName = selectedEntities.first().app.packageName
        }
        if (targetPackageName != null) {
            if (_currentPackageName.value != targetPackageName) {
                _currentPackageName.value = targetPackageName
                _preInstallAppInfo.value = null
            }
            // Fetch info if needed when entering or already in prepare state
            if ((targetStateIsPrepare && _preInstallAppInfo.value == null) || !targetStateIsPrepare) {
                fetchPreInstallAppInfo(targetPackageName)
            }
        } else {
            if (_currentPackageName.value != null) _currentPackageName.value = null
            if (_preInstallAppInfo.value != null) _preInstallAppInfo.value = null
        }
        if (!targetStateIsPrepare) {
            state = DialogViewState.InstallPrepare
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

    private fun uninstallAndRetryInstall(keepData: Boolean) {
        val packageName = _currentPackageName.value
        if (packageName == null) {
            toast("R.string.error_no_package_to_uninstall")
            return
        }
        repo.config.uninstallFlags = if (keepData)
            PackageInstallerUtil.DELETE_KEEP_DATA
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
     * 启动批量安装
     */
    private fun installMultiple() {
        multiInstallQueue = repo.entities.filter { it.selected }
        multiInstallResults.clear()
        currentMultiInstallIndex = 0
        _installProgress.value = 0f // [新增] 批量安装开始时，进度初始化为0
        triggerNextMultiInstall()
    }

    /**
     * 触发队列中的下一个安装任务
     */
    private fun triggerNextMultiInstall() {
        if (currentMultiInstallIndex < multiInstallQueue.size) {
            val entityToInstall = multiInstallQueue[currentMultiInstallIndex]
            // 修改 repo.entities，使其只包含当前要安装的这一个应用，并确保它是 selected
            repo.entities = listOf(entityToInstall.copy(selected = true))
            val appLabel = (entityToInstall.app as? AppEntity.BaseEntity)?.label ?: entityToInstall.app.packageName

            // 更新进度文本
            _installProgressText.value = UiText(
                id = R.string.installing_progress_text,
                formatArgs = listOf(appLabel, currentMultiInstallIndex + 1, multiInstallQueue.size)
            )
            _currentPackageName.value = entityToInstall.app.packageName // 更新当前包名，让UI可以显示信息
            // 更新数值进度
            // 在任务开始前更新，所以是 (当前索引 / 总数)
            _installProgress.value = currentMultiInstallIndex.toFloat() / multiInstallQueue.size.toFloat()

            // 切换到安装中状态
            state = DialogViewState.Installing

            // 调用 repo 的 install，但只传递当前要安装的这一个实体
            repo.install()
        } else {
            // 所有任务完成
            state = DialogViewState.InstallCompleted(multiInstallResults.toList())
            // 清理并恢复状态
            multiInstallQueue = emptyList()
            _installProgressText.value = null
            _currentPackageName.value = null
            // 恢复 repo 的原始状态，以便用户可以返回或查看
            repo.entities = originalEntities
            originalEntities = emptyList()
        }
    }
}
