// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleSourceMaterializerTest {
    private lateinit var tempDirectory: File
    private val materializer = ModuleSourceMaterializer()

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("module-materializer-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `materializes a descriptor-backed module to a real local file`() {
        val payload = "descriptor-backed-module".repeat(64).toByteArray()
        val backingFile = File(tempDirectory, "provider-backing.zip").apply { writeBytes(payload) }
        val displayPath = "/smb%3A%2F%2Fserver%2Fmodules%2Ftest.zip"
        val sourceIdentity = DataEntity.FileEntity(displayPath)
        val descriptor = DataEntity.FileDescriptorEntity(
            path = displayPath,
            startOffset = 0L,
            length = payload.size.toLong(),
            channelFactory = {
                FileChannel.open(backingFile.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("Materialization must use the seekable stream") },
        ).apply {
            source = sourceIdentity
        }
        val sessionCache = File(tempDirectory, "session-cache")

        val result = materializer.materializeForInstall(descriptor, sessionCache.path)

        assertNotSame(descriptor, result)
        assertFalse(result is DataEntity.FileDescriptorEntity)
        assertTrue(File(result.path).isFile)
        assertTrue(File(result.path).absolutePath.startsWith(sessionCache.absolutePath))
        assertContentEquals(payload, File(result.path).readBytes())
        assertEquals(displayPath, assertIs<DataEntity.FileEntity>(result.getSourceTop()).path)
    }

    @Test
    fun `keeps an existing local module file unchanged`() {
        val file = File(tempDirectory, "local-module.zip").apply { writeText("module") }
        val data = DataEntity.FileEntity(file.absolutePath)

        val result = materializer.materializeForInstall(
            data = data,
            cacheDirectory = File(tempDirectory, "unused-cache").path,
        )

        assertSame(data, result)
    }
}
