// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.privileged.provider

import android.content.ComponentName
import com.rosan.installer.data.privileged.util.getSpecialAuth
import com.rosan.installer.data.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.settings.model.Authorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppOpsProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : AppOpsProvider {
    override suspend fun setDefaultInstaller(authorizer: Authorizer, component: ComponentName, lock: Boolean) {
        withContext(Dispatchers.IO) {
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                special = getSpecialAuth(authorizer)
            ) { userService ->
                userService.privileged.setDefaultInstaller(component, lock)
            }
        }
    }

    override suspend fun setAdbVerifyEnabled(authorizer: Authorizer, customizeAuthorizer: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                customizeAuthorizer = customizeAuthorizer,
                special = getSpecialAuth(authorizer)
            ) { userService ->
                try {
                    userService.privileged.setAdbVerify(enabled)
                    Timber.i("Successfully requested to set ADB verify to $enabled.")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to set ADB verify")
                    throw e
                }
            }
        }
    }

    override suspend fun setPackageNetworkingEnabled(authorizer: Authorizer, uid: Int, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                useHookMode = true,
                special = getSpecialAuth(authorizer)
            ) { userService ->
                try {
                    userService.privileged.setPackageNetworkingEnabled(uid, enabled)
                    Timber.i("Network ${if (enabled) "RESTORED" else "BLOCKED"} for UID: $uid")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to set package networking via dedicated IPC method")
                    throw e
                }
            }
        }
    }
}
