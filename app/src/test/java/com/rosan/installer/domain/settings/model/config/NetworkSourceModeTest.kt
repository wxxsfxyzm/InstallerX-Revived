// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkSourceModeTest {
    @Test
    fun `cache mode always uses the full-download path`() {
        assertFalse(
            NetworkSourceMode.Cache.shouldTryRemoteSource(
                supportsRange = true,
                contentLength = 100L,
                platformSupported = true
            )
        )
        assertFalse(
            NetworkSourceMode.Cache.requiresRemoteSource(
                supportsRange = false,
                contentLength = -1L,
                platformSupported = true
            )
        )
    }

    @Test
    fun `smart mode tries a seekable remote source when metadata is usable`() {
        assertTrue(NetworkSourceMode.Smart.shouldTryRemoteSource(true, 100L, platformSupported = true))
        assertFalse(NetworkSourceMode.Smart.shouldTryRemoteSource(false, 100L, platformSupported = true))
        assertFalse(NetworkSourceMode.Smart.shouldTryRemoteSource(true, -1L, platformSupported = true))
        assertFalse(NetworkSourceMode.Smart.shouldTryRemoteSource(true, 100L, platformSupported = false))
        assertFalse(NetworkSourceMode.Smart.requiresRemoteSource(false, -1L, platformSupported = true))
    }

    @Test
    fun `low-storage mode requires a seekable remote source`() {
        assertTrue(NetworkSourceMode.LowStorage.shouldTryRemoteSource(true, 100L, platformSupported = true))
        assertTrue(NetworkSourceMode.LowStorage.requiresRemoteSource(false, 100L, platformSupported = true))
        assertTrue(NetworkSourceMode.LowStorage.requiresRemoteSource(true, -1L, platformSupported = true))
        assertTrue(NetworkSourceMode.LowStorage.requiresRemoteSource(true, 100L, platformSupported = false))
        assertFalse(NetworkSourceMode.LowStorage.requiresRemoteSource(true, 100L, platformSupported = true))
    }

    @Test
    fun `unknown persisted values preserve the cache default`() {
        assertEquals(NetworkSourceMode.Cache, NetworkSourceMode.fromValue("future_mode"))
        assertEquals(NetworkSourceMode.Smart, NetworkSourceMode.fromValue("smart"))
        assertEquals(NetworkSourceMode.LowStorage, NetworkSourceMode.fromValue("low_storage"))
    }
}
