// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import android.content.Intent
import com.rosan.installer.framework.privileged.util.useDirectPrivileged
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ComponentOpsProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : ComponentOpsProvider {
    override suspend fun startActivityPrivileged(config: ConfigModel, intent: Intent): Boolean =
        withContext(Dispatchers.IO) {
            var success = false
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                special = null
            ) {
                try {
                    success = it.startActivityPrivileged(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start activity privileged: $intent")
                }
            }
            success
        }

    override suspend fun sendBroadcastPrivileged(config: ConfigModel, intent: Intent): Boolean =
        withContext(Dispatchers.IO) {
            var success = false
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                special = null
            ) {
                try {
                    success = it.sendBroadcastPrivileged(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to send broadcast privileged: $intent")
                }
            }
            success
        }
}
