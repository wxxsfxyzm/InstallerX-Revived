// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.common.auth

import kotlinx.coroutines.channels.Channel

/**
 * Cross-component communication bridge. Completely replaces dangerous static callback variables.
 */
object BiometricAuthBridge {
    // Use CONFLATED to ensure only the latest result is kept, preventing pollution from old data.
    val authResultChannel = Channel<Boolean>(Channel.CONFLATED)

    @Volatile
    var isAuthenticating = false
}
