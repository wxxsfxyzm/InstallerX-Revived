// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.config

import com.rosan.installer.domain.settings.model.preferences.NetworkAppNameSource
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
                platformSupported = true,
            ),
        )
        assertFalse(
            NetworkSourceMode.Cache.requiresRemoteSource(
                supportsRange = false,
                contentLength = -1L,
                platformSupported = true,
            ),
        )
    }

    @Test
    fun `automatic mode tries a seekable remote source when metadata is usable`() {
        assertTrue(NetworkSourceMode.Automatic.shouldTryRemoteSource(true, 100L, platformSupported = true))
        assertFalse(NetworkSourceMode.Automatic.shouldTryRemoteSource(false, 100L, platformSupported = true))
        assertFalse(NetworkSourceMode.Automatic.shouldTryRemoteSource(true, -1L, platformSupported = true))
        assertFalse(NetworkSourceMode.Automatic.shouldTryRemoteSource(true, 100L, platformSupported = false))
        assertFalse(NetworkSourceMode.Automatic.requiresRemoteSource(false, -1L, platformSupported = true))
    }

    @Test
    fun `stream only mode requires a seekable remote source`() {
        assertTrue(NetworkSourceMode.StreamOnly.shouldTryRemoteSource(true, 100L, platformSupported = true))
        assertTrue(NetworkSourceMode.StreamOnly.requiresRemoteSource(false, 100L, platformSupported = true))
        assertTrue(NetworkSourceMode.StreamOnly.requiresRemoteSource(true, -1L, platformSupported = true))
        assertTrue(NetworkSourceMode.StreamOnly.requiresRemoteSource(true, 100L, platformSupported = false))
        assertFalse(NetworkSourceMode.StreamOnly.requiresRemoteSource(true, 100L, platformSupported = true))
    }

    @Test
    fun `stream only ignores resource names and enabled icons`() {
        for (source in NetworkAppNameSource.entries) {
            assertEquals(NetworkAppNameSource.ServerFilename, NetworkSourceMode.StreamOnly.resolveAppNameSource(source))
        }
        assertFalse(NetworkSourceMode.StreamOnly.shouldLoadAppIcon(true))
        assertFalse(NetworkSourceMode.StreamOnly.shouldLoadAppIcon(false))
    }

    @Test
    fun `automatic honors independent name and icon choices while full download uses resources`() {
        for (source in NetworkAppNameSource.entries) {
            assertEquals(source, NetworkSourceMode.Automatic.resolveAppNameSource(source))
            assertEquals(NetworkAppNameSource.ApkResources, NetworkSourceMode.Cache.resolveAppNameSource(source))
        }
        for (loadIcon in listOf(false, true)) {
            assertEquals(loadIcon, NetworkSourceMode.Automatic.shouldLoadAppIcon(loadIcon))
            assertTrue(NetworkSourceMode.Cache.shouldLoadAppIcon(loadIcon))
        }
    }

    @Test
    fun `unknown persisted values preserve the cache default`() {
        assertEquals(NetworkSourceMode.Cache, NetworkSourceMode.fromValue("future_mode"))
        assertEquals(NetworkSourceMode.Automatic, NetworkSourceMode.fromValue("smart"))
        assertEquals(NetworkSourceMode.StreamOnly, NetworkSourceMode.fromValue("low_storage"))
    }
}
