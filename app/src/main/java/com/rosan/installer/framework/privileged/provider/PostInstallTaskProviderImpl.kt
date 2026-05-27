// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import com.rosan.installer.framework.privileged.util.deletePaths
import com.rosan.installer.framework.privileged.util.useDirectPrivileged
import com.rosan.installer.framework.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class PostInstallTaskProviderImpl(
    private val appSettingsRepo: AppSettingsRepository,
    private val capabilityProvider: DeviceCapabilityProvider
) : PostInstallTaskProvider {

    override suspend fun executeTasks(
        authorizer: Authorizer,
        customizeAuthorizer: String,
        info: PostInstallTaskInfo
    ) {
        // Early exit if no tasks are present
        if (!info.hasTasks) return

        // 1. Fetch configuration within the coroutine scope
        val alwaysUseRootInSystem = appSettingsRepo
            .getBoolean(BooleanSetting.AlwaysUseRootInSystem, false)
            .first()

        // 2. Elevation logic: If authorizer is None, but it's a system app with AlwaysUseRoot enabled, elevate to Root
        val finalAuthorizer = if (authorizer == Authorizer.None &&
            capabilityProvider.isSystemApp &&
            alwaysUseRootInSystem
        ) {
            Timber.d("Elevating Authorizer.None to Authorizer.Root due to system app status and AlwaysUseRoot configuration.")
            Authorizer.Root
        } else authorizer

        // 3. Determine permission status based on the potentially elevated authorizer
        val noPerm = finalAuthorizer == Authorizer.None || finalAuthorizer == Authorizer.Dhizuku

        coroutineScope {
            if (!info.hasTasks) return@coroutineScope

            launch {
                if (info.enableDexopt) {
                    // Skip dexopt for non-privileged authorizers as they lack the required permissions.
                    if (noPerm) {
                        Timber.d("Dexopt skipped for non-privileged authorizer: $finalAuthorizer")
                        return@launch
                    }

                    runCatching {
                        // Pass finalAuthorizer instead of the original authorizer
                        useDirectPrivileged(
                            isSystemApp = capabilityProvider.isSystemApp,
                            authorizer = finalAuthorizer,
                            customizeAuthorizer = customizeAuthorizer
                        ) {
                            val result = it.performDexOpt(info.packageName, info.dexoptMode, info.forceDexopt)
                            Timber.i("Dexopt result: $result")
                        }
                    }.onFailure { Timber.e(it, "Dexopt failed") }
                }
            }

            launch {
                if (info.enableAutoDelete && info.deletePaths.isNotEmpty()) {
                    runCatching {
                        // Local deletion for non-privileged authorizers, silently fails on errors.
                        if (noPerm) {
                            Timber.d("Attempting local file deletion for non-privileged authorizer: $finalAuthorizer")
                            deletePaths(info.deletePaths)
                            Timber.i("Local delete completed")
                        } else {
                            // Privileged deletion using UserService.
                            // Pass finalAuthorizer instead of the original authorizer
                            useUserService(
                                isSystemApp = capabilityProvider.isSystemApp,
                                authorizer = finalAuthorizer,
                                customizeAuthorizer = customizeAuthorizer
                            ) {
                                it.privileged.delete(info.deletePaths.toTypedArray())
                                Timber.i("Privileged delete completed")
                            }
                        }
                    }.onFailure { Timber.e(it, "Delete failed") }
                }
            }
        }
    }
}
