// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import android.content.Context
import com.rosan.installer.domain.settings.model.app.AppModel
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.domain.settings.repository.IntSetting
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GetResolvedConfigUseCase(
    private val context: Context,
    private val appSettingsRepo: AppSettingsRepository,
    private val configRepo: ConfigRepository,
    private val appRepo: AppRepository
) {
    suspend operator fun invoke(packageName: String? = null): ConfigModel = withContext(Dispatchers.IO) {
        var model = getByPackageNameInner(packageName)

        if (model.authorizer == Authorizer.Global) {
            val globalAuthorizer = getGlobalAuthorizer()
            model = model.copy(
                authorizer = globalAuthorizer,
                customizeAuthorizer = getGlobalCustomizeAuthorizer()
            )
        }

        // Always store the initiator package name so it is available if the user switches to Initiator mode in the UI later.
        model = model.copy(initiatorPackageName = packageName)

        val currentUninstallFlags = appSettingsRepo.getInt(IntSetting.UninstallFlags, 0).first()
        model = model.copy(uninstallFlags = currentUninstallFlags)

        val targetUid = when (model.installRequesterMode) {
            InstallRequesterMode.Disable -> null
            InstallRequesterMode.Initiator -> packageName?.let { initiatorPkg ->
                runCatching { context.packageManager.getPackageUid(initiatorPkg, 0) }.getOrNull()
            }
            InstallRequesterMode.Custom -> model.installRequester?.let { requesterPkg ->
                runCatching { context.packageManager.getPackageUid(requesterPkg, 0) }.getOrNull()
            }
        }
        if (targetUid != null) {
            model = model.copy(callingFromUid = targetUid)
        }

        val allowInstallWithoutUserAction = appSettingsRepo.getBoolean(BooleanSetting.LabInstallWithoutUserAction).first()
        model = model.copy(allowInstallWithoutUserAction = allowInstallWithoutUserAction)

        return@withContext model
    }

    private suspend fun getByPackageNameInner(packageName: String?): ConfigModel {
        val app = getAppByPackageName(packageName)
        var config: ConfigModel?

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
        var app = appRepo.findByPackageName(packageName)
        if (app != null) return app
        if (packageName != null) app = appRepo.findByPackageName(null)
        return app
    }

    private suspend fun getGlobalAuthorizer() = appSettingsRepo.preferencesFlow.first().authorizer

    private suspend fun getGlobalCustomizeAuthorizer() = appSettingsRepo.getString(StringSetting.CustomizeAuthorizer, "").first()
}
