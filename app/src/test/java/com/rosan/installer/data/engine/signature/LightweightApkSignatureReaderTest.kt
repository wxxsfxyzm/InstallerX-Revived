// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.data.engine.parser.CommonsZipFileProvider
import com.rosan.installer.data.engine.parser.SeekableZipReader
import com.rosan.installer.data.engine.parser.UnifiedZipFileProvider
import com.rosan.installer.data.engine.signature.SignatureTestFixtures.Stamp
import com.rosan.installer.domain.engine.model.packageinfo.SignatureVerificationStatus
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampVerificationStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightweightApkSignatureReaderTest {
    private lateinit var tempDirectory: File
    private val reader = LightweightApkSignatureReader(
        CertificateFormatter(),
        ApkSourceStampReader(CommonsZipFileProvider(), CertificateFormatter()),
    )

    @BeforeTest
    fun setUp() {
        tempDirectory = Files.createTempDirectory("lightweight-signature-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDirectory.deleteRecursively()
    }

    @Test
    fun `v2 signer certificate is reported as an unverified declaration`() {
        val encodedCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01)
        val apk = File(tempDirectory, "declared-v2.apk").apply {
            writeBytes(createApkWithV2Certificate(encodedCertificate))
        }
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("A raw descriptor is not needed") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val result = reader.read(entity)

        assertFalse(result.verified)
        assertEquals(SignatureVerificationStatus.SIGNING_BLOCK_ONLY, result.verificationStatus)
        assertEquals(listOf("V2"), result.declaredSchemes)
        assertEquals(setOf(encodedCertificate.sha256()), result.signerSha256Set)
        assertEquals(1, result.certificates.size)
        assertEquals(emptyList(), result.verifiedSchemes)
    }

    @Test
    fun `APK without a signing block remains an unverified lightweight result`() {
        val apk = File(tempDirectory, "unsigned.apk").apply {
            writeBytes(createEocd(centralDirectoryOffset = 0))
        }
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/unsigned.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("A raw descriptor is not needed") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val result = reader.read(entity)

        assertEquals(SignatureVerificationStatus.SIGNING_BLOCK_ONLY, result.verificationStatus)
        assertEquals(emptyList(), result.declaredSchemes)
        assertEquals(emptySet(), result.signerSha256Set)
    }

    @Test
    fun `source stamp digest is separate from lightweight signer declarations`() {
        val encodedCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01)
        val stampDigest = ByteArray(32) { 0x34 }
        val apk = File(tempDirectory, "stamped-v2.apk").apply {
            writeBytes(createApkWithV2Certificate(encodedCertificate, stampDigest))
        }
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/stamped.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("A raw descriptor is not needed") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val result = reader.read(entity, platformSdk = 35)

        assertEquals("34".repeat(32), result.sourceStampInfo?.sha256)
        assertEquals(setOf(encodedCertificate.sha256()), result.signerSha256Set)
        assertEquals(SignatureVerificationStatus.SIGNING_BLOCK_ONLY, result.verificationStatus)
        assertFalse(result.verified)
    }

    @Test
    fun `lightweight stamp metadata reuses the signing block and borrowed ZIP view`() {
        val signingStartedAt = System.currentTimeMillis() / 1000
        val apk = SignatureTestFixtures.create(tempDirectory, Stamp.TIMESTAMP)
        val signingFinishedAt = System.currentTimeMillis() / 1000
        var openCount = 0
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/stamped.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = {
                check(++openCount <= 2) { "Stamp reads must not open a third channel" }
                FileChannel.open(apk.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("Unused") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )
        UnifiedZipFileProvider(CommonsZipFileProvider(), SeekableZipReader()).open(entity).use { archive ->
            val result = reader.read(entity, platformSdk = 30, zipFile = archive)

            val stamp = assertNotNull(result.sourceStampInfo)
            assertFalse(result.verified)
            assertEquals(SourceStampVerificationStatus.UNVERIFIED, stamp.verificationStatus)
            assertTrue(assertNotNull(stamp.timestampEpochSeconds) in signingStartedAt..signingFinishedAt)
            assertEquals(stamp.declaredCertificateSha256, stamp.certificate?.sha256)
            assertEquals(emptyList(), stamp.warnings)
        }
        assertEquals(2, openCount)
    }

    @Test
    fun `disabled source stamp analysis keeps lightweight signer declarations without stamp reads`() {
        val apk = SignatureTestFixtures.create(tempDirectory, Stamp.TIMESTAMP)
        var openCount = 0
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/stamped.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = {
                openCount++
                FileChannel.open(apk.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("Unused") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val result = reader.read(entity, platformSdk = 30, checkSourceStamp = false)

        assertNull(result.sourceStampInfo)
        assertTrue(result.certificates.isNotEmpty())
        assertEquals(SignatureVerificationStatus.SIGNING_BLOCK_ONLY, result.verificationStatus)
        assertEquals(1, openCount)
    }

    @Test
    fun `v3 signer certificate is filtered by the current platform SDK`() {
        val olderCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01)
        val currentCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x02)
        val apk = File(tempDirectory, "sdk-targeted-v3.apk").apply {
            writeBytes(
                createApkWithV3Signers(
                    createV3Signer(olderCertificate, minSdk = 28, maxSdk = 34),
                    createV3Signer(currentCertificate, minSdk = 35, maxSdk = Int.MAX_VALUE),
                ),
            )
        }
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("A raw descriptor is not needed") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val sdk34Result = reader.read(entity, platformSdk = 34)
        val sdk35Result = reader.read(entity, platformSdk = 35)

        assertEquals(listOf("V3"), sdk34Result.declaredSchemes)
        assertEquals(setOf(olderCertificate.sha256()), sdk34Result.signerSha256Set)
        assertEquals(setOf(currentCertificate.sha256()), sdk35Result.signerSha256Set)
    }

    @Test
    fun `v31 signer takes precedence over the v3 fallback on supported platforms`() {
        val fallbackCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01)
        val rotatedCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x02)
        val apk = File(tempDirectory, "v31-with-v3-fallback.apk").apply {
            writeBytes(
                createApkWithSchemePairs(
                    createSchemePair(
                        V3_BLOCK_ID,
                        createV3Signer(fallbackCertificate, minSdk = 28, maxSdk = Int.MAX_VALUE),
                    ),
                    createSchemePair(
                        V31_BLOCK_ID,
                        createV3Signer(rotatedCertificate, minSdk = 33, maxSdk = Int.MAX_VALUE),
                    ),
                ),
            )
        }
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("A raw descriptor is not needed") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val sdk32Result = reader.read(entity, platformSdk = 32)
        val sdk35Result = reader.read(entity, platformSdk = 35)

        assertEquals(listOf("V3", "V3.1"), sdk35Result.declaredSchemes)
        assertEquals(setOf(fallbackCertificate.sha256()), sdk32Result.signerSha256Set)
        assertEquals(setOf(rotatedCertificate.sha256()), sdk35Result.signerSha256Set)
    }

    @Test
    fun `unrecognized v32 hybrid roles remain unknown instead of ordinary multisigners`() {
        val classicalCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01)
        val pqcCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x02)
        val fallbackCertificate = byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x03)
        val apk = File(tempDirectory, "v32-with-v3-fallback.apk").apply {
            writeBytes(
                createApkWithSchemePairs(
                    createSchemePair(
                        V3_BLOCK_ID,
                        createV3Signer(fallbackCertificate, minSdk = 28, maxSdk = Int.MAX_VALUE),
                    ),
                    createSchemePair(
                        V32_BLOCK_ID,
                        createV3Signer(classicalCertificate, minSdk = 37, maxSdk = Int.MAX_VALUE),
                        createV3Signer(pqcCertificate, minSdk = 37, maxSdk = Int.MAX_VALUE),
                    ),
                ),
            )
        }
        val entity = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = { FileChannel.open(apk.toPath(), StandardOpenOption.READ) },
            descriptorFactory = { error("A raw descriptor is not needed") },
            preInstallSignatureAnalysis = false,
            preInstallSigningBlockAnalysis = true,
        )

        val result = reader.read(entity, platformSdk = 37)

        assertEquals(listOf("V3", "V3.2"), result.declaredSchemes)
        assertTrue(result.isHybrid)
        assertFalse(result.hasMultipleSigners)
        assertTrue(result.signerSha256Set.isEmpty())
        assertFalse(result.signingIdentity.isComplete)
        assertEquals(setOf(fallbackCertificate.sha256()), result.fallbackIdentity?.signerSha256Set)
    }

    private fun createApkWithV2Certificate(certificate: ByteArray, stampDigest: ByteArray? = null): ByteArray {
        val certificates = lengthPrefixed(certificate)
        val signedData = concat(
            lengthPrefixed(byteArrayOf()),
            lengthPrefixed(certificates),
            lengthPrefixed(byteArrayOf()),
        )
        val signer = concat(
            lengthPrefixed(signedData),
            lengthPrefixed(byteArrayOf()),
            lengthPrefixed(byteArrayOf()),
        )
        val schemeBlock = lengthPrefixed(lengthPrefixed(signer))
        val pair = littleEndianBuffer(8 + 4 + schemeBlock.size)
            .putLong((4 + schemeBlock.size).toLong())
            .putInt(V2_BLOCK_ID)
            .put(schemeBlock)
            .array()
        val sizeWithoutHeader = pair.size + 24L
        val signingBlock = littleEndianBuffer((sizeWithoutHeader + 8L).toInt())
            .putLong(sizeWithoutHeader)
            .put(pair)
            .putLong(sizeWithoutHeader)
            .put(APK_SIGNING_BLOCK_MAGIC)
            .array()
        if (stampDigest != null) {
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("stamp-cert-sha256"))
                zip.write(stampDigest)
                zip.closeEntry()
            }
            val zipBytes = output.toByteArray()
            val eocdOffset = zipBytes.size - 22
            val zipBuffer = ByteBuffer.wrap(zipBytes).order(ByteOrder.LITTLE_ENDIAN)
            val centralDirectoryOffset = zipBuffer.getInt(eocdOffset + 16)
            zipBuffer.putInt(eocdOffset + 16, centralDirectoryOffset + signingBlock.size)
            return concat(
                zipBytes.copyOfRange(0, centralDirectoryOffset),
                signingBlock,
                zipBytes.copyOfRange(centralDirectoryOffset, zipBytes.size),
            )
        }
        return concat(signingBlock, createEocd(signingBlock.size))
    }

    private fun createApkWithV3Signers(vararg signers: ByteArray): ByteArray = createApkWithSchemePairs(createSchemePair(V3_BLOCK_ID, *signers))

    private fun createSchemePair(id: Int, vararg signers: ByteArray): ByteArray {
        val schemeBlock = lengthPrefixed(concat(*signers.map(::lengthPrefixed).toTypedArray()))
        return littleEndianBuffer(8 + 4 + schemeBlock.size)
            .putLong((4 + schemeBlock.size).toLong())
            .putInt(id)
            .put(schemeBlock)
            .array()
    }

    private fun createApkWithSchemePairs(vararg pairs: ByteArray): ByteArray {
        val pairsSize = pairs.sumOf(ByteArray::size)
        val sizeWithoutHeader = pairsSize + 24L
        val signingBlock = littleEndianBuffer((sizeWithoutHeader + 8L).toInt())
            .putLong(sizeWithoutHeader)
            .apply { pairs.forEach(::put) }
            .putLong(sizeWithoutHeader)
            .put(APK_SIGNING_BLOCK_MAGIC)
            .array()
        return concat(signingBlock, createEocd(signingBlock.size))
    }

    private fun createV3Signer(certificate: ByteArray, minSdk: Int, maxSdk: Int): ByteArray {
        val certificates = lengthPrefixed(certificate)
        val signedData = concat(
            lengthPrefixed(byteArrayOf()),
            lengthPrefixed(certificates),
            littleEndianBuffer(8).putInt(minSdk).putInt(maxSdk).array(),
            lengthPrefixed(byteArrayOf()),
        )
        return concat(
            lengthPrefixed(signedData),
            littleEndianBuffer(8).putInt(minSdk).putInt(maxSdk).array(),
            lengthPrefixed(byteArrayOf()),
            lengthPrefixed(byteArrayOf()),
        )
    }

    private fun createEocd(centralDirectoryOffset: Int): ByteArray = littleEndianBuffer(22)
        .putInt(ZIP_EOCD_SIGNATURE)
        .putShort(0)
        .putShort(0)
        .putShort(0)
        .putShort(0)
        .putInt(0)
        .putInt(centralDirectoryOffset)
        .putShort(0)
        .array()

    private fun lengthPrefixed(value: ByteArray): ByteArray = littleEndianBuffer(4 + value.size).putInt(value.size).put(value).array()

    private fun concat(vararg values: ByteArray): ByteArray = ByteArray(values.sumOf(ByteArray::size)).also { result ->
        var offset = 0
        values.forEach { value ->
            value.copyInto(result, offset)
            offset += value.size
        }
    }

    private fun littleEndianBuffer(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val V2_BLOCK_ID = 0x7109871a
        const val V3_BLOCK_ID = 0xf05368c0.toInt()
        const val V31_BLOCK_ID = 0x1b93ad61
        const val V32_BLOCK_ID = 0x70e1c89f
        const val ZIP_EOCD_SIGNATURE = 0x06054b50
        val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
    }
}
