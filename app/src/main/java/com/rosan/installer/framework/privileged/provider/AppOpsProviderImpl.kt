// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import android.content.ComponentName
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.framework.privileged.util.UserServiceUidMode
import com.rosan.installer.framework.privileged.util.getSpecialAuth
import com.rosan.installer.framework.privileged.util.useDirectPrivileged
import com.rosan.installer.framework.privileged.util.useUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppOpsProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : AppOpsProvider {
    override suspend fun setDefaultInstaller(authorizer: Authorizer, component: ComponentName, lock: Boolean) {
        withContext(Dispatchers.IO) {
            val special = getSpecialAuth(authorizer)
            if (shouldUseUserServiceForDefaultInstaller(authorizer)) {
                useUserService(
                    isSystemApp = capabilityProvider.isSystemApp,
                    authorizer = authorizer,
                    special = null,
                    uidMode = UserServiceUidMode.SystemIfRoot
                ) { userService ->
                    userService.privileged.setDefaultInstaller(component, lock)
                }
            } else {
                useDirectPrivileged(
                    isSystemApp = capabilityProvider.isSystemApp,
                    authorizer = authorizer,
                    special = special
                ) { privileged ->
                    privileged.setDefaultInstaller(component, lock)
                }
            }
        }
    }

    override suspend fun setAdbVerifyEnabled(authorizer: Authorizer, customizeAuthorizer: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                customizeAuthorizer = customizeAuthorizer
            ) { privileged ->
                try {
                    privileged.setAdbVerify(enabled)
                    Timber.i("Successfully requested to set ADB verify to $enabled.")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to set ADB verify")
                    throw e
                }
            }
        }
    }

    override suspend fun setPackageNetworkingEnabled(authorizer: Authorizer, uid: Int, enabled: Boolean) {
        if (authorizer != Authorizer.Shizuku) throw IllegalStateException("Unsupported authorizer: $authorizer")
        withContext(Dispatchers.IO) {
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer
            ) { privileged ->
                try {
                    privileged.setPackageNetworkingEnabled(uid, enabled)
                    Timber.i("Network ${if (enabled) "RESTORED" else "BLOCKED"} for UID: $uid")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to set package networking via dedicated IPC method")
                    throw e
                }
            }
        }
    }

    override suspend fun prepareUnknownSourceAppOp(
        authorizer: Authorizer,
        customizeAuthorizer: String,
        uid: Int,
        packageName: String
    ): Int? = withContext(Dispatchers.IO) {
        var mode: Int? = null
        useDirectPrivileged(
            isSystemApp = capabilityProvider.isSystemApp,
            authorizer = authorizer,
            customizeAuthorizer = customizeAuthorizer,
            special = getSpecialAuth(authorizer)
        ) { privileged ->
            mode = privileged.prepareUnknownSourceAppOp(uid, packageName)
        }
        mode
    }

    private fun shouldUseUserServiceForDefaultInstaller(authorizer: Authorizer) =
        authorizer == Authorizer.Root ||
                (authorizer == Authorizer.Shizuku &&
                        capabilityProvider.shizukuModeFlow.value == ShizukuMode.ROOT)
}
