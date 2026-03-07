// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.privileged.provider

import android.os.Bundle
import com.rosan.installer.data.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
import com.rosan.installer.domain.settings.model.Authorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SystemInfoProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : SystemInfoProvider {
    override suspend fun getUsers(authorizer: Authorizer): Map<Int, String> {
        return withContext(Dispatchers.IO) {
            var users: Map<Int, String> = emptyMap()
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer
            ) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    users = it.privileged.users as? Map<Int, String> ?: emptyMap()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get users")
                }
            }
            users
        }
    }

    override suspend fun getSessionDetails(authorizer: Authorizer, sessionId: Int): Bundle? {
        return withContext(Dispatchers.IO) {
            var details: Bundle? = null
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer
            ) {
                try {
                    details = it.privileged.getSessionDetails(sessionId)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get session details")
                }
            }
            details
        }
    }
}
