// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import android.os.Bundle
import com.rosan.installer.framework.privileged.core.execution.dispatcher.useDirectPrivileged
import com.rosan.installer.framework.privileged.core.execution.dispatcher.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.provider.SessionDetailsProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.framework.privileged.core.execution.runtime.DefaultPrivilegedService
import timber.log.Timber

class SessionDetailsProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : SessionDetailsProvider {
    override fun getSessionDetails(sessionId: Int, config: ConfigModel): Bundle? =
        resolveSessionDetails(
            capabilityProvider = capabilityProvider,
            authorizer = config.authorizer,
            customizeAuthorizer = config.customizeAuthorizer,
            sessionId = sessionId
        )
}

internal fun resolveSessionDetails(
    capabilityProvider: DeviceCapabilityProvider,
    authorizer: Authorizer,
    customizeAuthorizer: String,
    sessionId: Int
): Bundle? {
    if (capabilityProvider.isSystemApp) {
        Timber.tag("SessionDetails").d("Using local system app session analysis for $sessionId")
        return DefaultPrivilegedService.system().getSessionDetails(sessionId)
    }

    var details: Bundle? = null
    useDirectPrivileged(
        isSystemApp = false,
        authorizer = authorizer,
        customizeAuthorizer = customizeAuthorizer
    ) {
        details = it.getSessionDetails(sessionId)
    }

    val currentDetails = details ?: return null
    val needsArchiveFallback = !currentDetails.containsKey("appLabel") ||
            !currentDetails.containsKey("appIcon")
    if (!needsArchiveFallback) return currentDetails

    val path = currentDetails.getString("resolvedBaseCodePath") ?: return currentDetails
    if (authorizer != Authorizer.Root && authorizer != Authorizer.Customize) {
        Timber.tag("SessionDetails").d("Archive UserService fallback is unavailable for $authorizer")
        return currentDetails
    }

    var archiveDetails: Bundle? = null
    runCatching {
        useUserService(
            isSystemApp = false,
            authorizer = authorizer,
            customizeAuthorizer = customizeAuthorizer
        ) { userService ->
            archiveDetails = userService.privileged.parsePackageArchive(path)
        }
    }.onFailure { error ->
        Timber.tag("SessionDetails").w(error, "Privileged APK parsing failed for session $sessionId")
    }

    archiveDetails?.let(currentDetails::putAll)
    return currentDetails
}
