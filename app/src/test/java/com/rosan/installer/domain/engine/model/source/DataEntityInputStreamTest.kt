// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class DataEntityInputStreamTest {
    @Test
    fun `descriptor analysis and installation can use separate streams`() {
        val analysisData = "analysis".encodeToByteArray()
        val installData = "install".encodeToByteArray()
        val file = Files.createTempFile("installerx-data-entity", ".bin")
        Files.write(file, analysisData)

        try {
            val entity = DataEntity.FileDescriptorEntity(
                path = "remote.apk",
                startOffset = 0L,
                length = analysisData.size.toLong(),
                channelFactory = {
                    FileChannel.open(file, StandardOpenOption.READ)
                },
                descriptorFactory = { error("descriptor is unused") },
                inputStreamFactory = { installData.inputStream() }
            )

            assertContentEquals(analysisData, entity.getInputStream().use { it.readBytes() })
            assertContentEquals(installData, entity.getInstallInputStream().use { it.readBytes() })
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun `descriptor subrange inherits pre-install analysis policies`() {
        val file = Files.createTempFile("installerx-policy", ".bin")
        Files.write(file, byteArrayOf(1, 2))

        try {
            val materializationKey = AnalysisMaterializationKey()
            val entity = DataEntity.FileDescriptorEntity(
                path = "remote.apk",
                startOffset = 0L,
                length = 2L,
                channelFactory = { FileChannel.open(file, StandardOpenOption.READ) },
                descriptorFactory = { error("descriptor is unused") },
                preInstallSignatureAnalysis = false,
                preInstallIdentityAnalysis = false,
                analysisMaterializationPolicy =
                    AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT,
                analysisMaterializationKey = materializationKey
            )

            val subrange = entity.subrange(relativeOffset = 0L, subrangeLength = 1L)
            assertFalse(subrange.preInstallSignatureAnalysis)
            assertFalse(subrange.preInstallIdentityAnalysis)
            assertEquals(
                AnalysisMaterializationPolicy.RETAINED_SOURCE_REPLACEMENT,
                subrange.analysisMaterializationPolicy
            )
            assertSame(materializationKey, subrange.analysisMaterializationKey)
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
