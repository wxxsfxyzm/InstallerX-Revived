// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s InstallerX Revived contributors
package com.rosan.installer.data.settings.util

import android.content.Context
import com.rosan.installer.data.settings.local.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.local.room.entity.converter.InstallModeConverter
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

object ConfigUtil : KoinComponent {
    private val context by inject<Context>()
    private val appSettingsRepo by inject<AppSettingsRepo>()
    private val deviceCapabilityProvider by inject<DeviceCapabilityProvider>()

    suspend fun getGlobalAuthorizer(): Authorizer {
        val str = appSettingsRepo.getString(StringSetting.Authorizer, "").first()
        return AuthorizerConverter.revert(str)
    }

    suspend fun Authorizer.readGlobal() =
        if (this == Authorizer.Global)
            getGlobalAuthorizer()
        else
            this

    suspend fun getGlobalCustomizeAuthorizer(): String {
        return appSettingsRepo.getString(StringSetting.CustomizeAuthorizer, "").first()
    }

    suspend fun getGlobalInstallMode(): InstallMode {
        val str = appSettingsRepo.getString(StringSetting.InstallMode, "").first()
        return InstallModeConverter.revert(str)
    }

    suspend fun getByPackageName(packageName: String? = null): ConfigModel {
        var model = getByPackageNameInner(packageName)

        // Handle Global overrides for Authorizer and InstallMode
        if (model.authorizer == Authorizer.Global)
            model = model.copy(
                authorizer = getGlobalAuthorizer(),
                customizeAuthorizer = getGlobalCustomizeAuthorizer()
            )
        if (model.installMode == InstallMode.Global)
            model = model.copy(installMode = getGlobalInstallMode())

        // Apply runtime properties
        return model.apply {
            // Resolve uninstallFlags set by user
            uninstallFlags = appSettingsRepo.getInt(IntSetting.UninstallFlags, 0).first()
            // Check if the Install Requester feature is enabled in DataStore
            val isRequesterEnabled = appSettingsRepo.getBoolean(BooleanSetting.LabSetInstallRequester).first()

            if (isRequesterEnabled) {
                // Try to resolve UID from the custom 'installRequester' defined in ConfigEntity
                var targetUid: Int? = installRequester?.let { requesterPkg ->
                    runCatching {
                        context.packageManager.getPackageUid(requesterPkg, 0)
                    }.getOrNull()
                }

                // Fallback: If 'installRequester' is not set, or the package is not found on device,
                // fall back to the existing logic using the incoming 'packageName'.
                if (targetUid == null) {
                    packageName?.let { pkg ->
                        targetUid = runCatching {
                            context.packageManager.getPackageUid(pkg, 0)
                        }.getOrNull()
                    }
                }

                callingFromUid = targetUid
            }
        }
    }

    private suspend fun getByPackageNameInner(packageName: String? = null): ConfigModel =
        withContext(Dispatchers.IO) {
            val repo = get<ConfigRepo>()
            val app = getAppByPackageName(packageName)
            var config: ConfigModel? = null
            if (app != null) config = repo.find(app.configId)
            if (config != null) return@withContext config
            config = repo.all().firstOrNull()
            if (config != null) return@withContext config
            return@withContext ConfigModel.generateOptimalDefault(deviceCapabilityProvider)
        }

    private suspend fun getAppByPackageName(packageName: String? = null): AppModel? {
        val repo = get<AppRepository>()
        var app: AppModel? = repo.findByPackageName(packageName)
        if (app != null) return app
        if (packageName != null) app = repo.findByPackageName(null)
        if (app != null) return app
        return null
    }
}