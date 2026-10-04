// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.util.zip.CRC32
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.MemoryLimitException
import org.tukaani.xz.XZOutputStream

class XzMemoryLimitTest {
    private lateinit var tempDirectory: File
    private val payload = "id=test\nname=Test\n".toByteArray()
    private val provider = CommonsZipFileProvider()

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("xz-memory-limit-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `Commons entry reads reject a 96 MiB dictionary`() {
        assertCommonsLimit(dictionaryProperty = 29)
    }

    @Test
    fun `file entity reads reject a 96 MiB dictionary and close the archive`() {
        assertFileEntityLimit(dictionaryProperty = 29)
    }

    @Test
    fun `slice entity reads reject a 96 MiB dictionary and close the channel`() {
        assertSliceLimit(dictionaryProperty = 29)
    }

    @Test
    fun `tiny ZIP entries declaring a 1 GiB dictionary fail with a memory limit exception`() {
        assertCommonsLimit(dictionaryProperty = 36)
        assertFileEntityLimit(dictionaryProperty = 36)
        assertSliceLimit(dictionaryProperty = 36)
    }

    @Test
    fun `ordinary XZ entries still decode through every entry path`() {
        val fixture = createFixture(dictionaryProperty = null)
        provider.openMetadata(fixture.source).use { archive ->
            assertContentEquals(
                payload,
                provider.openEntry(archive, requireNotNull(archive.getEntry("module.prop"))).use { it.readBytes() },
            )
        }
        assertContentEquals(
            payload,
            requireNotNull(DataEntity.ZipFileEntity("module.prop", fixture.source).getInputStream()).use { it.readBytes() },
        )
        assertContentEquals(payload, fixture.slice().getInputStream().use { it.readBytes() })
        assertTrue(fixture.source.channels.all { !it.isOpen })
    }

    @Test
    fun `raw XZ decoding still rejects encrypted ZIP entries`() {
        val fixture = createFixture(dictionaryProperty = null, encrypted = true)
        provider.openMetadata(fixture.source).use { archive ->
            val error = assertFailsWith<UnsupportedZipFeatureException> {
                provider.openEntry(archive, requireNotNull(archive.getEntry("module.prop"))).close()
            }
            assertEquals(UnsupportedZipFeatureException.Feature.ENCRYPTION, error.feature)
        }
        assertFailsWith<UnsupportedZipFeatureException> {
            DataEntity.ZipFileEntity("module.prop", fixture.source).getInputStream()?.close()
        }
        assertTrue(fixture.source.channels.all { !it.isOpen })
    }

    private fun assertCommonsLimit(dictionaryProperty: Int) {
        val fixture = createFixture(dictionaryProperty)
        provider.openMetadata(fixture.source).use { archive ->
            val entry = requireNotNull(archive.getEntry("module.prop"))
            val error = assertFailsWith<MemoryLimitException> {
                provider.openEntry(archive, entry).use { it.readBytes() }
            }
            assertLimit(error)
        }
        assertTrue(fixture.source.channels.all { !it.isOpen })
    }

    private fun assertFileEntityLimit(dictionaryProperty: Int) {
        val fixture = createFixture(dictionaryProperty)
        val entity = DataEntity.ZipFileEntity("module.prop", fixture.source)
        val error = assertFailsWith<MemoryLimitException> {
            requireNotNull(entity.getInputStream()).use { it.readBytes() }
        }
        assertLimit(error)
        assertFalse(fixture.source.channels.single().isOpen)
    }

    private fun assertSliceLimit(dictionaryProperty: Int) {
        val fixture = createFixture(dictionaryProperty)
        val error = assertFailsWith<MemoryLimitException> {
            fixture.slice().getInputStream().use { it.readBytes() }
        }
        assertLimit(error)
        assertFalse(fixture.source.channels.single().isOpen)
    }

    private fun assertLimit(error: MemoryLimitException) {
        assertEquals(64 * 1024, error.memoryLimit)
        assertTrue(error.memoryNeeded > error.memoryLimit)
    }

    private fun createFixture(dictionaryProperty: Int?, encrypted: Boolean = false): Fixture {
        val compressed = ByteArrayOutputStream().also { output ->
            XZOutputStream(output, LZMA2Options(1)).use { it.write(payload) }
        }.toByteArray()
        if (dictionaryProperty != null) {
            // Change only the LZMA2 dictionary declaration, without allocating that dictionary
            // in the fixture encoder. Recompute the XZ Block Header CRC to keep the input valid.
            val headerOffset = 12
            val headerSize = ((compressed[headerOffset].toInt() and 0xFF) + 1) * 4
            assertEquals(0x21, compressed[headerOffset + 2].toInt() and 0xFF)
            assertEquals(1, compressed[headerOffset + 3].toInt() and 0xFF)
            compressed[headerOffset + 4] = dictionaryProperty.toByte()
            val headerCrc = CRC32().apply { update(compressed, headerOffset, headerSize - 4) }.value
            repeat(4) { index ->
                compressed[headerOffset + headerSize - 4 + index] = (headerCrc ushr (8 * index)).toByte()
            }
        }

        val bytes = ByteArrayOutputStream()
        val crc = CRC32().apply { update(payload) }.value
        var centralDirectoryOffset: Int
        ZipArchiveOutputStream(bytes).use { output ->
            val entry = ZipArchiveEntry("module.prop").apply {
                method = 95
                size = payload.size.toLong()
                compressedSize = compressed.size.toLong()
                this.crc = crc
            }
            compressed.inputStream().use { output.addRawArchiveEntry(entry, it) }
            centralDirectoryOffset = bytes.size()
            output.finish()
        }
        assertTrue(bytes.size() < 256, "The oversized dictionary must fit in a tiny ZIP")
        val zipBytes = bytes.toByteArray()
        if (encrypted) {
            zipBytes[6] = (zipBytes[6].toInt() or 1).toByte()
            zipBytes[centralDirectoryOffset + 8] = (zipBytes[centralDirectoryOffset + 8].toInt() or 1).toByte()
        }
        val dataOffset = 30L + zipBytes.unsignedShortAt(26) + zipBytes.unsignedShortAt(28)
        val file = File(tempDirectory, "fixture-${System.nanoTime()}.zip").apply { writeBytes(zipBytes) }
        return Fixture(TrackingFileEntity(file.path), dataOffset, compressed.size.toLong(), crc)
    }

    private fun ByteArray.unsignedShortAt(offset: Int): Int = (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private inner class Fixture(val source: TrackingFileEntity, val dataOffset: Long, val compressedSize: Long, val crc: Long) {
        fun slice() = DataEntity.SeekableZipEntryEntity(
            name = "module.prop",
            parent = source,
            dataOffset = dataOffset,
            compressedSize = compressedSize,
            uncompressedSize = payload.size.toLong(),
            compressionMethod = 95,
            crc = crc,
        )
    }

    private class TrackingFileEntity(path: String) : DataEntity.FileEntity(path) {
        val channels = mutableListOf<SeekableByteChannel>()

        override fun openChannel(): SeekableByteChannel = super.openChannel().also { channels += it }
    }
}
