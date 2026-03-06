// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.privileged.provider

import android.content.Intent
import com.rosan.installer.data.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ComponentOpsProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : ComponentOpsProvider {
    override suspend fun startActivityPrivileged(config: ConfigModel, intent: Intent): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                special = null
            ) {
                try {
                    success = it.privileged.startActivityPrivileged(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start activity privileged: $intent")
                }
            }
            success
        }
    }

    override suspend fun sendBroadcastPrivileged(config: ConfigModel, intent: Intent): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                special = null
            ) {
                try {
                    success = it.privileged.sendBroadcastPrivileged(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to send broadcast privileged: $intent")
                }
            }
            success
        }
    }
}
