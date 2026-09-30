// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.rosan.installer.core.app.SecretCodeActions.SECRET_CODE_ACTION
import com.rosan.installer.core.app.SecretCodeActions.SECRET_CODE_ACTION_OLD
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class OpenLSPosedUseCase(
    private val componentOpsProvider: ComponentOpsProvider,
    private val capabilityProvider: DeviceCapabilityProvider,
) {
    private companion object {
        const val LSPOSED_SECRET_CODE = "android_secret_code://5776733"
        const val VECTOR_SECRET_CODE = "android_secret_code://832867"
    }

    /**
     * Attempts to open LSPosed or Vector via privileged secret-code broadcasts.
     * @return true if the action was attempted, false if skipped due to authorizer rules.
     */
    suspend operator fun invoke(config: ConfigModel): Boolean {
        if (!config.shouldAttemptPrivilegedStart(capabilityProvider.isSystemApp)) return false

        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SECRET_CODE_ACTION
        } else {
            SECRET_CODE_ACTION_OLD
        }

        fun createSecretCodeIntent(secretCode: String): Intent = Intent().apply {
            this.action = action
            data = secretCode.toUri()
        }

        withTimeoutOrNull(DEFAULT_PRIVILEGED_START_TIMEOUT_MS.milliseconds) {
            componentOpsProvider.sendBroadcastPrivileged(
                config,
                createSecretCodeIntent(LSPOSED_SECRET_CODE),
            )
        }

        delay(100)

        withTimeoutOrNull(DEFAULT_PRIVILEGED_START_TIMEOUT_MS.milliseconds) {
            componentOpsProvider.sendBroadcastPrivileged(
                config,
                createSecretCodeIntent(VECTOR_SECRET_CODE),
            )
        }

        return true
    }
}
