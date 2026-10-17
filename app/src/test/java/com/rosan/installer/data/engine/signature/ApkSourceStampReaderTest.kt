// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.data.engine.parser.CommonsZipFileProvider
import com.rosan.installer.data.engine.parser.SeekableZipReader
import com.rosan.installer.data.engine.parser.UnifiedZipFileProvider
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampVerificationStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApkSourceStampReaderTest {
    private lateinit var tempDirectory: File
    private val reader = ApkSourceStampReader(CommonsZipFileProvider(), CertificateFormatter())
    private val digest = ByteArray(32) { it.toByte() }
    private val digestHex = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("source-stamp-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `stored and deflated stamp entries are displayed as their existing digest`() {
        for (method in listOf(ZipEntry.STORED, ZipEntry.DEFLATED)) {
            val apk = createApk(digest, method)

            assertEquals(digestHex, reader.read(DataEntity.FileEntity(apk.path)))
        }
    }

    @Test
    fun `missing or malformed stamp entries have no digest`() {
        for (payload in listOf(null, ByteArray(0), ByteArray(31), ByteArray(33))) {
            val apk = createApk(payload)

            assertNull(reader.read(DataEntity.FileEntity(apk.path)))
        }
    }

    @Test
    fun `descriptor stamp reads use the channel without opening the display path`() {
        val apk = createApk(digest)
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("Stamp reads must use the seekable channel") },
        )

        assertEquals(digestHex, reader.read(entity))
    }

    @Test
    fun `unreadable ZIP metadata does not fail signature analysis`() {
        val apk = File(tempDirectory, "invalid.apk").apply { writeText("invalid ZIP") }

        assertNull(reader.read(DataEntity.FileEntity(apk.path)))
    }

    @Test
    fun `borrowed ZIP metadata is reused without reopening the source`() {
        val apk = createApk(digest)
        var openCount = 0
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = {
                check(++openCount == 1) { "The source must not be reopened for its stamp entry" }
                FileChannel.open(apk.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("Unused") },
        )
        UnifiedZipFileProvider(CommonsZipFileProvider(), SeekableZipReader()).open(entity).use { archive ->
            assertEquals(digestHex, reader.read(entity, archive))
            // The caller still owns the archive after the stamp read.
            archive.openEntry(requireNotNull(archive.getEntry("AndroidManifest.xml"))).use {
                assertEquals("manifest", it.bufferedReader().readText())
            }
        }
        assertEquals(1, openCount)
    }

    @Test
    fun `stamp declarations retain certificate and timestamp without claiming verification`() {
        val certificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x02)
        val timestamp = 1790571492L
        val attribute = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0xe43c5946.toInt()).putLong(timestamp).array()
        val attributes = lengthPrefixed(lengthPrefixed(attribute))
        val data = lengthPrefixed(certificate) + lengthPrefixed(byteArrayOf()) +
            lengthPrefixed(attributes) + lengthPrefixed(byteArrayOf())
        val block = ByteBuffer.wrap(lengthPrefixed(data)).order(ByteOrder.LITTLE_ENDIAN)

        val stamp = reader.readDeclaration(block)

        assertEquals(SourceStampVerificationStatus.UNVERIFIED, stamp.verificationStatus)
        assertEquals(CertificateFormatter().format(certificate).sha256, stamp.certificate?.sha256)
        assertEquals(timestamp, stamp.timestampEpochSeconds)
        assertEquals(emptyList(), stamp.warnings)
    }

    @Test
    fun `truncated stamp declarations preserve partial metadata as unverified`() {
        val certificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x02)
        val block = ByteBuffer.wrap(lengthPrefixed(lengthPrefixed(certificate)))
            .order(ByteOrder.LITTLE_ENDIAN)

        val stamp = reader.readDeclaration(block)

        assertEquals(SourceStampVerificationStatus.UNVERIFIED, stamp.verificationStatus)
        assertEquals(CertificateFormatter().format(certificate).sha256, stamp.certificate?.sha256)
        assertTrue(stamp.warnings.isNotEmpty())
    }

    private fun lengthPrefixed(bytes: ByteArray): ByteArray = ByteBuffer.allocate(4 + bytes.size)
        .order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.size).put(bytes).array()

    private fun createApk(stamp: ByteArray?, method: Int = ZipEntry.DEFLATED): File {
        val apk = File.createTempFile("stamp", ".apk", tempDirectory)
        ZipOutputStream(apk.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("AndroidManifest.xml"))
            output.write("manifest".toByteArray())
            output.closeEntry()
            if (stamp != null) {
                val entry = ZipEntry("stamp-cert-sha256").apply {
                    this.method = method
                    if (method == ZipEntry.STORED) {
                        size = stamp.size.toLong()
                        compressedSize = size
                        crc = CRC32().apply { update(stamp) }.value
                    }
                }
                output.putNextEntry(entry)
                output.write(stamp)
                output.closeEntry()
            }
        }
        return apk
    }
}
