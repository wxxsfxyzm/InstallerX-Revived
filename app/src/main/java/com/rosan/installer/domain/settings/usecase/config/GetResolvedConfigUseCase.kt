// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import android.content.Context
import com.rosan.installer.domain.settings.model.AppModel
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.model.InstallMode
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.ConfigRepo
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GetResolvedConfigUseCase(
    private val context: Context,
    private val appSettingsRepo: AppSettingsRepo,
    private val configRepo: ConfigRepo,
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(packageName: String? = null): ConfigModel = withContext(Dispatchers.IO) {
        var model = getByPackageNameInner(packageName)

        // Handle Global overrides
        if (model.authorizer == Authorizer.Global) {
            val globalAuthorizer = getGlobalAuthorizer()
            model = model.copy(
                authorizer = globalAuthorizer,
                customizeAuthorizer = getGlobalCustomizeAuthorizer()
            )
        }

        if (model.installMode == InstallMode.Global) {
            val globalInstallMode = getGlobalInstallMode()
            model = model.copy(installMode = globalInstallMode)
        }

        // Apply runtime properties
        model.uninstallFlags = appSettingsRepo.getInt(IntSetting.UninstallFlags, 0).first()

        val isRequesterEnabled = appSettingsRepo.getBoolean(BooleanSetting.LabSetInstallRequester).first()
        if (isRequesterEnabled) {
            var targetUid: Int? = model.installRequester?.let { requesterPkg ->
                runCatching { context.packageManager.getPackageUid(requesterPkg, 0) }.getOrNull()
            }

            if (targetUid == null && packageName != null) {
                targetUid = runCatching { context.packageManager.getPackageUid(packageName, 0) }.getOrNull()
            }
            model.callingFromUid = targetUid
        }

        return@withContext model
    }

    private suspend fun getByPackageNameInner(packageName: String?): ConfigModel {
        val app = getAppByPackageName(packageName)
        var config: ConfigModel? = null

        if (app != null) {
            config = configRepo.find(app.configId)
            if (config != null) return config
        }

        // Fetch only the default configuration directly from the database
        // Avoid using all().firstOrNull() to save memory and IO performance
        config = configRepo.findDefault()
        if (config != null) return config

        return ConfigModel.generateOptimalDefault()
    }

    private suspend fun getAppByPackageName(packageName: String?): AppModel? {
        var app = appRepository.findByPackageName(packageName)
        if (app != null) return app
        if (packageName != null) app = appRepository.findByPackageName(null)
        return app
    }

    private suspend fun getGlobalAuthorizer() = appSettingsRepo.preferencesFlow.first().authorizer

    private suspend fun getGlobalCustomizeAuthorizer() = appSettingsRepo.getString(StringSetting.CustomizeAuthorizer, "").first()

    private suspend fun getGlobalInstallMode() = appSettingsRepo.preferencesFlow.first().installMode
}
