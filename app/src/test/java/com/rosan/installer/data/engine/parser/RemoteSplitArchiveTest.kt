// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.data.engine.signature.ApkSourceStampReader
import com.rosan.installer.data.engine.signature.CertificateFormatter
import com.rosan.installer.data.engine.signature.LightweightApkSignatureReader
import com.rosan.installer.domain.engine.exception.DescriptorAnalysisUnsupportedException
import com.rosan.installer.domain.engine.model.packageinfo.SignatureVerificationStatus
import com.rosan.installer.domain.engine.model.source.AnalysisMaterializationPolicy
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteSplitArchiveTest {
    private val provider = UnifiedZipFileProvider(CommonsZipFileProvider(), SeekableZipReader())

    @Test
    fun `stored and deflated bases analyze a prefix then install from the original entries`() {
        val directory = Files.createTempDirectory("remote-splits").toFile()
        try {
            val manifest = "binary-manifest".toByteArray()
            val base = apk(manifest, Random(1).nextBytes(1024 * 1024))
            val split = apk("split-manifest".toByteArray(), Random(2).nextBytes(1024 * 1024))
            for (method in listOf(ZipEntry.STORED, ZipEntry.DEFLATED)) {
                val backing = File(directory, "$method.apks")
                ZipOutputStream(backing.outputStream()).use { zip ->
                    zip.writeEntry("base.apk", base, method)
                    zip.writeEntry("split_config.en.apk", split, ZipEntry.DEFLATED)
                }
                val reads = AtomicLong()
                val source = remote(backing, reads)
                lateinit var baseData: DataEntity
                lateinit var splitData: DataEntity
                provider.open(source).use { archive ->
                    val metadataReads = reads.get()
                    splitData = archive.toDataEntity(requireNotNull(archive.getEntry("split_config.en.apk")), source, deferLocalHeader = true)
                    assertIs<DataEntity.ZipFileEntity>(splitData)
                    assertEquals(metadataReads, reads.get(), "Split selection must not read its local header or payload")
                    baseData = archive.toDataEntity(requireNotNull(archive.getEntry("base.apk")), source)
                    val actual = if (baseData is DataEntity.FileDescriptorEntity) {
                        val view = assertIs<DataEntity.FileDescriptorEntity>(baseData)
                        assertTrue(view.isNetworkSource)
                        provider.open(view, allowLocalHeaderFallback = false).use { inner ->
                            inner.openEntry(requireNotNull(inner.getEntry("AndroidManifest.xml"))).use { readBoundedManifest(it) }
                        }
                    } else {
                        requireNotNull(baseData.getInputStream()).use { readStreamingApkManifest(it) }
                    }
                    assertContentEquals(manifest, actual)
                    val baseEntry = requireNotNull(archive.getEntry("base.apk"))
                    val lazyBase = archive.toDataEntity(baseEntry, source, deferLocalHeader = true)
                    val readsBeforeSigningBlock = reads.get()
                    val formatter = CertificateFormatter()
                    val reader = LightweightApkSignatureReader(formatter, ApkSourceStampReader(CommonsZipFileProvider(), formatter))
                    val signature = readStreamingBaseSignature(archive, lazyBase) { reader.read(it, checkSourceStamp = false) }
                    if (method == ZipEntry.STORED) {
                        assertEquals(SignatureVerificationStatus.SIGNING_BLOCK_ONLY, assertNotNull(signature).verificationStatus)
                        assertEquals(false, signature.verified)
                    } else {
                        assertNull(signature)
                        assertEquals(readsBeforeSigningBlock, reads.get(), "Compressed bases must not be read for signing information")
                    }
                }
                assertTrue(reads.get() < backing.length() / 4, "Analysis read too much: ${reads.get()} of ${backing.length()}")
                // The archive's metadata handle is now closed. Installation must reopen each original range.
                assertContentEquals(base, requireNotNull(baseData.getInstallInputStream()).use { it.readBytes() })
                assertContentEquals(split, requireNotNull(splitData.getInstallInputStream()).use { it.readBytes() })
            }
            assertEquals(setOf("0.apks", "8.apks"), directory.list()!!.toSet())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `remote metadata has a size limit while local metadata keeps its existing behavior`() {
        val directory = Files.createTempDirectory("remote-metadata").toFile()
        try {
            val backing = File(directory, "app.apkm")
            val payload = ByteArray(MAX_STREAMING_MANIFEST_BYTES + 1) { ' '.code.toByte() }
            ZipOutputStream(backing.outputStream()).use { it.writeEntry("info.json", payload, ZipEntry.DEFLATED) }
            provider.open(remote(backing, AtomicLong())).use { archive ->
                assertFailsWith<DescriptorAnalysisUnsupportedException> { archive.openContainerMetadata(requireNotNull(archive.getEntry("info.json"))).close() }
            }
            provider.open(backing).use { archive ->
                archive.openContainerMetadata(requireNotNull(archive.getEntry("info.json"))).use {
                    assertEquals(payload.size, it.readBytes().size)
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun remote(backing: File, reads: AtomicLong) = DataEntity.FileDescriptorEntity(
        path = "https://example.com/${backing.name}",
        startOffset = 0,
        length = backing.length(),
        channelFactory = {
            val channel = FileChannel.open(backing.toPath(), StandardOpenOption.READ)
            object : SeekableByteChannel by channel {
                override fun read(dst: ByteBuffer): Int = channel.read(dst).also { if (it > 0) reads.addAndGet(it.toLong()) }
            }
        },
        descriptorFactory = { error("Manifest-only analysis must not request a platform FD") },
        preInstallSignatureAnalysis = false,
        preInstallIconAnalysis = false,
        isNetworkSource = true,
        analysisMaterializationPolicy = AnalysisMaterializationPolicy.DISALLOW,
    )

    private fun apk(manifest: ByteArray, payload: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            zip.writeEntry("AndroidManifest.xml", manifest, ZipEntry.DEFLATED)
            zip.writeEntry("classes.dex", payload, ZipEntry.STORED)
        }
    }.toByteArray()

    private fun ZipOutputStream.writeEntry(name: String, data: ByteArray, compression: Int) {
        val entry = ZipEntry(name).apply {
            method = compression
            if (compression == ZipEntry.STORED) {
                size = data.size.toLong()
                compressedSize = size
                crc = CRC32().apply { update(data) }.value
            }
        }
        putNextEntry(entry)
        write(data)
        closeEntry()
    }
}
