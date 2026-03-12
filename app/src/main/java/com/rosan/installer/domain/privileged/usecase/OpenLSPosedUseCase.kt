// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.rosan.installer.SecretCodeReceiver.Companion.SECRET_CODE_ACTION
import com.rosan.installer.SecretCodeReceiver.Companion.SECRET_CODE_ACTION_OLD
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import kotlinx.coroutines.withTimeoutOrNull

class OpenLSPosedUseCase(
    private val componentOpsProvider: ComponentOpsProvider,
    private val capabilityProvider: DeviceCapabilityProvider
) {
    companion object {
        private const val PRIVILEGED_START_TIMEOUT_MS = 2500L
        private const val LSPOSED_SECRET_CODE = "android_secret_code://5776733"
    }

    /**
     * Attempts to open LSPosed via a privileged broadcast.
     * @return true if the broadcast was sent, false if skipped due to authorizer rules.
     */
    suspend operator fun invoke(config: ConfigModel): Boolean {
        val shouldAttemptPrivileged = config.authorizer == Authorizer.Root ||
                config.authorizer == Authorizer.Shizuku ||
                (config.authorizer == Authorizer.None && capabilityProvider.isSystemApp)

        if (!shouldAttemptPrivileged) return false

        val intent = Intent().apply {
            action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SECRET_CODE_ACTION
            } else {
                SECRET_CODE_ACTION_OLD
            }
            data = LSPOSED_SECRET_CODE.toUri()
        }

        withTimeoutOrNull(PRIVILEGED_START_TIMEOUT_MS) {
            componentOpsProvider.sendBroadcastPrivileged(config, intent)
        }

        return true
    }
}
