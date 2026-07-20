// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.DataEntity
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonsZipFileProviderTest {
    private lateinit var tempDirectory: File
    private val provider = CommonsZipFileProvider()

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("commons-zip-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `reads entries and payloads through a seekable channel`() {
        val basePayload = validInnerApk("base")
        val splitPayload = validInnerApk("split")
        val archiveFile = File(tempDirectory, "valid.apks")
        ZipOutputStream(archiveFile.outputStream()).use { output ->
            output.writeEntry("base.apk", basePayload)
            output.writeEntry("split_config.en.apk", splitPayload)
        }

        provider.openMetadata(archiveFile).use { archive ->
            assertEquals(
                listOf("base.apk", "split_config.en.apk"),
                archive.entries.asSequence().map { it.name }.toList()
            )
            val baseEntry = requireNotNull(archive.getEntry("base.apk"))
            assertContentEquals(
                basePayload,
                provider.openEntry(archive, baseEntry).use { it.readBytes() }
            )
        }

        val entity = DataEntity.ZipFileEntity(
            name = "split_config.en.apk",
            parent = DataEntity.FileEntity(archiveFile.path)
        )
        assertContentEquals(
            splitPayload,
            requireNotNull(entity.getInputStream()).use { it.readBytes() }
        )
        assertEquals(splitPayload.size.toLong(), entity.getSize())
    }

    @Test
    fun `rejects optional ZIP compression methods with an analysis exception`() {
        listOf(ZSTANDARD_METHOD, XZ_METHOD).forEach { compressionMethod ->
            val entry = ZipArchiveEntry("base.apk").apply { method = compressionMethod }

            val error = assertFailsWith<AnalyseException> {
                provider.validateEntry(entry)
            }

            assertEquals(AnalyseErrorType.ALL_FILES_UNSUPPORTED, error.errorType)
            assertTrue(error.message.orEmpty().contains("compression method $compressionMethod"))
        }
    }

    @Test
    fun `local header reader recovers outer APK entries hidden by a nested central directory`() {
        val archiveFile = File(tempDirectory, "missing-outer-central-directory.apks")
        val output = ByteArrayOutputStream().apply {
            writeStoredLocalEntry("base.apk", validInnerApk("base"))
            writeStoredLocalEntry("split_config.en.apk", validInnerApk("split"))
        }
        archiveFile.writeBytes(output.toByteArray())

        val commonsEntries = runCatching {
            provider.open(archiveFile).use { archive ->
                archive.entries.asSequence().map { it.name }.toList()
            }
        }.getOrDefault(emptyList())
        assertFalse(commonsEntries.any { it.equals("base.apk", ignoreCase = true) })

        val recovered = SeekableZipReader().read(archiveFile)
        assertEquals(
            listOf("base.apk", "split_config.en.apk"),
            recovered.entries.map { it.name }
        )
        assertFalse(recovered.hasCentralDirectory)
    }

    private fun validInnerApk(marker: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { output ->
            output.writeEntry("AndroidManifest.xml", marker.toByteArray())
        }
    }.toByteArray()

    private fun ZipOutputStream.writeEntry(name: String, payload: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(payload)
        closeEntry()
    }

    private fun ByteArrayOutputStream.writeStoredLocalEntry(name: String, payload: ByteArray) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val crc = CRC32().apply { update(payload) }.value
        writeIntLittleEndian(LOCAL_FILE_HEADER_SIGNATURE)
        writeShortLittleEndian(10)
        writeShortLittleEndian(UTF8_FLAG)
        writeShortLittleEndian(ZipEntry.STORED)
        writeShortLittleEndian(0)
        writeShortLittleEndian(0)
        writeIntLittleEndian(crc)
        writeIntLittleEndian(payload.size.toLong())
        writeIntLittleEndian(payload.size.toLong())
        writeShortLittleEndian(nameBytes.size)
        writeShortLittleEndian(0)
        write(nameBytes)
        write(payload)
    }

    private fun ByteArrayOutputStream.writeShortLittleEndian(value: Int) {
        repeat(Short.SIZE_BYTES) { index -> write(value ushr (index * Byte.SIZE_BITS)) }
    }

    private fun ByteArrayOutputStream.writeIntLittleEndian(value: Long) {
        repeat(Int.SIZE_BYTES) { index -> write((value ushr (index * Byte.SIZE_BITS)).toInt()) }
    }

    private companion object {
        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50L
        const val UTF8_FLAG = 1 shl 11
        const val ZSTANDARD_METHOD = 93
        const val XZ_METHOD = 95
    }
}
