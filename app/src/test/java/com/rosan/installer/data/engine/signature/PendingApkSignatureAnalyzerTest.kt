// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.android.apksig.ApkVerifier
import com.rosan.installer.data.engine.parser.CommonsZipFileProvider
import com.rosan.installer.data.engine.parser.SeekableZipReader
import com.rosan.installer.data.engine.parser.UnifiedZipFileProvider
import com.rosan.installer.data.engine.signature.SignatureTestFixtures.Stamp
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampVerificationStatus
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingApkSignatureAnalyzerTest {
    private lateinit var tempDirectory: File
    private val certificateFormatter = CertificateFormatter()
    private val sourceStampReader = ApkSourceStampReader(CommonsZipFileProvider(), certificateFormatter)
    private val analyzer = PendingApkSignatureAnalyzer(
        certificateFormatter,
        LightweightApkSignatureReader(certificateFormatter, sourceStampReader),
        sourceStampReader,
    )

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
        val stampDigest = ByteArray(32) { 0x12 }
        ZipOutputStream(backingFile.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("AndroidManifest.xml"))
            output.write("manifest".toByteArray())
            output.closeEntry()
            output.putNextEntry(ZipEntry("stamp-cert-sha256"))
            output.write(stampDigest)
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
            },
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
        assertEquals("12".repeat(32), fileResult.sourceStampInfo?.sha256)
        assertEquals(fileResult.sourceStampInfo, descriptorResult.sourceStampInfo)
    }

    @Test
    fun `descriptor can delegate signature verification to PackageInstaller`() {
        var channelOpened = false
        val source = DataEntity.FileDescriptorEntity(
            path = "remote.apk",
            startOffset = 0L,
            length = 1L,
            channelFactory = {
                channelOpened = true
                error("System-verified source must skip pre-install signature reads")
            },
            descriptorFactory = { error("descriptor is unused") },
            preInstallSignatureAnalysis = false,
        )

        val result = analyzer.analyze(source, tempDirectory.path)

        assertEquals(null, result)
        assertFalse(channelOpened)
    }

    @Test
    fun `verified source stamp metadata is mapped without reopening the APK`() {
        for (stampType in listOf(Stamp.VALID, Stamp.TIMESTAMP)) {
            val apk = SignatureTestFixtures.create(tempDirectory, stampType)
            val nativeResult = verifyFixture(apk)
            assertTrue(nativeResult.isVerified)
            assertTrue(nativeResult.isSourceStampVerified)
            val source = DataEntity.FileDescriptorEntity(
                path = "https://example.test/${apk.name}",
                startOffset = 0L,
                length = apk.length(),
                channelFactory = { error("A verified stamp must reuse the apksig result") },
                descriptorFactory = { error("Unused") },
            )

            val result = analyzer.mapVerificationResult(nativeResult, source)

            val stamp = assertNotNull(result.sourceStampInfo)
            assertTrue(result.verified)
            assertEquals(SourceStampVerificationStatus.VERIFIED, stamp.verificationStatus)
            assertEquals(certificateFormatter.format(nativeResult.sourceStampInfo.certificate), stamp.certificate)
            assertEquals(nativeResult.sourceStampInfo.infoMessages.map { it.toString() }, stamp.infoMessages)
            assertEquals(
                nativeResult.sourceStampInfo.timestampEpochSeconds.takeIf { it > 0L },
                stamp.timestampEpochSeconds,
            )
        }
    }

    @Test
    fun `failed stamp verification preserves verified app signer identity`() {
        val apk = SignatureTestFixtures.create(tempDirectory, Stamp.CERTIFICATE_MISMATCH)
        val nativeResult = verifyFixture(apk)
        assertTrue(nativeResult.isVerified)
        assertFalse(nativeResult.isSourceStampVerified)

        val result = analyzer.mapVerificationResult(nativeResult, DataEntity.FileEntity(apk.path))

        assertTrue(result.verified)
        assertEquals(
            nativeResult.signerCertificates.mapTo(linkedSetOf()) { certificateFormatter.format(it).sha256 },
            result.signerSha256Set,
        )
        val stamp = assertNotNull(result.sourceStampInfo)
        assertEquals(SourceStampVerificationStatus.FAILED, stamp.verificationStatus)
        assertTrue(stamp.warnings.isNotEmpty())
        assertNotNull(stamp.declaredCertificateSha256)
    }

    @Test
    fun `missing stamp block retains its declaration using borrowed ZIP metadata`() {
        val apk = SignatureTestFixtures.create(tempDirectory, Stamp.MISSING_BLOCK)
        val nativeResult = verifyFixture(apk)
        var openCount = 0
        val source = DataEntity.FileDescriptorEntity(
            path = "https://example.test/partial-stamp.apk",
            startOffset = 0L,
            length = apk.length(),
            channelFactory = {
                check(++openCount == 1) { "Stamp fallback must reuse the existing ZIP metadata" }
                FileChannel.open(apk.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("Unused") },
        )
        UnifiedZipFileProvider(CommonsZipFileProvider(), SeekableZipReader()).open(source).use { archive ->
            val result = analyzer.mapVerificationResult(nativeResult, source, archive)

            assertTrue(result.verified)
            val stamp = assertNotNull(result.sourceStampInfo)
            assertEquals(SourceStampVerificationStatus.UNVERIFIED, stamp.verificationStatus)
            assertNotNull(stamp.declaredCertificateSha256)
            assertTrue(stamp.warnings.isNotEmpty())
        }
        assertEquals(1, openCount)
    }

    @Test
    fun `disabled source stamp analysis preserves APK verification and hides stamp diagnostics`() {
        for (stampType in listOf(Stamp.VALID, Stamp.MISSING_BLOCK)) {
            val apk = SignatureTestFixtures.create(tempDirectory, stampType)
            val nativeResult = verifyFixture(apk)
            val source = DataEntity.FileDescriptorEntity(
                path = "https://example.test/${apk.name}",
                startOffset = 0L,
                length = apk.length(),
                channelFactory = { error("Disabled stamp analysis must not read stamp metadata") },
                descriptorFactory = { error("Unused") },
            )

            val result = analyzer.mapVerificationResult(nativeResult, source, checkSourceStamp = false)

            assertTrue(result.verified)
            assertNull(result.sourceStampInfo)
            assertEquals(
                nativeResult.signerCertificates.mapTo(linkedSetOf()) { certificateFormatter.format(it).sha256 },
                result.signerSha256Set,
            )
            assertEquals(
                nativeResult.warnings.filterNot { it.issue.name.startsWith("SOURCE_STAMP_") }.map { it.toString() },
                result.warnings,
            )
        }
    }

    @Test
    fun `disabled source stamp analysis skips fallback reads after an APK verification failure`() {
        val backingFile = File(tempDirectory, "invalid-manifest.apk")
        ZipOutputStream(backingFile.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("AndroidManifest.xml"))
            output.write("invalid manifest".toByteArray())
            output.closeEntry()
            output.putNextEntry(ZipEntry("stamp-cert-sha256"))
            output.write(ByteArray(32) { 0x12 })
            output.closeEntry()
        }
        var openCount = 0
        val source = DataEntity.FileDescriptorEntity(
            path = "https://example.test/app.apk",
            startOffset = 0L,
            length = backingFile.length(),
            channelFactory = {
                openCount++
                FileChannel.open(backingFile.toPath(), StandardOpenOption.READ)
            },
            descriptorFactory = { error("Unused") },
        )

        val result = assertNotNull(analyzer.analyze(source, tempDirectory.path, checkSourceStamp = false))

        assertFalse(result.verified)
        assertTrue(result.errors.isNotEmpty())
        assertNull(result.sourceStampInfo)
        assertEquals(1, openCount)
    }

    private fun verifyFixture(apk: File): ApkVerifier.Result = ApkVerifier.Builder(apk)
        .setMinCheckedPlatformVersion(30)
        .setMaxCheckedPlatformVersion(30)
        .build().verify()
}
