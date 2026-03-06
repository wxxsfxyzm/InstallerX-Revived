// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.session.manager.InstallerSessionManager
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstallerActivityViewModel(
    private val sessionManager: InstallerSessionManager,
    private val appSettingsRepo: AppSettingsRepo
) : ViewModel() {

    // 1. 维护当前的 Installer 引用
    var installer: InstallerSessionRepository? = null
        private set

    // 2. 暴露安全的 StateFlow 给 UI 观察
    private val _currentProgress = MutableStateFlow<ProgressEntity>(ProgressEntity.Ready)
    val currentProgress = _currentProgress.asStateFlow()

    fun attachInstaller(id: String?) {
        installer = sessionManager.getOrCreate(id)

        viewModelScope.launch {
            installer?.progress?.collect {
                _currentProgress.value = it // 缓存最新状态供随时读取
            }
        }
    }

    // 3. 把 onStop 的复杂逻辑搬进来
    fun handleActivityStopped(isFinishing: Boolean, isChangingConfigurations: Boolean, isRequestingPermission: Boolean) {
        if (isFinishing || isChangingConfigurations || isRequestingPermission) return

        val progress = _currentProgress.value
        val isRunning = currentProgress is ProgressEntity.InstallResolving ||
                currentProgress is ProgressEntity.InstallAnalysing ||
                currentProgress is ProgressEntity.InstallPreparing ||
                currentProgress is ProgressEntity.Installing ||
                currentProgress is ProgressEntity.InstallConfirming ||
                currentProgress is ProgressEntity.InstallingModule

        if (isRunning) {
            installer?.background(true)
        } else {
            // ... 处理完成后的逻辑
        }
    }
}