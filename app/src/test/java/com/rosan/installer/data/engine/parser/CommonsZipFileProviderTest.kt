// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream

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
                archive.entries.asSequence().map { it.name }.toList(),
            )
            val baseEntry = requireNotNull(archive.getEntry("base.apk"))
            assertContentEquals(
                basePayload,
                provider.openEntry(archive, baseEntry).use { it.readBytes() },
            )
        }

        val entity = DataEntity.ZipFileEntity(
            name = "split_config.en.apk",
            parent = DataEntity.FileEntity(archiveFile.path),
        )
        assertContentEquals(
            splitPayload,
            requireNotNull(entity.getInputStream()).use { it.readBytes() },
        )
        assertEquals(splitPayload.size.toLong(), entity.getSize())
    }

    @Test
    fun `reading an entry while opening another preserves both payloads`() {
        assertConcurrentEntryAccess(resolveRange = false)
    }

    @Test
    fun `reading an entry while resolving another data range preserves both payloads`() {
        assertConcurrentEntryAccess(resolveRange = true)
    }

    @Test
    fun `many mixed compression entries retain their contents during concurrent reads`() {
        for (entryCount in listOf(3, 16, 64)) {
            val payloads = List(entryCount) { index ->
                ByteArray(65536 + index * 31).also { java.util.Random(index.toLong()).nextBytes(it) }
            }
            val names = List(entryCount) { index ->
                when (index) {
                    0 -> "module.prop"
                    1 -> "launcher.png"
                    else -> "app-$index.apk"
                }
            }
            val file = File(tempDirectory, "parallel-$entryCount.zip")
            ZipOutputStream(file.outputStream()).use { output ->
                payloads.forEachIndexed { index, payload ->
                    val entry = ZipEntry(names[index])
                    if (index % 2 == 0) {
                        entry.method = ZipEntry.STORED
                        entry.size = payload.size.toLong()
                        entry.compressedSize = payload.size.toLong()
                        entry.crc = CRC32().apply { update(payload) }.value
                    }
                    output.putNextEntry(entry)
                    output.write(payload)
                    output.closeEntry()
                }
            }

            val source = object : DataEntity.FileEntity(file.path) {
                override fun openChannel(): SeekableByteChannel {
                    val delegate = FileChannel.open(file.toPath(), StandardOpenOption.READ)
                    return object : SeekableByteChannel by delegate {
                        override fun position(newPosition: Long): SeekableByteChannel {
                            delegate.position(newPosition)
                            Thread.yield()
                            return this
                        }
                    }
                }
            }

            // Fresh metadata views retain unresolved headers in every round.
            repeat(3) {
                provider.openMetadata(source).use { archive ->
                    val workers = minOf(entryCount, 16)
                    val ready = CountDownLatch(workers)
                    val start = CountDownLatch(1)
                    val executor = Executors.newFixedThreadPool(workers)
                    try {
                        val results = names.mapIndexed { index, name ->
                            executor.submit<ByteArray> {
                                ready.countDown()
                                check(start.await(5, TimeUnit.SECONDS))
                                val entry = requireNotNull(archive.getEntry(name))
                                if (index % 2 == 0) {
                                    assertEquals(payloads[index].size.toLong(), provider.resolveStoredDataRange(archive, entry)?.length)
                                } else {
                                    assertEquals(entry.compressedSize, provider.resolveDataRange(archive, entry)?.length)
                                }
                                provider.openEntry(archive, entry).use { input ->
                                    ByteArrayOutputStream().also { output ->
                                        input.copyTo(output, bufferSize = 257 + index * 7)
                                    }.toByteArray()
                                }
                            }
                        }
                        assertTrue(ready.await(5, TimeUnit.SECONDS))
                        start.countDown()
                        results.forEachIndexed { index, result ->
                            assertContentEquals(payloads[index], result.get(10, TimeUnit.SECONDS), "entries=$entryCount, name=${names[index]}")
                        }
                    } finally {
                        start.countDown()
                        executor.shutdownNow()
                        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
                    }
                }
            }
        }
    }

    private fun assertConcurrentEntryAccess(resolveRange: Boolean) {
        val daemon = ByteArray(65536).also { java.util.Random(1).nextBytes(it) }
        val manager = ByteArray(65536).also { java.util.Random(2).nextBytes(it) }
        val file = File(tempDirectory, "parallel.zip")
        ZipOutputStream(file.outputStream()).use { output ->
            output.writeEntry("daemon.apk", daemon)
            output.writeEntry("manager.apk", manager)
        }

        val pauseNextRead = AtomicBoolean(false)
        val payloadReadStarted = CountDownLatch(1)
        val resumePayloadRead = CountDownLatch(1)
        val metadataStarted = CountDownLatch(1)
        val metadataSeekDuringRead = CountDownLatch(1)
        val source = object : DataEntity.FileEntity(file.path) {
            override fun openChannel(): SeekableByteChannel {
                val delegate = FileChannel.open(file.toPath(), StandardOpenOption.READ)
                // Content-provider and HTTP sources expose a generic seekable channel, not a
                // FileChannel. Exercise Commons' seek + read path used for those sources.
                return object : SeekableByteChannel by delegate {
                    override fun position(newPosition: Long): SeekableByteChannel {
                        if (payloadReadStarted.count == 0L && resumePayloadRead.count != 0L) {
                            metadataSeekDuringRead.countDown()
                        }
                        delegate.position(newPosition)
                        return this
                    }

                    override fun read(dst: ByteBuffer): Int {
                        if (pauseNextRead.compareAndSet(true, false)) {
                            payloadReadStarted.countDown()
                            check(resumePayloadRead.await(5, TimeUnit.SECONDS))
                        }
                        return delegate.read(dst)
                    }
                }
            }
        }

        provider.openMetadata(source).use { archive ->
            val daemonEntry = requireNotNull(archive.getEntry("daemon.apk"))
            val managerEntry = requireNotNull(archive.getEntry("manager.apk"))
            provider.openEntry(archive, daemonEntry).use { daemonStream ->
                val executor = Executors.newFixedThreadPool(2)
                try {
                    pauseNextRead.set(true)
                    val daemonResult = executor.submit<ByteArray> { daemonStream.readBytes() }
                    assertTrue(payloadReadStarted.await(5, TimeUnit.SECONDS))
                    val managerResult = executor.submit<ByteArray> {
                        metadataStarted.countDown()
                        if (resolveRange) {
                            assertEquals(managerEntry.compressedSize, provider.resolveDataRange(archive, managerEntry)?.length)
                        }
                        provider.openEntry(archive, managerEntry).use { it.readBytes() }
                    }
                    assertTrue(metadataStarted.await(5, TimeUnit.SECONDS))
                    // Give lazy header resolution an opportunity to seek while the payload read
                    // is paused. Correct synchronization blocks that seek until we release it.
                    metadataSeekDuringRead.await(200, TimeUnit.MILLISECONDS)
                    resumePayloadRead.countDown()
                    assertContentEquals(daemon, daemonResult.get(5, TimeUnit.SECONDS))
                    assertContentEquals(manager, managerResult.get(5, TimeUnit.SECONDS))
                } finally {
                    resumePayloadRead.countDown()
                    executor.shutdownNow()
                    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
                }
            }
        }
    }

    @Test
    fun `uses CP437 consistently when the UTF-8 flag is absent`() {
        val entryName = "gr\u00fc\u00dfe.apk"
        val payload = "apk".toByteArray()
        val archiveFile = File(tempDirectory, "cp437.zip")
        ZipArchiveOutputStream(archiveFile).use { output ->
            output.setEncoding("Cp437")
            output.setUseLanguageEncodingFlag(false)
            output.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NEVER)
            output.putArchiveEntry(ZipArchiveEntry(entryName))
            output.write(payload)
            output.closeArchiveEntry()
        }

        provider.openMetadata(archiveFile).use { archive ->
            assertEquals(entryName, archive.entries.asSequence().single().name)
        }
        assertEquals(entryName, SeekableZipReader().read(archiveFile).entries.single().name)
        val entity = DataEntity.ZipFileEntity(entryName, DataEntity.FileEntity(archiveFile.path))
        assertContentEquals(payload, requireNotNull(entity.getInputStream()).use { it.readBytes() })
    }

    @Test
    fun `reports an unknown zip entry size as -1 instead of a substitute value`() {
        val archiveFile = File(tempDirectory, "sized.apks")
        ZipOutputStream(archiveFile.outputStream()).use { output ->
            output.writeEntry("base.apk", validInnerApk("base"))
        }

        val missing = DataEntity.ZipFileEntity("missing.apk", DataEntity.FileEntity(archiveFile.path))

        assertEquals(-1L, missing.getSize())
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
            recovered.entries.map { it.name },
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
