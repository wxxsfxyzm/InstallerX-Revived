package com.rosan.installer.ui.page.installer.dialog

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.app.model.entity.AppEntity
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DialogViewModel(
    private var repo: InstallerRepo
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf<DialogViewState>(DialogViewState.Ready)
        private set

    private val _preInstallAppInfo = MutableStateFlow<InstalledAppInfo?>(null)
    val preInstallAppInfo: StateFlow<InstalledAppInfo?> = _preInstallAppInfo.asStateFlow()

    private val _currentPackageName = MutableStateFlow<String?>(null)
    val currentPackageName: StateFlow<String?> = _currentPackageName.asStateFlow()

    private var fetchPreInstallInfoJob: Job? = null
    private var autoInstallJob: Job? = null
    private var collectRepoJob: Job? = null


    fun dispatch(action: DialogViewAction) {
        when (action) {
            is DialogViewAction.CollectRepo -> collectRepo(action.repo)
            is DialogViewAction.Close -> close()
            is DialogViewAction.Analyse -> analyse()
            is DialogViewAction.InstallChoice -> installChoice()
            is DialogViewAction.InstallPrepare -> installPrepare()
            is DialogViewAction.Install -> {
                install()
            }
            is DialogViewAction.Background -> background()
        }
    }

    private fun collectRepo(repo: InstallerRepo) {
        this.repo = repo
        _preInstallAppInfo.value = null
        _currentPackageName.value = null
        collectRepoJob?.cancel()
        autoInstallJob?.cancel()
        fetchPreInstallInfoJob?.cancel()

        collectRepoJob = viewModelScope.launch {
            repo.progress.collect { progress ->
                val previousState = state
                var newState = state
                var newPackageNameFromProgress: String? = _currentPackageName.value

                when (progress) {
                    is ProgressEntity.Ready -> {
                        newState = DialogViewState.Ready
                        newPackageNameFromProgress = null
                    }
                    is ProgressEntity.Resolving -> newState = DialogViewState.Resolving
                    is ProgressEntity.ResolvedFailed -> newState = DialogViewState.ResolveFailed
                    is ProgressEntity.Analysing -> newState = DialogViewState.Analysing
                    is ProgressEntity.AnalysedFailed -> newState = DialogViewState.AnalyseFailed
                    is ProgressEntity.AnalysedSuccess -> {
                        val selectedEntities = repo.entities.filter { it.selected }
                        val mappedApps = selectedEntities.map { it.app }
                        val uniquePackages = mappedApps.groupBy { appEntity: AppEntity -> appEntity.packageName }

                        if (uniquePackages.size != 1) {
                            newState = DialogViewState.InstallChoice
                            newPackageNameFromProgress = null
                        } else {
                            newState = DialogViewState.InstallPrepare
                            newPackageNameFromProgress = selectedEntities.first().app.packageName
                        }
                    }
                    is ProgressEntity.Installing -> {
                        newState = DialogViewState.Installing
                        autoInstallJob?.cancel()
                        if (newPackageNameFromProgress == null && repo.entities.isNotEmpty()) {
                            val selectedEntities = repo.entities.filter { it.selected }
                            val mappedApps = selectedEntities.map { it.app }
                            val uniquePackages = mappedApps.groupBy { appEntity: AppEntity -> appEntity.packageName }
                            if (uniquePackages.size == 1) {
                                newPackageNameFromProgress = selectedEntities.first().app.packageName
                            }
                        }
                    }
                    is ProgressEntity.InstallFailed -> {
                        newState = DialogViewState.InstallFailed
                        autoInstallJob?.cancel()
                    }
                    is ProgressEntity.InstallSuccess -> {
                        newState = DialogViewState.InstallSuccess
                        autoInstallJob?.cancel()
                    }
                    else -> newState = DialogViewState.Ready
                }

                if (newPackageNameFromProgress != null) {
                    if (_currentPackageName.value != newPackageNameFromProgress) {
                        _currentPackageName.value = newPackageNameFromProgress
                        _preInstallAppInfo.value = null
                        fetchPreInstallAppInfo(newPackageNameFromProgress)
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

            // --- 关键修改：增加状态检查 ---
            // 只在状态不是最终状态（成功/失败）时更新 preInstallAppInfo
            // 并且包名仍然匹配
            if (state !is DialogViewState.InstallSuccess &&
                state !is DialogViewState.InstallFailed &&
                _currentPackageName.value == packageNameAtFetchStart) {
                _preInstallAppInfo.value = info
            }
            // --- 修改结束 ---
        }
    }

    private fun toast(message: String) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    private fun toast(@StringRes resId: Int) { Toast.makeText(context, resId, Toast.LENGTH_LONG).show() }

    private fun close() {
        autoInstallJob?.cancel()
        fetchPreInstallInfoJob?.cancel()
        collectRepoJob?.cancel()
        _preInstallAppInfo.value = null
        _currentPackageName.value = null
        repo.close()
        state = DialogViewState.Ready
    }

    private fun analyse() { repo.analyse() }

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

    private fun install() {
        autoInstallJob?.cancel()
        repo.install()
    }

    private fun background() { repo.background(true) }
}
