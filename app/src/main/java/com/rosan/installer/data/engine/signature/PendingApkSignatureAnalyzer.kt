// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import android.os.Build
import com.android.apksig.ApkVerifier
import com.android.apksig.SigningCertificateLineage
import com.android.apksig.util.DataSink
import com.android.apksig.util.DataSource
import com.rosan.installer.data.engine.parser.UnifiedZipFile
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.SignatureCertificateInfo
import com.rosan.installer.domain.engine.model.packageinfo.SigningCertificateCapabilities
import com.rosan.installer.domain.engine.model.packageinfo.SigningIdentity
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampInfo
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampVerificationStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.security.cert.X509Certificate
import java.util.UUID
import kotlin.math.min
import timber.log.Timber

/**
 * APK signature analyzer.
 */
class PendingApkSignatureAnalyzer(
    private val certificateFormatter: CertificateFormatter,
    private val lightweightApkSignatureReader: LightweightApkSignatureReader,
    private val sourceStampReader: ApkSourceStampReader,
) {
    /**
     * apksig is the source of truth for pending APK signer certificates.
     */
    fun analyze(apkPath: String, checkSourceStamp: Boolean = true): AppSignatureInfo {
        val startedAt = System.nanoTime()
        return analyze(File(apkPath), apkPath, checkSourceStamp = checkSourceStamp).also {
            Timber.d(
                "Pending APK signature analysis finished: route=file, elapsedMs=${startedAt.elapsedMillis()}, source=$apkPath",
            )
        }
    }

    fun analyze(
        data: DataEntity,
        cacheDirectory: String,
        zipFile: UnifiedZipFile? = null,
        checkSourceStamp: Boolean = true,
    ): AppSignatureInfo? {
        val startedAt = System.nanoTime()
        val route = when (data) {
            is DataEntity.FileDescriptorEntity -> "descriptor"
            is DataEntity.FileEntity -> "file"
            else -> "materialized-stream"
        }
        return when (data) {
            is DataEntity.FileDescriptorEntity -> {
                if (data.preInstallSignatureAnalysis) {
                    analyze(data, zipFile, checkSourceStamp)
                } else if (data.preInstallSigningBlockAnalysis) {
                    lightweightApkSignatureReader.read(data, zipFile, checkSourceStamp)
                } else {
                    Timber.d("Skipping pre-install signature analysis; PackageInstaller will verify: ${data.path}")
                    null
                }
            }

            is DataEntity.FileEntity -> analyze(File(data.path), data.path, zipFile, checkSourceStamp)

            else -> analyzeStream(data, cacheDirectory, checkSourceStamp)
        }.also {
            Timber.d(
                "Pending APK signature analysis finished: route=$route, " +
                    "elapsedMs=${startedAt.elapsedMillis()}, source=$data",
            )
        }
    }

    private fun analyze(
        file: File,
        displayName: String,
        zipFile: UnifiedZipFile? = null,
        checkSourceStamp: Boolean = true,
    ): AppSignatureInfo = verify(displayName, DataEntity.FileEntity(file.path), zipFile, checkSourceStamp) {
        ApkVerifier.Builder(file)
            .setMinCheckedPlatformVersion(Build.VERSION.SDK_INT)
            .setMaxCheckedPlatformVersion(Build.VERSION.SDK_INT)
            .build()
            .verify()
    }

    private fun analyze(
        data: DataEntity.FileDescriptorEntity,
        zipFile: UnifiedZipFile?,
        checkSourceStamp: Boolean,
    ): AppSignatureInfo = data.openChannel().use { channel ->
        verify(data.path, data, zipFile, checkSourceStamp) {
            ApkVerifier.Builder(SeekableChannelDataSource(channel))
                .setMinCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .setMaxCheckedPlatformVersion(Build.VERSION.SDK_INT)
                .build()
                .verify()
        }
    }

    private fun verify(
        displayName: String,
        data: DataEntity.FileEntity,
        zipFile: UnifiedZipFile?,
        checkSourceStamp: Boolean,
        operation: () -> ApkVerifier.Result,
    ): AppSignatureInfo = try {
        mapVerificationResult(operation(), data, zipFile, checkSourceStamp)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get signature hash from APK: $displayName")
        failedSignatureInfo(e.message ?: e::class.java.simpleName).copy(
            sourceStampInfo = if (checkSourceStamp) {
                sourceStampReader.read(data, zipFile)?.let { SourceStampInfo(declaredCertificateSha256 = it) }
            } else {
                null
            },
        )
    }

    internal fun mapVerificationResult(
        result: ApkVerifier.Result,
        data: DataEntity.FileEntity,
        zipFile: UnifiedZipFile? = null,
        checkSourceStamp: Boolean = true,
    ): AppSignatureInfo {
        val formatted = mutableMapOf<X509Certificate, SignatureCertificateInfo>()
        fun format(certificate: X509Certificate) = formatted.getOrPut(certificate) { certificateFormatter.format(certificate) }
        // Build Tools 37 apksig populates signerCertificates from V3.1/V3/V2/V1.
        // V3.2 hybrid signers are exposed separately through v32SchemeSigner.
        // Recheck this fallback selection when upgrading apksig.
        val legacyCertificates = result.signerCertificates.map(::format)
        val legacyHistory = result.signingCertificateLineageCertificates().map(::format)
        val legacyCapabilities = result.signingCertificateLineage.capabilities(::format)
        val hybrid = result.v32SchemeSigner?.takeIf { result.isVerified && result.isVerifiedUsingV32Scheme }
        val classical = hybrid?.classicalSignerInfo?.certificate?.let(::format)
        val pqc = hybrid?.pqcSignerInfo?.certificate?.let(::format)
        val certificates = pqc?.let(::listOf) ?: legacyCertificates
        // Android represents a hybrid as the classical lineage followed by its PQC signer.
        val history = if (hybrid != null && pqc != null && classical != null) {
            hybrid.classicalSignerInfo.signingCertificateLineage?.certificatesInLineage.orEmpty()
                .map(::format).ifEmpty { listOf(classical) } + pqc
        } else {
            legacyHistory
        }
        return AppSignatureInfo(
            verified = result.isVerified,
            signerSha256Set = certificates.mapTo(linkedSetOf()) { it.sha256 },
            certificates = certificates,
            signingCertificateHistory = history,
            hasMultipleSigners = certificates.size > 1,
            verifiedSchemes = result.verifiedSchemes(),
            warnings = result.warnings.filter { checkSourceStamp || !it.issue.name.startsWith("SOURCE_STAMP_") }
                .map { it.toString() },
            errors = result.errors.filter { checkSourceStamp || !it.issue.name.startsWith("SOURCE_STAMP_") }
                .map { it.toString() },
            sourceStampInfo = if (checkSourceStamp) readSourceStampInfo(result, data, zipFile) else null,
            isHybrid = hybrid != null,
            hybridClassicalCertificate = classical,
            lineageCapabilities = if (hybrid != null) hybrid.classicalSignerInfo.signingCertificateLineage.capabilities(::format) else legacyCapabilities,
            fallbackIdentity = if (hybrid != null && legacyCertificates.isNotEmpty()) {
                SigningIdentity(
                    signerSha256Set = legacyCertificates.mapTo(linkedSetOf()) { it.sha256 },
                    historySha256 = legacyHistory.map { it.sha256 },
                    capabilities = legacyCapabilities,
                )
            } else {
                null
            },
        )
    }

    private fun SigningCertificateLineage?.capabilities(
        format: (X509Certificate) -> SignatureCertificateInfo,
    ): Map<String, SigningCertificateCapabilities> = this?.certificatesInLineage.orEmpty().associate { certificate ->
        val flags = requireNotNull(this).getSignerCapabilities(certificate)
        format(certificate).sha256 to SigningCertificateCapabilities(flags.hasInstalledData(), flags.hasRollback())
    }

    private fun readSourceStampInfo(
        result: ApkVerifier.Result,
        data: DataEntity.FileEntity,
        zipFile: UnifiedZipFile?,
    ): SourceStampInfo? = runCatching {
        val nativeStamp = result.sourceStampInfo
        val stamp = nativeStamp?.takeUnless {
            it.sourceStampVerificationStatus == ApkVerifier.Result.SourceStampInfo.SourceStampVerificationStatus.STAMP_MISSING
        }?.let {
            SourceStampInfo(
                verificationStatus = when {
                    result.isSourceStampVerified -> SourceStampVerificationStatus.VERIFIED

                    else -> when (it.sourceStampVerificationStatus) {
                        ApkVerifier.Result.SourceStampInfo.SourceStampVerificationStatus.STAMP_VERIFICATION_FAILED -> SourceStampVerificationStatus.FAILED
                        ApkVerifier.Result.SourceStampInfo.SourceStampVerificationStatus.CERT_DIGEST_MISMATCH -> SourceStampVerificationStatus.CERTIFICATE_MISMATCH
                        ApkVerifier.Result.SourceStampInfo.SourceStampVerificationStatus.VERIFICATION_ERROR -> SourceStampVerificationStatus.VERIFICATION_ERROR
                        else -> SourceStampVerificationStatus.UNVERIFIED
                    }
                },
                certificate = it.certificate?.let(certificateFormatter::format),
                certificateLineage = it.certificatesInLineage.map(certificateFormatter::format),
                timestampEpochSeconds = it.timestampEpochSeconds.takeIf { timestamp -> timestamp > 0L },
                infoMessages = it.infoMessages.map { message -> message.toString() },
                warnings = it.warnings.map { warning -> warning.toString() },
                errors = it.errors.map { error -> error.toString() },
            )
        }
        // A successful full verification has already checked the entry against this certificate.
        // Mapping that result must not reopen the APK or scan its central directory again.
        if (stamp?.verificationStatus == SourceStampVerificationStatus.VERIFIED) return stamp

        val hasDeclaredDigest = zipFile?.getEntry(ApkSourceStampReader.STAMP_ENTRY_NAME) != null
        val missingStampSignature = result.warnings.any { it.issue == ApkVerifier.Issue.SOURCE_STAMP_SIG_MISSING }
        if (stamp == null && result.isVerified && !hasDeclaredDigest && !missingStampSignature) return null

        val digest = sourceStampReader.read(data, zipFile)
        stamp?.copy(declaredCertificateSha256 = digest)
            ?: digest?.let {
                SourceStampInfo(
                    declaredCertificateSha256 = it,
                    warnings = result.warnings.filter { warning -> warning.issue == ApkVerifier.Issue.SOURCE_STAMP_SIG_MISSING }
                        .map { warning -> warning.toString() },
                )
            }
    }.getOrElse { error ->
        Timber.w(error, "Unable to map APK source stamp information: ${data.path}")
        SourceStampInfo(
            verificationStatus = SourceStampVerificationStatus.VERIFICATION_ERROR,
            errors = listOf(error.message ?: error::class.java.simpleName),
        )
    }

    private fun analyzeStream(data: DataEntity, cacheDirectory: String, checkSourceStamp: Boolean): AppSignatureInfo {
        val tempFile = createSignatureTempFile(cacheDirectory)
        return try {
            val input = data.getInputStream()
                ?: return failedSignatureInfo("Unable to open APK input stream")
            val materializeStartedAt = System.nanoTime()
            input.use { source ->
                tempFile.outputStream().use { output ->
                    source.copyTo(output, SIGNATURE_COPY_BUFFER_SIZE)
                }
            }
            Timber.d(
                "Materialized APK for signature analysis: bytes=${tempFile.length()}, " +
                    "elapsedMs=${materializeStartedAt.elapsedMillis()}, source=$data",
            )
            val verificationStartedAt = System.nanoTime()
            analyze(tempFile, data.toString(), checkSourceStamp = checkSourceStamp).also {
                Timber.d(
                    "Verified materialized APK signature: elapsedMs=${verificationStartedAt.elapsedMillis()}, " +
                        "source=$data",
                )
            }
        } catch (e: AnalyseException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signature hash from APK data: $data")
            failedSignatureInfo(e.message ?: e::class.java.simpleName)
        } finally {
            tempFile.delete()
        }
    }

    private fun createSignatureTempFile(cacheDirectory: String): File {
        val cacheDir = File(cacheDirectory).apply { mkdirs() }
        return File.createTempFile("sig_${UUID.randomUUID()}", ".apk", cacheDir)
    }

    private fun failedSignatureInfo(message: String): AppSignatureInfo = AppSignatureInfo(
        verified = false,
        signerSha256Set = emptySet(),
        certificates = emptyList(),
        errors = listOf(message),
    )

    private fun ApkVerifier.Result.verifiedSchemes(): List<String> = buildList {
        if (isVerifiedUsingV1Scheme) add("V1")
        if (isVerifiedUsingV2Scheme) add("V2")
        if (isVerifiedUsingV3Scheme) add("V3")
        if (isVerifiedUsingV31Scheme) add("V3.1")
        if (isVerifiedUsingV32Scheme) add("V3.2")
        if (isVerifiedUsingV4Scheme) add("V4")
    }

    private fun ApkVerifier.Result.signingCertificateLineageCertificates(): List<X509Certificate> = runCatching {
        signingCertificateLineage?.certificatesInLineage.orEmpty()
    }.getOrElse { error ->
        Timber.w(error, "Failed to read APK signing certificate lineage")
        emptyList()
    }

    private fun Long.elapsedMillis(): Long = (System.nanoTime() - this) / 1_000_000L

    private class SeekableChannelDataSource(
        private val channel: SeekableByteChannel,
        private val baseOffset: Long = 0L,
        private val dataSize: Long = channel.size(),
        private val lock: Any = Any(),
    ) : DataSource {
        init {
            require(baseOffset >= 0L) { "baseOffset must be non-negative" }
            require(dataSize >= 0L) { "dataSize must be non-negative" }
            require(baseOffset <= channel.size() && dataSize <= channel.size() - baseOffset) {
                "Data source range exceeds channel size"
            }
        }

        override fun size(): Long = dataSize

        override fun feed(offset: Long, size: Long, sink: DataSink) {
            checkRange(offset, size)
            var currentOffset = offset
            var remaining = size
            val buffer = ByteBuffer.allocate(min(size, FEED_BUFFER_SIZE.toLong()).toInt().coerceAtLeast(1))
            while (remaining > 0L) {
                val count = min(remaining, buffer.capacity().toLong()).toInt()
                buffer.clear()
                buffer.limit(count)
                readFully(currentOffset, buffer)
                buffer.flip()
                sink.consume(buffer)
                currentOffset += count
                remaining -= count
            }
        }

        override fun getByteBuffer(offset: Long, size: Int): ByteBuffer {
            val result = ByteBuffer.allocate(size)
            copyTo(offset, size, result)
            result.flip()
            return result
        }

        override fun copyTo(offset: Long, size: Int, destination: ByteBuffer) {
            checkRange(offset, size.toLong())
            if (destination.remaining() < size) throw IOException("Destination buffer is too small")
            val originalLimit = destination.limit()
            try {
                destination.limit(destination.position() + size)
                readFully(offset, destination)
            } finally {
                destination.limit(originalLimit)
            }
        }

        override fun slice(offset: Long, size: Long): DataSource {
            checkRange(offset, size)
            return SeekableChannelDataSource(channel, baseOffset + offset, size, lock)
        }

        private fun readFully(offset: Long, destination: ByteBuffer) {
            synchronized(lock) {
                channel.position(baseOffset + offset)
                while (destination.hasRemaining()) {
                    when (channel.read(destination)) {
                        -1 -> throw EOFException("Unexpected end of APK data")
                        0 -> continue
                    }
                }
            }
        }

        private fun checkRange(offset: Long, size: Long) {
            if (offset < 0L || size < 0L || offset > dataSize || size > dataSize - offset) {
                throw IndexOutOfBoundsException("offset=$offset, size=$size, dataSize=$dataSize")
            }
        }

        private companion object {
            const val FEED_BUFFER_SIZE = 1024 * 1024
        }
    }

    private companion object {
        const val SIGNATURE_COPY_BUFFER_SIZE = 1024 * 1024
    }
}
