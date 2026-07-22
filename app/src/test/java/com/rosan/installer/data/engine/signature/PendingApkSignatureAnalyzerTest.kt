// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingApkSignatureAnalyzerTest {
    private lateinit var tempDirectory: File
    private val analyzer = PendingApkSignatureAnalyzer(CertificateFormatter())

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("pending-signature-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `descriptor-backed verification does not open its display path`() {
        val backingFile = File(tempDirectory, "backing.apk")
        ZipOutputStream(backingFile.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("AndroidManifest.xml"))
            output.write("manifest".toByteArray())
            output.closeEntry()
        }

        var channelOpenCount = 0
        var rawDescriptorRequested = false
        val displayPath = File(tempDirectory, "missing/source.apk").path
        val source = DataEntity.FileDescriptorEntity(
            path = displayPath,
            startOffset = 0L,
            length = backingFile.length(),
            channelFactory = {
                channelOpenCount++
                FileChannel.open(backingFile.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = {
                rawDescriptorRequested = true
                error("Signature verification must use the seekable channel")
            }
        )

        val fileResult = analyzer.analyze(backingFile.path)
        val descriptorResult = requireNotNull(analyzer.analyze(source, tempDirectory.path))

        assertFalse(File(displayPath).exists())
        assertTrue(channelOpenCount > 0)
        assertFalse(rawDescriptorRequested)
        assertEquals(fileResult.verified, descriptorResult.verified)
        assertEquals(fileResult.signerSha256Set, descriptorResult.signerSha256Set)
        assertEquals(fileResult.verifiedSchemes, descriptorResult.verifiedSchemes)
        assertEquals(fileResult.errors, descriptorResult.errors)
    }
}
