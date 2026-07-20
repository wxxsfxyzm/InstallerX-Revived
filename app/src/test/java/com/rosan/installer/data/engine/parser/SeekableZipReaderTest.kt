// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.RawDeflateInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeekableZipReaderTest {
    private lateinit var tempDirectory: File
    private val reader = SeekableZipReader()

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("seekable-zip-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `reads stored APKS entries without a central directory`() {
        val basePayload = "base payload".toByteArray()
        val splitPayload = "split payload".toByteArray()
        val file = writeArchive(
            entries = listOf(
                TestEntry("meta.sai_v2.json", "{}".toByteArray()),
                TestEntry("base.apk", basePayload),
                TestEntry("split_config.arm64_v8a.apk", splitPayload)
            ),
            includeCentralDirectoryMarker = false
        )

        val archive = reader.read(file)

        assertFalse(archive.hasCentralDirectory)
        assertEquals(
            listOf("meta.sai_v2.json", "base.apk", "split_config.arm64_v8a.apk"),
            archive.entries.map { it.name }
        )
        assertEquals(basePayload.size.toLong(), archive.entries[1].uncompressedSize)
        assertEquals(splitPayload.size.toLong(), archive.entries[2].compressedSize)
    }

    @Test
    fun `stops at a central directory marker`() {
        val file = writeArchive(
            entries = listOf(TestEntry("base.apk", "base".toByteArray())),
            includeCentralDirectoryMarker = true
        )

        val archive = reader.read(file)

        assertTrue(archive.hasCentralDirectory)
        assertEquals(listOf("base.apk"), archive.entries.map { it.name })
    }

    @Test
    fun `rejects entries that require a data descriptor`() {
        val file = writeArchive(
            entries = listOf(TestEntry("base.apk", "base".toByteArray(), flags = UTF8_FLAG or DATA_DESCRIPTOR_FLAG)),
            includeCentralDirectoryMarker = false
        )

        val error = assertFailsWith<SeekableZipException> { reader.read(file) }

        assertTrue(error.message.orEmpty().contains("data descriptor"))
    }

    @Test
    fun `rejects payload sizes outside the parent file`() {
        val output = ByteArrayOutputStream().apply {
            writeLocalHeader(
                name = "base.apk",
                flags = UTF8_FLAG,
                compressionMethod = ZipEntry.STORED,
                crc = 0,
                compressedSize = 100,
                uncompressedSize = 100
            )
            write(1)
        }
        val file = File(tempDirectory, "truncated.apks").apply { writeBytes(output.toByteArray()) }

        val error = assertFailsWith<SeekableZipException> { reader.read(file) }

        assertTrue(error.message.orEmpty().contains("payload exceeds file size"))
    }

    @Test
    fun `slice entity reads and verifies stored entry data`() {
        val payload = ByteArray(4096) { index -> (index % 251).toByte() }
        val file = writeArchive(
            entries = listOf(TestEntry("base.apk", payload)),
            includeCentralDirectoryMarker = false
        )
        val entry = reader.read(file).entries.single()
        val entity = entry.toDataEntity(file)

        val actual = entity.getInputStream().use { it.readBytes() }

        assertContentEquals(payload, actual)
        assertEquals(payload.size.toLong(), entity.getSize())
    }

    @Test
    fun `slice entity inflates raw deflate entry data`() {
        val payload = "deflated split payload".repeat(200).toByteArray()
        val file = writeArchive(
            entries = listOf(TestEntry("split_config.en.apk", payload, compressionMethod = ZipEntry.DEFLATED)),
            includeCentralDirectoryMarker = false
        )
        val entry = reader.read(file).entries.single()
        val entity = entry.toDataEntity(file)

        val actual = entity.getInputStream().use { it.readBytes() }

        assertContentEquals(payload, actual)
    }

    @Test
    fun `slice entity rejects unsupported ZIP compression with an analysis exception`() {
        val error = assertFailsWith<AnalyseException> {
            DataEntity.SeekableZipEntryEntity(
                name = "base.apk",
                parent = DataEntity.FileEntity(File(tempDirectory, "unused.apks").path),
                dataOffset = 0,
                compressedSize = 0,
                uncompressedSize = 0,
                compressionMethod = XZ_METHOD,
                crc = 0
            )
        }

        assertEquals(AnalyseErrorType.ALL_FILES_UNSUPPORTED, error.errorType)
    }

    @Test
    fun `raw deflate stream ends its inflater exactly once when closed`() {
        val inflater = TrackingInflater()
        val stream = RawDeflateInputStream(ByteArrayInputStream(ByteArray(0)), inflater)

        stream.close()
        stream.close()

        assertEquals(1, inflater.endCount)
    }

    @Test
    fun `slice entity rejects a CRC mismatch`() {
        val payload = "base".toByteArray()
        val file = writeArchive(
            entries = listOf(TestEntry("base.apk", payload, crcOverride = 0)),
            includeCentralDirectoryMarker = false
        )
        val entry = reader.read(file).entries.single()
        val entity = entry.toDataEntity(file)

        assertFailsWith<ZipException> {
            entity.getInputStream().use { it.readBytes() }
        }
    }

    private fun SeekableZipEntry.toDataEntity(file: File): DataEntity.SeekableZipEntryEntity =
        DataEntity.SeekableZipEntryEntity(
            name = name,
            parent = DataEntity.FileEntity(file.path),
            dataOffset = dataOffset,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            compressionMethod = compressionMethod,
            crc = crc
        )

    private fun writeArchive(
        entries: List<TestEntry>,
        includeCentralDirectoryMarker: Boolean
    ): File {
        val output = ByteArrayOutputStream()
        entries.forEach { entry ->
            val compressedPayload = when (entry.compressionMethod) {
                ZipEntry.STORED -> entry.payload
                ZipEntry.DEFLATED -> entry.payload.rawDeflate()
                else -> error("Unsupported test compression method")
            }
            val crc = entry.crcOverride ?: CRC32().apply { update(entry.payload) }.value
            output.writeLocalHeader(
                name = entry.name,
                flags = entry.flags,
                compressionMethod = entry.compressionMethod,
                crc = crc,
                compressedSize = compressedPayload.size.toLong(),
                uncompressedSize = entry.payload.size.toLong()
            )
            output.write(compressedPayload)
        }
        if (includeCentralDirectoryMarker) output.writeIntLittleEndian(CENTRAL_DIRECTORY_SIGNATURE)

        return File(tempDirectory, "fixture-${System.nanoTime()}.apks").apply {
            writeBytes(output.toByteArray())
        }
    }

    private fun ByteArrayOutputStream.writeLocalHeader(
        name: String,
        flags: Int,
        compressionMethod: Int,
        crc: Long,
        compressedSize: Long,
        uncompressedSize: Long
    ) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        writeIntLittleEndian(LOCAL_FILE_HEADER_SIGNATURE)
        writeShortLittleEndian(10)
        writeShortLittleEndian(flags)
        writeShortLittleEndian(compressionMethod)
        writeShortLittleEndian(0)
        writeShortLittleEndian(0)
        writeIntLittleEndian(crc)
        writeIntLittleEndian(compressedSize)
        writeIntLittleEndian(uncompressedSize)
        writeShortLittleEndian(nameBytes.size)
        writeShortLittleEndian(0)
        write(nameBytes)
    }

    private fun ByteArrayOutputStream.writeShortLittleEndian(value: Int) {
        repeat(Short.SIZE_BYTES) { index -> write(value ushr (index * Byte.SIZE_BITS)) }
    }

    private fun ByteArrayOutputStream.writeIntLittleEndian(value: Long) {
        repeat(Int.SIZE_BYTES) { index -> write((value ushr (index * Byte.SIZE_BITS)).toInt()) }
    }

    private fun ByteArray.rawDeflate(): ByteArray {
        val output = ByteArrayOutputStream()
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        DeflaterOutputStream(output, deflater).use { it.write(this) }
        return output.toByteArray()
    }

    private data class TestEntry(
        val name: String,
        val payload: ByteArray,
        val compressionMethod: Int = ZipEntry.STORED,
        val flags: Int = UTF8_FLAG,
        val crcOverride: Long? = null
    )

    private class TrackingInflater : Inflater(true) {
        var endCount = 0
            private set

        override fun end() {
            endCount++
            super.end()
        }
    }

    private companion object {
        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50L
        const val CENTRAL_DIRECTORY_SIGNATURE = 0x02014B50L
        const val UTF8_FLAG = 1 shl 11
        const val DATA_DESCRIPTOR_FLAG = 1 shl 3
        const val XZ_METHOD = 95
    }
}
