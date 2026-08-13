// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.util

import kotlin.test.Test
import kotlin.test.assertEquals

class SourcePathUtilTest {
    @Test
    fun `material files SMB path is decoded once for display`() {
        val encoded =
            "/smb%3A%2F%2FAdministrator%40192.168.31.3%2Fshare%2Fapp-Preview-debug.apk"

        assertEquals(
            "smb://Administrator@192.168.31.3/share/app-Preview-debug.apk",
            encoded.decodeSmbProviderPath()
        )
    }

    @Test
    fun `literal plus signs in SMB path are preserved`() {
        assertEquals(
            "smb://server/My+Apps/app.apk",
            "/smb%3A%2F%2Fserver%2FMy+Apps%2Fapp.apk".decodeSmbProviderPath()
        )
    }

    @Test
    fun `non SMB provider path is not decoded`() {
        assertEquals(
            "/primary/My%20Apps/app.apk",
            "/primary/My%20Apps/app.apk".decodeSmbProviderPath()
        )
    }
}
