// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteFileNameTest {
    @Test
    fun `decodes spaces and punctuation without relying on URL query parameters`() {
        assertEquals(
            "TikTok - Videos, Shop & LIVE_47.6.3_APKPure.apk",
            decodeRemoteFileName("TikTok+-+Videos%2C+Shop+%26+LIVE_47.6.3_APKPure.apk"),
        )
    }

    @Test
    fun `decodes UTF-8 and percent encoded plus signs once`() {
        assertEquals("\u6d4b\u8bd5.apk", decodeRemoteFileName("%E6%B5%8B%E8%AF%95.apk"))
        assertEquals("C++ App.apk", decodeRemoteFileName("C%2B%2B+App.apk"))
        assertEquals("App%2BBeta.apk", decodeRemoteFileName("App%252BBeta.apk"))
    }

    @Test
    fun `treats literal plus signs as spaces like DownloadEngine`() {
        assertEquals("App Beta.apk", decodeRemoteFileName("App+Beta.apk"))
    }

    @Test
    fun `preserves filenames with invalid percent escapes`() {
        assertEquals("100%.apk", decodeRemoteFileName("100%.apk"))
        assertEquals("App%ZZ.apk", decodeRemoteFileName("App%ZZ.apk"))
    }

    @Test
    fun `keeps only the basename and removes control characters`() {
        assertEquals("app.apk", decodeRemoteFileName("../folder/app.apk"))
        assertEquals("app.apk", decodeRemoteFileName("folder\\app.apk"))
        assertEquals("badname.apk", decodeRemoteFileName("bad%0Aname.apk"))
        assertNull(decodeRemoteFileName(".."))
        assertNull(decodeRemoteFileName(""))
    }
}
