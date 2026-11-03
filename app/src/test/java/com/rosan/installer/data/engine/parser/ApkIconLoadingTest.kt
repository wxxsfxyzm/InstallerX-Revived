// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApkIconLoadingTest {
    @Test
    fun `disabled remote icons never read either drawable or open the source`() {
        assertNull(
            resolveApkIcon<String>(
                data = descriptor(loadIcon = false),
                loadPrimaryIcon = { error("Primary icon must not be read") },
                loadRoundIcon = { error("Round icon must not be read") },
            ),
        )
    }

    @Test
    fun `enabled remote icons load the primary without reading the round icon`() {
        assertEquals(
            "primary",
            resolveApkIcon(
                data = descriptor(loadIcon = true),
                loadPrimaryIcon = { "primary" },
                loadRoundIcon = { error("Round icon is unnecessary") },
            ),
        )
    }

    @Test
    fun `round icon is used when the primary is unavailable`() {
        val reads = mutableListOf<String>()
        assertEquals(
            "round",
            resolveApkIcon(
                data = descriptor(loadIcon = true),
                loadPrimaryIcon = {
                    reads.add("primary")
                    null
                },
                loadRoundIcon = {
                    reads.add("round")
                    "round"
                },
            ),
        )
        assertEquals(listOf("primary", "round"), reads)
    }

    @Test
    fun `local and fully cached APKs keep loading icons`() {
        val cached = DataEntity.FileEntity("cached.apk").apply { source = descriptor(loadIcon = false) }
        for (data in listOf(DataEntity.FileEntity("local.apk"), cached, descriptor())) {
            assertEquals("primary", resolveApkIcon(data, { "primary" }, { error("Unexpected fallback") }))
        }
    }

    @Test
    fun `nested descriptor views preserve disabled icon reads`() {
        val view = descriptor(loadIcon = false).subrange(0, 8).subrange(0, 4)
        assertNull(resolveApkIcon<String>(view, { error("Unexpected primary read") }, { error("Unexpected round read") }))
    }

    private fun descriptor(loadIcon: Boolean = true) = DataEntity.FileDescriptorEntity(
        path = "https://example.com/app.apk",
        startOffset = 0,
        length = 16,
        channelFactory = { error("No source reads expected") },
        descriptorFactory = { error("No file descriptor expected") },
        preInstallIconAnalysis = loadIcon,
    )
}
