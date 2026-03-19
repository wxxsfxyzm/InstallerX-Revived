// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.activity

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rosan.installer.ui.common.auth.BiometricAuthBridge
import timber.log.Timber

class BiometricsAuthenticationActivity : FragmentActivity() {
    private var isResultSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("auth_title") ?: ""
        val subTitle = intent.getStringExtra("auth_subtitle") ?: ""

        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subTitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                sendResultAndFinish(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Timber.e("Biometric auth error ($errorCode): $errString")
                sendResultAndFinish(false)
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun sendResultAndFinish(success: Boolean) {
        if (!isResultSent) {
            isResultSent = true
            BiometricAuthBridge.authResultChannel.trySend(success)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendResultAndFinish(false)
    }
}
