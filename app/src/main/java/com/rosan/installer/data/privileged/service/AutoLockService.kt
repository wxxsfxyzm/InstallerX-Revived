// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.privileged.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.rosan.installer.data.settings.util.ConfigUtil
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import timber.log.Timber

class AutoLockService(
    private val context: Context,
    private val appSettingsRepo: AppSettingsRepo,
    private val appOpsProvider: AppOpsProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        isInitialized = true

        Shizuku.addBinderReceivedListener { checkAndLockForShizukuStartup() }
        if (Shizuku.pingBinder()) checkAndLockForShizukuStartup()
    }

    private fun checkAndLockForShizukuStartup() {
        scope.launch {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return@launch
                if (!appSettingsRepo.getBoolean(BooleanSetting.AutoLockInstaller, false).first()) return@launch

                if (ConfigUtil.getGlobalAuthorizer() == Authorizer.Shizuku) {
                    executeLock(Authorizer.Shizuku, "ShizukuStartup")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed during Shizuku startup check")
            }
        }
    }

    fun onResolveInstall(sessionAuthorizer: Authorizer) {
        scope.launch {
            try {
                if (!appSettingsRepo.getBoolean(BooleanSetting.AutoLockInstaller, false).first()) return@launch
                if (sessionAuthorizer in listOf(Authorizer.Root, Authorizer.Shizuku, Authorizer.Dhizuku)) {
                    if (sessionAuthorizer == Authorizer.Shizuku && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return@launch
                    executeLock(sessionAuthorizer, "InstallSession")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed during install session check")
            }
        }
    }

    private suspend fun executeLock(authorizer: Authorizer, source: String) {
        try {
            Timber.d("Locking via $authorizer (Source: $source)")
            val component = ComponentName(context, "com.rosan.installer.ui.activity.InstallerActivity")
            appOpsProvider.setDefaultInstaller(authorizer = authorizer, component = component, lock = true)
        } catch (e: Exception) {
            Timber.e(e, "AutoLock execution failed")
        }
    }
}
