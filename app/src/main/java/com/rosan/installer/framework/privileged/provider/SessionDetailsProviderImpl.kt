// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.provider

import android.os.Bundle
import com.rosan.installer.framework.privileged.core.execution.dispatcher.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.provider.SessionDetailsProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel

class SessionDetailsProviderImpl(
    private val capabilityProvider: DeviceCapabilityProvider
) : SessionDetailsProvider {
    override fun getSessionDetails(sessionId: Int, config: ConfigModel): Bundle? {
        var bundle: Bundle? = null
        useUserService(
            isSystemApp = capabilityProvider.isSystemApp,
            authorizer = config.authorizer,
            customizeAuthorizer = config.customizeAuthorizer
        ) {
            bundle = it.privileged.getSessionDetails(sessionId)
        }
        return bundle
    }
}
