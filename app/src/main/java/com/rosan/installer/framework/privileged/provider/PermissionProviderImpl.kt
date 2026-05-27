// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import com.rosan.installer.framework.privileged.util.getSpecialAuth
import com.rosan.installer.framework.privileged.util.useDirectPrivileged
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.PermissionProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PermissionProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : PermissionProvider {
    override suspend fun grantRuntimePermission(authorizer: Authorizer, packageName: String, permission: String) {
        withContext(Dispatchers.IO) {
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                special = getSpecialAuth(authorizer)
            ) {
                try {
                    it.grantRuntimePermission(packageName, permission)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to grant permission '$permission' for '$packageName'")
                    throw e
                }
            }
        }
    }

    override suspend fun isPermissionGranted(authorizer: Authorizer, packageName: String, permission: String): Boolean {
        return withContext(Dispatchers.IO) {
            var isGranted = false
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                special = getSpecialAuth(authorizer)
            ) {
                try {
                    isGranted = it.isPermissionGranted(packageName, permission)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to check permission '$permission' for '$packageName'")
                    isGranted = false
                    throw e
                }
            }
            isGranted
        }
    }
}
