// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.handler

import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionConfirmationBiometricPolicyTest {
    @Test
    fun `manual approval of external session requires biometric auth when enabled`() {
        assertTrue(requiresAuth(globalMode = BiometricAuthMode.Enable, configRequireBiometricAuth = false))
        assertTrue(requiresAuth(globalMode = BiometricAuthMode.FollowConfig, configRequireBiometricAuth = true))
    }

    @Test
    fun `denial auto approval self session and disabled modes skip biometric auth`() {
        assertFalse(requiresAuth(granted = false))
        assertFalse(requiresAuth(triggerAuth = false))
        assertFalse(requiresAuth(isSelfSession = true))
        assertFalse(requiresAuth(globalMode = BiometricAuthMode.Disable))
        assertFalse(requiresAuth(globalMode = BiometricAuthMode.FollowConfig, configRequireBiometricAuth = false))
    }

    private fun requiresAuth(
        granted: Boolean = true,
        triggerAuth: Boolean = true,
        isSelfSession: Boolean = false,
        globalMode: BiometricAuthMode = BiometricAuthMode.Enable,
        configRequireBiometricAuth: Boolean = true,
    ): Boolean = shouldRequireSessionConfirmationBiometricAuth(
        granted = granted,
        triggerAuth = triggerAuth,
        isSelfSession = isSelfSession,
        globalMode = globalMode,
        configRequireBiometricAuth = configRequireBiometricAuth,
    )
}
