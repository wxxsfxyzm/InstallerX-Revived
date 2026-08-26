// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.packageinfo.SignatureVerificationStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LightweightApkSignatureReaderTest {
    private lateinit var tempDirectory: File
    private val reader = LightweightApkSignatureReader(CertificateFormatter())

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

    private fun createApkWithV2Certificate(certificate: ByteArray): ByteArray {
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
        const val ZIP_EOCD_SIGNATURE = 0x06054b50
        val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
    }
}
