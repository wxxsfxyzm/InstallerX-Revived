// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.engine.model.AppEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.sourcePath
import com.rosan.installer.domain.engine.usecase.GetAppIconColorUseCase
import com.rosan.installer.domain.engine.usecase.GetAppIconUseCase
import com.rosan.installer.domain.privileged.usecase.GetAvailableUsersUseCase
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.util.addFlag
import com.rosan.installer.util.getErrorMessage
import com.rosan.installer.util.hasFlag
import com.rosan.installer.util.removeFlag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File

class InstallerViewModel(
    private var session: InstallerSessionRepository,
    private val appSettingsRepo: AppSettingsRepository,
    private val getAvailableUsers: GetAvailableUsersUseCase,
    private val getAppIcon: GetAppIconUseCase,
    private val getAppIconColor: GetAppIconColorUseCase,
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    // Event channel for one-off side effects (e.g. Toasts)
    private val _uiEvents = MutableSharedFlow<InstallerViewEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents: SharedFlow<InstallerViewEvent> = _uiEvents.asSharedFlow()

    // Cache default seed color to avoid heavy recalculation
    private var defaultFallbackSeedColor: Int? = null

    // Internal mutable state for high-frequency UI changes and progress
    private val _localState = MutableStateFlow(
        InstallerState(
            // Use the aggregated ConfigModel
            config = session.config,
            error = session.error
        )
    )

    // The single source of truth for the UI.
    // Combines dynamic local state with reactive global app settings.
    val uiState: StateFlow<InstallerState> = combine(
        _localState,
        appSettingsRepo.preferencesFlow
    ) { local, prefs ->
        local.copy(
            viewSettings = local.viewSettings.copy(
                useBlur = prefs.useBlur,
                autoCloseCountDown = prefs.dhizukuAutoCloseCountDown,
                showExtendedMenu = prefs.showDialogInstallExtendedMenu,
                showSmartSuggestion = prefs.showSmartSuggestion,
                disableNotificationOnDismiss = prefs.disableNotificationForDialogInstall,
                versionCompareInSingleLine = prefs.versionCompareInSingleLine,
                sdkCompareInMultiLine = prefs.sdkCompareInMultiLine,
                showOPPOSpecial = local.tempShowOPPOSpecial ?: prefs.showOPPOSpecial,
                autoSilentInstall = prefs.autoSilentInstall,
                labTapIconToShare = prefs.labTapIconToShare
            ),
            managedInstallerPackages = prefs.managedInstallerPackages,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _localState.value
    )

    val isInstallingModule: Boolean
        get() = _localState.value.analysisResults.any { result ->
            result.appEntities.any { entity -> entity.selected && entity.app is AppEntity.ModuleEntity }
        }

    private var originalAnalysisResults: List<PackageAnalysisResult> = emptyList()
    private var isRetryingInstall = false

    private var loadingStateJob: Job? = null
    private val iconJobs = mutableMapOf<String, Job>()
    private var autoInstallJob: Job? = null
    private val settingsLoadingJob: Job
    private var collectRepoJob: Job? = null

    init {
        settingsLoadingJob = loadInitialSettings()
    }

    /**
     * Loads specific settings that might not be exposed directly in preferencesFlow,
     * but are needed locally by the installer.
     */
    private fun loadInitialSettings() = viewModelScope.launch {
        _localState.update { state ->
            state.copy(
                viewSettings = state.viewSettings.copy(
                    uiExpressive = appSettingsRepo.getBoolean(BooleanSetting.UiExpressiveSwitch, true).first(),
                    preferSystemIconForUpdates = appSettingsRepo.getBoolean(BooleanSetting.PreferSystemIconForInstall, false).first(),
                    enableModuleInstall = appSettingsRepo.getBoolean(BooleanSetting.LabEnableModuleFlash, false).first(),
                    useDynColorFollowPkgIcon = appSettingsRepo.getBoolean(BooleanSetting.UiDynColorFollowPkgIcon, false).first()
                )
            )
        }
    }

    /**
     * Centralized helper function to update ConfigModel immutably.
     * Ensures both UI state and underlying session config are synchronized.
     */
    fun updateConfig(updateBlock: (ConfigModel) -> ConfigModel) {
        val newConfig = updateBlock(_localState.value.config)
        session.config = newConfig
        _localState.update { it.copy(config = newConfig) }
    }

    fun dispatch(action: InstallerViewAction) {
        when (action) {
            is InstallerViewAction.CollectSession -> collectRepo(action.session)
            is InstallerViewAction.Close -> close()
            is InstallerViewAction.Cancel -> cancel()
            is InstallerViewAction.Analyse -> analyse()
            is InstallerViewAction.InstallChoice -> {
                _localState.update { it.copy(navigatedFromPrepareToChoice = uiState.value.stage is InstallerStage.InstallPrepare) }
                installChoice()
            }

            is InstallerViewAction.InstallPrepare -> installPrepare()
            is InstallerViewAction.InstallExtendedMenu -> installExtendedMenu()
            is InstallerViewAction.InstallExtendedSubMenu -> installExtendedSubMenu()
            is InstallerViewAction.InstallMultiple -> installMultiple()
            is InstallerViewAction.Install -> install()
            is InstallerViewAction.Background -> background()
            is InstallerViewAction.Reboot -> session.reboot(action.reason)
            is InstallerViewAction.UninstallAndRetryInstall -> uninstallAndRetryInstall(action.keepData, action.conflictingPackage)
            is InstallerViewAction.Uninstall -> session.uninstallInfo.value?.packageName?.let { session.uninstall(it) }

            is InstallerViewAction.ShowMiuixSheetRightActionSettings -> _localState.update { it.copy(showMiuixSheetRightActionSettings = true) }
            is InstallerViewAction.HideMiuixSheetRightActionSettings -> _localState.update { it.copy(showMiuixSheetRightActionSettings = false) }
            is InstallerViewAction.ShowMiuixPermissionList -> _localState.update { it.copy(showMiuixPermissionList = true) }
            is InstallerViewAction.HideMiuixPermissionList -> _localState.update { it.copy(showMiuixPermissionList = false) }

            is InstallerViewAction.SetTempShowOPPOSpecial -> _localState.update { it.copy(tempShowOPPOSpecial = action.show) }
            is InstallerViewAction.ToggleSelection -> toggleSelection(action.packageName, action.entity, action.isMultiSelect)
            is InstallerViewAction.ToggleUninstallFlag -> toggleUninstallFlag(action.flag, action.enable)
            is InstallerViewAction.SetInstaller -> selectInstaller(action.installer)
            is InstallerViewAction.SetTargetUser -> selectTargetUser(action.userId)
            is InstallerViewAction.ApproveSession -> session.approveConfirmation(action.sessionId, action.granted)
            is InstallerViewAction.ShareApp -> shareApp(action.appEntity)
            is InstallerViewAction.ShowToast -> toast(action.message)
            is InstallerViewAction.ShowToastRes -> toast(action.messageResId)
        }
    }

    private fun mapProgressToStage(
        progress: ProgressEntity,
        currentAnalysisResults: List<PackageAnalysisResult>,
        isRetrying: Boolean
    ) = when (progress) {
        ProgressEntity.Ready -> InstallerStage.Ready
        ProgressEntity.UninstallResolveFailed,
        ProgressEntity.InstallResolvedFailed -> InstallerStage.ResolveFailed

        ProgressEntity.InstallAnalysedFailed -> InstallerStage.AnalyseFailed

        ProgressEntity.InstallAnalysedSuccess -> {
            val containerType = currentAnalysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType

            val isMultiAppMode = currentAnalysisResults.size > 1 ||
                    containerType == DataType.MULTI_APK ||
                    containerType == DataType.MULTI_APK_ZIP ||
                    containerType == DataType.MIXED_MODULE_APK ||
                    containerType == DataType.MIXED_MODULE_ZIP

            if (isMultiAppMode) InstallerStage.InstallChoice else InstallerStage.InstallPrepare
        }

        is ProgressEntity.Installing -> {
            val floatProgress = if (progress.total > 1) progress.current.toFloat() / progress.total.toFloat() else 0f
            InstallerStage.Installing(floatProgress, progress.current, progress.total, progress.appLabel)
        }

        is ProgressEntity.InstallCompleted -> InstallerStage.InstallCompleted(progress.results)

        ProgressEntity.InstallFailed -> {
            if (isInstallingModule) {
                val currentOutput = session.moduleLog.toMutableList()
                session.error.message?.let { msg ->
                    val errorLine = "ERROR: $msg"
                    if (currentOutput.lastOrNull() != errorLine) currentOutput.add(errorLine)
                }
                InstallerStage.InstallingModule(output = currentOutput, isFinished = true)
            } else InstallerStage.InstallFailed
        }

        ProgressEntity.InstallSuccess -> {
            if (isInstallingModule) InstallerStage.InstallingModule(output = session.moduleLog, isFinished = true)
            else InstallerStage.InstallSuccess
        }

        is ProgressEntity.InstallingModule -> InstallerStage.InstallingModule(progress.output)

        ProgressEntity.InstallConfirming -> {
            val details = session.confirmationDetails.value
            if (details != null) InstallerStage.InstallConfirm(details.appLabel, details.appIcon, details.sessionId)
            else InstallerStage.ResolveFailed
        }

        ProgressEntity.Uninstalling -> if (isRetrying) InstallerStage.InstallRetryDowngradeUsingUninstall else InstallerStage.Uninstalling
        ProgressEntity.UninstallFailed -> if (isRetrying) InstallerStage.InstallFailed else InstallerStage.UninstallFailed
        ProgressEntity.UninstallSuccess -> if (isRetrying) InstallerStage.InstallRetryDowngradeUsingUninstall else InstallerStage.UninstallSuccess
        ProgressEntity.UninstallReady -> InstallerStage.UninstallReady
        ProgressEntity.InstallResolving, ProgressEntity.InstallAnalysing, is ProgressEntity.InstallPreparing -> _localState.value.stage
        else -> InstallerStage.Ready
    }

    private fun collectRepo(session: InstallerSessionRepository) {
        this.session = session
        if (session.config.enableCustomizeUser) loadAvailableUsers(session.config.authorizer)

        _localState.update {
            it.copy(
                config = session.config, // Synchronize the entire ConfigModel to UI state
                currentPackageName = null,
                analysisResults = session.analysisResults,
                displayIcons = it.displayIcons.filterKeys { key -> key in session.analysisResults.map { res -> res.packageName } },
                error = session.error
            )
        }

        collectRepoJob?.cancel()
        autoInstallJob?.cancel()

        collectRepoJob = viewModelScope.launch {
            settingsLoadingJob.join()

            // Core fix: Listen to both progress and uninstallInfo flows simultaneously
            combine(session.progress, session.uninstallInfo) { progress, uninstallInfo ->
                Pair(progress, uninstallInfo)
            }.collect { (progress, uninstallInfo) ->

                if (progress is ProgressEntity.InstallResolving || progress is ProgressEntity.InstallPreparing || progress is ProgressEntity.InstallAnalysing) {
                    if (isInstallingModule) {
                        loadingStateJob?.cancel()
                        _localState.update {
                            it.copy(stage = if (progress is ProgressEntity.InstallPreparing) InstallerStage.Preparing(progress.progress) else InstallerStage.Analysing)
                        }
                    } else if (loadingStateJob == null || !loadingStateJob!!.isActive) {
                        loadingStateJob = viewModelScope.launch {
                            delay(200L)
                            _localState.update {
                                it.copy(stage = if (progress is ProgressEntity.InstallPreparing) InstallerStage.Preparing(progress.progress) else InstallerStage.Analysing)
                            }
                        }
                    }
                    return@collect
                }

                loadingStateJob?.cancel()
                loadingStateJob = null

                // Handle side effects BEFORE mapping the stage
                if (progress is ProgressEntity.InstallAnalysedSuccess) {
                    if (originalAnalysisResults.isEmpty()) {
                        originalAnalysisResults = session.analysisResults
                    }
                    // Update state first
                    _localState.update { it.copy(analysisResults = session.analysisResults) }
                    // Trigger side effects (like loading icons) after the state is fully updated
                    session.analysisResults.forEach { result -> loadDisplayIcon(result.packageName) }
                }

                // Pass the current results to the pure mapper
                val newStage = mapProgressToStage(progress, session.analysisResults, isRetryingInstall)

                // Handle retry side effects AFTER mapping the UI stage
                if (isRetryingInstall) {
                    when (progress) {
                        is ProgressEntity.UninstallFailed -> {
                            isRetryingInstall = false
                        }

                        is ProgressEntity.UninstallSuccess -> {
                            isRetryingInstall = false
                            session.install(false)
                        }

                        else -> {}
                    }
                }

                // Optimized package name resolution logic, covering all uninstall stages
                val newPackageName = when (newStage) {
                    is InstallerStage.Installing -> {
                        if (newStage.total > 1) {
                            val selectedEntities = _localState.value.analysisResults.flatMap { it.appEntities }.filter { it.selected }
                            val groupedApps = selectedEntities.groupBy { it.app.packageName }.values.toList()
                            groupedApps.getOrNull(newStage.current - 1)?.firstOrNull()?.app?.packageName ?: _localState.value.currentPackageName
                        } else {
                            _localState.value.currentPackageName
                                ?: _localState.value.analysisResults.firstNotNullOfOrNull { r -> if (r.appEntities.any { it.selected }) r.packageName else null }
                                ?: _localState.value.analysisResults.firstOrNull()?.packageName
                        }
                    }

                    is InstallerStage.InstallPrepare, is InstallerStage.InstallFailed, is InstallerStage.InstallSuccess -> {
                        _localState.value.currentPackageName ?: _localState.value.analysisResults.firstOrNull()?.packageName
                    }

                    is InstallerStage.InstallChoice, is InstallerStage.Ready -> null

                    // All uninstall stages read directly from the latest uninstallInfo
                    is InstallerStage.UninstallReady,
                    is InstallerStage.Uninstalling,
                    is InstallerStage.UninstallSuccess,
                    is InstallerStage.UninstallFailed -> uninstallInfo?.packageName

                    else -> _localState.value.currentPackageName
                }

                val oldPackageName = _localState.value.currentPackageName

                _localState.update { currentState ->
                    val updatedState = currentState.copy(
                        stage = newStage,
                        currentPackageName = newPackageName, // Explicitly assign the new value, no fallback needed
                        uiUninstallInfo = uninstallInfo ?: currentState.uiUninstallInfo, // Synchronize uninstall info in real-time
                        error = session.error
                    )

                    // Re-calculate seed color from the icon if dynamic color is enabled
                    if (updatedState.viewSettings.useDynColorFollowPkgIcon) {
                        if (newPackageName.isNullOrEmpty()) {
                            if (defaultFallbackSeedColor != null) {
                                _localState.update { it.copy(seedColor = Color(defaultFallbackSeedColor!!)) }
                            } else {
                                viewModelScope.launch {
                                    defaultFallbackSeedColor = getAppIconColor(
                                        sessionId = session.id,
                                        packageName = "",
                                        preferSystemIcon = updatedState.viewSettings.preferSystemIconForUpdates
                                    )
                                    _localState.update { it.copy(seedColor = defaultFallbackSeedColor?.let { c -> Color(c) }) }
                                }
                            }
                        } else {
                            viewModelScope.launch {
                                val colorInt = getAppIconColor(
                                    sessionId = session.id,
                                    packageName = newPackageName,
                                    preferSystemIcon = updatedState.viewSettings.preferSystemIconForUpdates
                                )
                                _localState.update { it.copy(seedColor = colorInt?.let { c -> Color(c) }) }
                            }
                        }
                    }

                    updatedState
                }

                // Icon loading logic: load only when the package name changes and is not null/empty
                if (newPackageName != null && newPackageName != oldPackageName) {
                    loadDisplayIcon(newPackageName)
                }

                autoInstallJob?.cancel()
                if (newStage is InstallerStage.InstallPrepare && session.config.installMode == InstallMode.AutoDialog) {
                    autoInstallJob = viewModelScope.launch {
                        delay(500)
                        if (_localState.value.stage is InstallerStage.InstallPrepare) install()
                    }
                }
            }
        }
    }

    fun toggleInstallFlag(flag: Int, enable: Boolean) {
        updateConfig { currentConfig ->
            val newFlags = if (enable) currentConfig.installFlags.addFlag(flag) else currentConfig.installFlags.removeFlag(flag)
            currentConfig.copy(installFlags = newFlags)
        }
    }

    fun toggleBypassBlacklist(enable: Boolean) {
        updateConfig { it.copy(bypassBlacklistInstallSetByUser = enable) }
    }

    private fun selectInstaller(packageName: String?) {
        updateConfig { it.copy(installer = packageName) }
    }

    private fun selectTargetUser(userId: Int) {
        updateConfig { it.copy(targetUserId = userId) }
    }

    private fun loadAvailableUsers(authorizer: Authorizer) {
        viewModelScope.launch {
            getAvailableUsers(authorizer)
                .onSuccess { users ->
                    _localState.update { it.copy(availableUsers = users) }
                    // If the currently selected user is not in the available list, reset it to 0 (Owner).
                    if (!users.containsKey(_localState.value.config.targetUserId)) selectTargetUser(0)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.e(error, "Failed to load available users.")
                    toast(error.getErrorMessage(context))

                    _localState.update { it.copy(availableUsers = emptyMap()) }
                    if (_localState.value.config.targetUserId != 0) selectTargetUser(0)
                }
        }
    }

    private fun loadDisplayIcon(packageName: String) {
        if (packageName.isBlank()) return
        if (_localState.value.displayIcons[packageName] != null || iconJobs[packageName]?.isActive == true) return

        _localState.update { it.copy(displayIcons = it.displayIcons + (packageName to null)) }

        iconJobs[packageName]?.cancel()
        iconJobs[packageName] = viewModelScope.launch {
            val rawEntities = _localState.value.analysisResults.find { it.packageName == packageName }?.appEntities?.map { it.app }
            val entityToInstall = rawEntities?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
                ?: rawEntities?.filterIsInstance<AppEntity.ModuleEntity>()?.firstOrNull()

            val loadedIconBitmap = getAppIcon(
                sessionId = session.id,
                packageName = packageName,
                entityToInstall = entityToInstall,
                preferSystemIcon = uiState.value.viewSettings.preferSystemIconForUpdates
            )

            val finalImageBitmap = loadedIconBitmap?.asImageBitmap()

            _localState.update {
                if (it.displayIcons[packageName] == null) it.copy(displayIcons = it.displayIcons + (packageName to finalImageBitmap))
                else it
            }
        }
    }

    private fun toast(message: String) = _uiEvents.tryEmit(InstallerViewEvent.ShowToast(message))
    private fun toast(@StringRes resId: Int) = _uiEvents.tryEmit(InstallerViewEvent.ShowToastRes(resId))

    private fun close() {
        autoInstallJob?.cancel()
        collectRepoJob?.cancel()
        iconJobs.values.forEach { it.cancel() }
        iconJobs.clear()
        session.close()
        _localState.update { it.copy(currentPackageName = null, uiUninstallInfo = null, stage = InstallerStage.Ready) }
    }

    private fun cancel() {
        autoInstallJob?.cancel()
        iconJobs.values.forEach { it.cancel() }
        session.cancel()
    }

    private fun analyse() = session.analyse()

    private fun installChoice() {
        autoInstallJob?.cancel()

        // Read the current data from the Single Source of Truth (_localState)
        var currentResults = _localState.value.analysisResults

        val containerType = currentResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType
        if (containerType == DataType.MIXED_MODULE_APK) {
            // Generate a new list with all selections cleared
            currentResults = currentResults.map { result ->
                result.copy(appEntities = result.appEntities.map { it.copy(selected = false) })
            }
            // Sync the updated list back to the underlying session
            session.analysisResults = currentResults.toMutableList()
        }

        // Update all relevant UI states in a single transaction
        _localState.update {
            it.copy(
                currentPackageName = null,
                stage = InstallerStage.InstallChoice,
                analysisResults = currentResults // Ensure the UI receives the latest list
            )
        }
    }

    private fun installPrepare() {
        // Read from _localState instead of session
        val selectedEntities = _localState.value.analysisResults.flatMap { it.appEntities }.filter { it.selected }
        val uniquePackages = selectedEntities.groupBy { it.app.packageName }

        if (uniquePackages.size == 1) {
            val targetPackageName = selectedEntities.first().app.packageName
            _localState.update {
                it.copy(
                    currentPackageName = targetPackageName,
                    stage = InstallerStage.InstallPrepare,
                    seedColor = if (it.viewSettings.useDynColorFollowPkgIcon)
                        _localState.value.analysisResults.find { res -> res.packageName == targetPackageName }?.seedColor?.let { c -> Color(c) }
                    else null
                )
            }
        } else {
            _localState.update { it.copy(stage = InstallerStage.InstallChoice) }
        }
    }

    private fun installExtendedMenu() {
        if (_localState.value.stage in listOf(
                InstallerStage.InstallPrepare,
                InstallerStage.InstallExtendedSubMenu,
                InstallerStage.InstallFailed
            )
        ) {
            _localState.update { it.copy(stage = InstallerStage.InstallExtendedMenu) }
        } else toast(R.string.error_dialog_install_menu_not_available)
    }

    private fun installExtendedSubMenu() {
        if (_localState.value.stage is InstallerStage.InstallExtendedMenu) {
            _localState.update { it.copy(stage = InstallerStage.InstallExtendedSubMenu) }
        } else toast(R.string.error_dialog_install_menu_not_available)
    }

    private fun install() {
        autoInstallJob?.cancel()
        Timber.d("Standard foreground installation triggered. Contains Module: $isInstallingModule")
        session.install(true)
    }

    private fun background() = session.background(true)

    fun toggleSelection(packageName: String, entityToToggle: SelectInstallEntity, isMultiSelect: Boolean) {
        val currentResults = _localState.value.analysisResults.toMutableList()
        val packageIndex = currentResults.indexOfFirst { it.packageName == packageName }

        if (packageIndex != -1) {
            val packageToUpdate = currentResults[packageIndex]
            val updatedEntities = packageToUpdate.appEntities.map { currentEntity ->
                if (currentEntity === entityToToggle) currentEntity.copy(selected = !currentEntity.selected)
                else if (!isMultiSelect) currentEntity.copy(selected = false)
                else currentEntity
            }.toMutableList()

            if (!isMultiSelect && entityToToggle.selected) {
                updatedEntities.replaceAll { it.copy(selected = false) }
            }

            val newPackageAnalysisResult = packageToUpdate.copy(appEntities = updatedEntities)
            currentResults[packageIndex] = newPackageAnalysisResult

            // Sync to session
            session.analysisResults = currentResults

            // Correctly update the StateFlow with new data, Compose will recompose automatically
            _localState.update { it.copy(analysisResults = currentResults.toList()) }
        }
    }

    private fun toggleUninstallFlag(flag: Int, enable: Boolean) {
        updateConfig { currentConfig ->
            val currentFlags = currentConfig.uninstallFlags
            var newFlags = if (enable) currentFlags.addFlag(flag) else currentFlags.removeFlag(flag)

            if (enable && flag == PackageManagerUtil.DELETE_ALL_USERS && currentFlags.hasFlag(PackageManagerUtil.DELETE_SYSTEM_APP)) {
                newFlags = newFlags.removeFlag(PackageManagerUtil.DELETE_SYSTEM_APP)
                toast(R.string.uninstall_system_app_disabled)
            } else if (enable && flag == PackageManagerUtil.DELETE_SYSTEM_APP && currentFlags.hasFlag(PackageManagerUtil.DELETE_ALL_USERS)) {
                newFlags = newFlags.removeFlag(PackageManagerUtil.DELETE_ALL_USERS)
                toast(R.string.uninstall_all_users_disabled)
            }

            currentConfig.copy(uninstallFlags = newFlags)
        }
    }

    private fun uninstallAndRetryInstall(keepData: Boolean, conflictingPackage: String?) {
        val targetPackageName = conflictingPackage ?: _localState.value.currentPackageName
        if (targetPackageName == null) {
            toast(R.string.error_no_package_to_uninstall)
            return
        }

        updateConfig { it.copy(uninstallFlags = if (keepData) PackageManagerUtil.DELETE_KEEP_DATA else 0) }

        isRetryingInstall = true
        Timber.d("Uninstalling conflicting/old package: $targetPackageName for retry")
        session.uninstall(targetPackageName)
    }

    private fun installMultiple() {
        // Read from _localState instead of session
        val selectedEntities = _localState.value.analysisResults.flatMap { it.appEntities }.filter { it.selected }
        session.installMultiple(selectedEntities)
    }

    private fun shareApp(entity: AppEntity) {
        Timber.d("Sharing app: $entity")
        try {
            val filePath = entity.data.sourcePath()
            if (filePath == null) {
                toast("Invalid file entity for sharing")
                return
            }

            val fileToShare = File(filePath)
            if (!fileToShare.exists()) {
                toast("File does not exist")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, fileToShare)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    entity is AppEntity.ModuleEntity -> "application/zip"
                    filePath.endsWith(".apkm", true) || filePath.endsWith(".apks", true) || filePath.endsWith(".xapk", true) -> "application/zip"
                    else -> "application/vnd.android.package-archive"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, null)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Timber.e(e, "Failed to share file")
            toast("Failed to share file")
        }
    }
}
