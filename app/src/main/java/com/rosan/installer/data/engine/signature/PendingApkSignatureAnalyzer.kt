// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.android.apksig.ApkVerifier
import com.android.apksig.util.DataSink
import com.android.apksig.util.DataSource
import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.source.DataEntity
import timber.log.Timber
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.security.cert.X509Certificate
import java.util.UUID
import kotlin.math.min

/**
 * APK signature analyzer.
 */
class PendingApkSignatureAnalyzer(
    private val certificateFormatter: CertificateFormatter
) {
    /**
     * apksig is the source of truth for pending APK signer certificates.
     */
    fun analyze(apkPath: String): AppSignatureInfo {
        return analyze(File(apkPath), apkPath)
    }

    fun analyze(data: DataEntity, cacheDirectory: String): AppSignatureInfo? {
        return when (data) {
            is DataEntity.FileDescriptorEntity -> analyze(data)
            is DataEntity.FileEntity -> analyze(File(data.path), data.path)
            else -> analyzeStream(data, cacheDirectory)
        }
    }

    private fun analyze(file: File, displayName: String): AppSignatureInfo {
        return verify(displayName) {
            ApkVerifier.Builder(file).build().verify()
        }
    }

    private fun analyze(data: DataEntity.FileDescriptorEntity): AppSignatureInfo {
        return data.openChannel().use { channel ->
            verify(data.path) {
                ApkVerifier.Builder(SeekableChannelDataSource(channel)).build().verify()
            }
        }
    }

    private fun verify(
        displayName: String,
        operation: () -> ApkVerifier.Result
    ): AppSignatureInfo {
        return try {
            val result = operation()
            val certificates = result.signerCertificates.map { certificate ->
                certificateFormatter.format(certificate)
            }
            AppSignatureInfo(
                verified = result.isVerified,
                signerSha256Set = certificates.mapTo(linkedSetOf()) { it.sha256 },
                certificates = certificates,
                signingCertificateHistory = result.signingCertificateLineageCertificates().map { certificate ->
                    certificateFormatter.format(certificate)
                },
                hasMultipleSigners = certificates.size > 1,
                verifiedSchemes = result.verifiedSchemes(),
                warnings = result.warnings.map { it.toString() },
                errors = result.errors.map { it.toString() }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get signature hash from APK: $displayName")
            failedSignatureInfo(e.message ?: e::class.java.simpleName)
        }
    }

    private fun analyzeStream(
        data: DataEntity,
        cacheDirectory: String
    ): AppSignatureInfo {
        val tempFile = createSignatureTempFile(cacheDirectory)
        return try {
            val input = data.getInputStream()
                ?: return failedSignatureInfo("Unable to open APK input stream")
            input.use { source ->
                tempFile.outputStream().use { output -> source.copyTo(output) }
            }
            analyze(tempFile, data.toString())
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

    private fun failedSignatureInfo(message: String): AppSignatureInfo {
        return AppSignatureInfo(
            verified = false,
            signerSha256Set = emptySet(),
            certificates = emptyList(),
            errors = listOf(message)
        )
    }

    private fun ApkVerifier.Result.verifiedSchemes(): List<String> {
        return buildList {
            if (isVerifiedUsingV1Scheme) add("V1")
            if (isVerifiedUsingV2Scheme) add("V2")
            if (isVerifiedUsingV3Scheme) add("V3")
            if (isVerifiedUsingV31Scheme) add("V3.1")
            if (isVerifiedUsingV4Scheme) add("V4")
        }
    }

    private fun ApkVerifier.Result.signingCertificateLineageCertificates(): List<X509Certificate> {
        return runCatching {
            signingCertificateLineage?.certificatesInLineage.orEmpty()
        }.getOrElse { error ->
            Timber.w(error, "Failed to read APK signing certificate lineage")
            emptyList()
        }
    }

    private class SeekableChannelDataSource(
        private val channel: SeekableByteChannel,
        private val baseOffset: Long = 0L,
        private val dataSize: Long = channel.size(),
        private val lock: Any = Any()
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
}
