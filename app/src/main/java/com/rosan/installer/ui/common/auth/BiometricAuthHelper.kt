// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.common.auth

import android.content.Context
import android.content.Intent
import com.rosan.installer.domain.engine.exception.AuthenticationFailedException
import com.rosan.installer.ui.activity.BiometricsAuthenticationActivity

/**
 * Safely initiates biometric authentication (non-throwing version).
 */
suspend fun Context.safeBiometricAuth(title: String, subTitle: String): Boolean {
    // Clear the single legacy data point from the conflated channel
    BiometricAuthBridge.authResultChannel.tryReceive()

    val intent = Intent(this, BiometricsAuthenticationActivity::class.java).apply {
        putExtra("auth_title", title)
        putExtra("auth_subtitle", subTitle)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Ensure multiple transparent Activities are not launched.
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    BiometricAuthBridge.isAuthenticating = true
    this.startActivity(intent)

    // The coroutine will safely suspend here until the transparent Activity sends the result.
    return try {
        // Suspend and wait for the result.
        BiometricAuthBridge.authResultChannel.receive()
    } finally {
        // 💡 Ensure the state is reset regardless of success, failure, or coroutine cancellation.
        BiometricAuthBridge.isAuthenticating = false
    }
}

/**
 * Safely initiates biometric authentication (throws exception on failure).
 */
suspend fun Context.safeBiometricAuthOrThrow(title: String, subTitle: String) {
    val success = safeBiometricAuth(title, subTitle)
    if (!success) {
        throw AuthenticationFailedException("Biometric authentication failed or was cancelled.")
    }
}
