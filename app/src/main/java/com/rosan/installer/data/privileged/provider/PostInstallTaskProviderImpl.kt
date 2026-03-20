// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.privileged.provider

import com.rosan.installer.data.privileged.util.deletePaths
import com.rosan.installer.data.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.settings.model.Authorizer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class PostInstallTaskProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : PostInstallTaskProvider {
    override suspend fun executeTasks(authorizer: Authorizer, customizeAuthorizer: String, info: PostInstallTaskInfo) {
        val noPerm = authorizer == Authorizer.None || authorizer == Authorizer.Dhizuku
        coroutineScope {
            if (!info.hasTasks) return@coroutineScope

            launch {
                if (info.enableDexopt) {
                    // Skip dexopt for non-privileged authorizers as they lack the required permissions.
                    if (noPerm) {
                        Timber.d("Dexopt skipped for non-privileged authorizer: $authorizer")
                        return@launch
                    }

                    runCatching {
                        useUserService(
                            isSystemApp = capabilityProvider.isSystemApp,
                            authorizer = authorizer,
                            customizeAuthorizer = customizeAuthorizer
                        ) {
                            val result = it.privileged.performDexOpt(info.packageName, info.dexoptMode, info.forceDexopt)
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
                            Timber.d("Attempting local file deletion for non-privileged authorizer: $authorizer")
                            deletePaths(info.deletePaths)
                            Timber.i("Local delete completed")
                        } else {
                            // Privileged deletion using UserService.
                            useUserService(
                                isSystemApp = capabilityProvider.isSystemApp,
                                authorizer = authorizer,
                                customizeAuthorizer = customizeAuthorizer,
                                useHookMode = false
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
