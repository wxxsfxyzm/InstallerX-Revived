// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import android.content.Intent
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.privileged.provider.ComponentOpsProvider
import com.rosan.installer.domain.settings.model.config.ConfigModel
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class OpenAppUseCase(
    private val componentOpsProvider: ComponentOpsProvider,
    private val capabilityProvider: DeviceCapabilityProvider
) {
    companion object {
        const val PRIVILEGED_START_TIMEOUT_MS = DEFAULT_PRIVILEGED_START_TIMEOUT_MS
        private const val TAG = "OpenAppUseCase"
    }

    sealed interface Result {
        data object SuccessPrivileged : Result
        data class FallbackRequired(val reason: String) : Result
    }

    /**
     * Orchestrates the logic to open an app.
     * Determines whether to use privileged execution or fallback to standard intent.
     *
     * @param config The installer configuration.
     * @param launchIntent The intent to launch the app (provided by the caller to avoid Context dependency here).
     * @return The result of the operation, instructing the UI layer on what to do next.
     */
    suspend operator fun invoke(config: ConfigModel, launchIntent: Intent): Result {
        if (!config.shouldAttemptPrivilegedStart(capabilityProvider.isSystemApp)) {
            Timber.tag(TAG).i("Privileged start skipped based on authorizer rules.")
            return Result.FallbackRequired("Skipped")
        }

        Timber.tag(TAG).i("Attempting privileged API start...")

        val timeoutResult = withTimeoutOrNull(PRIVILEGED_START_TIMEOUT_MS.milliseconds) {
            componentOpsProvider.startActivityPrivileged(config, launchIntent)
        }

        return if (timeoutResult == null) {
            Timber.tag(TAG).w("Privileged API start timed out.")
            Result.FallbackRequired("Timeout")
        } else if (timeoutResult) {
            Timber.tag(TAG).i("Privileged API start succeeded.")
            Result.SuccessPrivileged
        } else {
            Timber.tag(TAG).w("Privileged API start failed.")
            Result.FallbackRequired("Failed")
        }
    }
}
