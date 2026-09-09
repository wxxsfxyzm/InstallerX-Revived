// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class InstalledModuleInfoProviderImplTest {
    @Test
    fun `parses multiple Magisk module properties without trailing newlines`() {
        val raw = "\u001e" +
            "id=first\nname=First Module\nversion=1.0\nversionCode=10" +
            "\u001e" +
            "id=second\nname=Second Module\nversion=2.0\nversionCode=20"

        val modules = InstalledModuleInfoProviderImpl.parseMagiskModuleList(raw)

        assertEquals(2, modules.size)
        assertEquals("first", modules[0].id)
        assertEquals("1.0", modules[0].version)
        assertEquals(10L, modules[0].versionCode)
        assertEquals("second", modules[1].id)
        assertEquals(20L, modules[1].versionCode)
    }

    @Test
    fun `handles BOM and skips malformed Magisk module properties`() {
        val raw = "\u001e" +
            "\uFEFFid=valid\nname=Valid\nversionCode=not-a-number\nupdateJson=https\\://example.com/update.json" +
            "\u001e" +
            "name=Missing ID"

        val modules = InstalledModuleInfoProviderImpl.parseMagiskModuleList(raw)

        assertEquals(1, modules.size)
        assertEquals("valid", modules.single().id)
        assertEquals(null, modules.single().versionCode)
        assertEquals("https://example.com/update.json", modules.single().updateJson)
    }
}
