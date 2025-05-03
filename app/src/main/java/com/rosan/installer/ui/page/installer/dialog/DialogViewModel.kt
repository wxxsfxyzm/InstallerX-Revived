package com.rosan.installer.ui.page.installer.dialog

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.app.util.InstalledAppInfo
import com.rosan.installer.data.app.util.sortedBest
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.repo.InstallerRepo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.Job
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

    fun dispatch(action: DialogViewAction) {
        when (action) {
            is DialogViewAction.CollectRepo -> collectRepo(action.repo)
            is DialogViewAction.Close -> close()
            is DialogViewAction.Analyse -> analyse()
            is DialogViewAction.InstallChoice -> installChoice()
            is DialogViewAction.InstallPrepare -> installPrepare()
            is DialogViewAction.Install -> {
                viewModelScope.launch {
                    fetchAndStorePreInstallInfoSuspend()
                    install()
                }
            }
            is DialogViewAction.Background -> background()
        }
    }

    private var collectRepoJob: Job? = null

    private fun collectRepo(repo: InstallerRepo) {
        this.repo = repo
        _preInstallAppInfo.value = null
        _currentPackageName.value = null
        collectRepoJob?.cancel()
        collectRepoJob = viewModelScope.launch {
            repo.progress.collect { progress ->
                val previousState = state
                var newState = when (progress) {
                    is ProgressEntity.Ready -> DialogViewState.Ready
                    is ProgressEntity.Resolving -> DialogViewState.Resolving
                    is ProgressEntity.ResolvedFailed -> DialogViewState.ResolveFailed
                    is ProgressEntity.Analysing -> DialogViewState.Analysing
                    is ProgressEntity.AnalysedFailed -> DialogViewState.AnalyseFailed
                    is ProgressEntity.AnalysedSuccess -> {
                        if (repo.entities.filter { it.selected }
                                .groupBy { it.app.packageName }.size != 1) {
                            DialogViewState.InstallChoice
                        } else {
                            DialogViewState.InstallPrepare
                        }
                    }
                    is ProgressEntity.Installing -> DialogViewState.Installing
                    is ProgressEntity.InstallFailed -> DialogViewState.InstallFailed
                    is ProgressEntity.InstallSuccess -> DialogViewState.InstallSuccess
                    else -> DialogViewState.Ready
                }

                if (newState is DialogViewState.Installing &&
                    previousState !is DialogViewState.InstallPrepare &&
                    previousState !is DialogViewState.Installing &&
                    _preInstallAppInfo.value == null) {
                    launch { fetchAndStorePreInstallInfoSuspend() }
                }

                if (newState is DialogViewState.InstallPrepare && previousState !is DialogViewState.InstallPrepare) {
                    if (repo.config.installMode == ConfigEntity.InstallMode.AutoDialog) {
                        dispatch(DialogViewAction.Install)
                    } else {
                    }
                }


                if (newState != previousState) {
                    if (state != newState) {
                        state = newState
                    }
                }
            }
        }
    }

    private suspend fun fetchAndStorePreInstallInfoSuspend() {
        val entitiesToInstall = repo.entities.filter { it.selected }.map { it.app }.sortedBest()
        val uniquePackages = entitiesToInstall.groupBy { it.packageName }

        if (entitiesToInstall.isNotEmpty() && uniquePackages.size == 1) {
            val entity = entitiesToInstall.first()
            val packageName = entity.packageName
            _currentPackageName.value = packageName
            try {
                val info = withContext(Dispatchers.IO) {
                    InstalledAppInfo.buildByPackageName(packageName)
                }
                _preInstallAppInfo.value = info
            } catch (e: Exception) {
                _currentPackageName.value = null
                _preInstallAppInfo.value = null
            }
        } else {
            _currentPackageName.value = null
            _preInstallAppInfo.value = null
        }
    }


    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun toast(@StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    private fun close() {
        _preInstallAppInfo.value = null
        _currentPackageName.value = null
        repo.close()
    }

    private fun analyse() {
        repo.analyse()
    }

    private fun installChoice() {
        _preInstallAppInfo.value = null
        _currentPackageName.value = null
        state = DialogViewState.InstallChoice
    }

    private fun installPrepare() {
        if(state !is DialogViewState.InstallPrepare) {
            state = DialogViewState.InstallPrepare
        }
    }

    private fun install() {
        repo.install()
    }

    private fun background() {
        repo.background(true)
    }
}
