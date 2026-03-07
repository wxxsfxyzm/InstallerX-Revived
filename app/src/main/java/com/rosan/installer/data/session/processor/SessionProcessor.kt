// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.rosan.installer.data.privileged.util.useUserService
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.repository.InstallerRepository
import com.rosan.installer.domain.session.model.ConfirmationDetails
import com.rosan.installer.domain.settings.model.ConfigModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SessionProcessor : KoinComponent {
    private val capabilityProvider by inject<DeviceCapabilityProvider>()
    private val installerRepository by inject<InstallerRepository>()

    fun getSessionDetails(sessionId: Int, config: ConfigModel): ConfirmationDetails {
        var label: CharSequence? = "N/A"
        var icon: Bitmap? = null

        Timber.d("Getting session details via service (Authorizer: ${config.authorizer})")

        // Uniformly use useUserService.
        // If authorizer is None (System App), UserServiceUtil will dispatch to the local DefaultPrivilegedService.
        // If it is Root/Shizuku, it will dispatch to the corresponding IPC service.
        var bundle: Bundle? = null
        try {
            useUserService(
                isSystemApp = capabilityProvider.isSystemApp,
                authorizer = config.authorizer,
                customizeAuthorizer = config.customizeAuthorizer,
                useHookMode = false
            ) { bundle = it.privileged.getSessionDetails(sessionId) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get session details via ${config.authorizer}")
        }

        if (bundle != null) {
            label = bundle.getCharSequence("appLabel") ?: "N/A"
            val bytes = bundle.getByteArray("appIcon")
            if (bytes != null) {
                try {
                    icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode icon bitmap")
                }
            }
        } else {
            Timber.w("Service returned null bundle for session $sessionId")
        }

        return ConfirmationDetails(sessionId, label ?: "N/A", icon)
    }

    suspend fun approveSession(sessionId: Int, granted: Boolean, config: ConfigModel) {
        Timber.d("Approving session $sessionId (granted: $granted) via ${config.authorizer}")

        try {
            installerRepository.approveSession(config, sessionId, granted)
            Timber.d("Session $sessionId approval processed successfully.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to approve/reject session via ${config.authorizer}")
        }
    }
}