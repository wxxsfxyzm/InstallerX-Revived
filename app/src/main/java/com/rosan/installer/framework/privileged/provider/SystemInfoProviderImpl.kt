// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import android.os.Bundle
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.framework.privileged.core.execution.dispatcher.useDirectPrivileged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SystemInfoProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : SystemInfoProvider {
    override suspend fun getUsers(authorizer: Authorizer, customizeAuthorizer: String): Map<Int, String> =
        withContext(Dispatchers.IO) {
            var users: Map<Int, String> = emptyMap()
            useDirectPrivileged(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = authorizer,
                customizeAuthorizer = customizeAuthorizer
            ) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    users = it.getUsers()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get users")
                }
            }
            users
        }

    override suspend fun getSessionDetails(authorizer: Authorizer, sessionId: Int): Bundle? =
        withContext(Dispatchers.IO) {
            runCatching {
                resolveSessionDetails(
                    capabilityProvider = capabilityProvider,
                    authorizer = authorizer,
                    customizeAuthorizer = "",
                    sessionId = sessionId
                )
            }.onFailure { error ->
                Timber.e(error, "Failed to get session details")
            }.getOrNull()
        }
}
