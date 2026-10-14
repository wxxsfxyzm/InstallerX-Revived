// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.data.engine.parser.CommonsZipFileProvider
import com.rosan.installer.data.engine.parser.UnifiedZipFile
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampInfo
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import timber.log.Timber

/** Reads the declared source stamp certificate digest without verifying the source stamp. */
class ApkSourceStampReader internal constructor(
    private val zipFileProvider: CommonsZipFileProvider,
    private val certificateFormatter: CertificateFormatter,
) {
    fun read(data: DataEntity.FileEntity, zipFile: UnifiedZipFile? = null): String? = runCatching {
        if (zipFile != null) {
            val entry = zipFile.getEntry(STAMP_ENTRY_NAME) ?: return null
            if (zipFile.entries.count { it.name == STAMP_ENTRY_NAME } != 1 ||
                !hasDigestSize(entry.size, entry.compressedSize)
            ) {
                return null
            }
            return zipFile.openEntry(entry).use(::readDigest)
        }
        zipFileProvider.openMetadata(data).use { archive ->
            val entries = archive.getEntries(STAMP_ENTRY_NAME).iterator()
            if (!entries.hasNext()) return null
            val entry = entries.next()
            if (entries.hasNext() || !hasDigestSize(entry.size, entry.compressedSize)) {
                return null
            }

            zipFileProvider.openEntry(archive, entry).use(::readDigest)
        }
    }.onFailure { error ->
        Timber.w(error, "Unable to read APK source stamp digest: ${data.path}")
    }.getOrNull()

    /** Parses declarations from an already-read v2 source stamp block, without cryptographic checks. */
    internal fun readDeclaration(block: ByteBuffer): SourceStampInfo {
        var stamp = SourceStampInfo()
        return try {
            val data = block.readLengthPrefixedSlice()
            val encodedCertificate = data.readLengthPrefixedSlice()
            if (encodedCertificate.remaining() !in 1..MAX_CERTIFICATE_SIZE) {
                throw IOException("Source stamp certificate size is outside the lightweight limit")
            }
            val certificateBytes = ByteArray(encodedCertificate.remaining()).also(encodedCertificate::get)
            stamp = stamp.copy(certificate = certificateFormatter.format(certificateBytes))
            data.readLengthPrefixedSlice() // signatures over APK signing-scheme digests; not verified
            if (data.hasRemaining()) {
                val attributeData = data.readLengthPrefixedSlice()
                data.readLengthPrefixedSlice() // attribute signatures; not verified
                val attributes = attributeData.readLengthPrefixedSlice()
                var attributeCount = 0
                while (attributes.hasRemaining()) {
                    if (++attributeCount > MAX_ATTRIBUTE_COUNT) throw IOException("Too many source stamp attributes")
                    val attribute = attributes.readLengthPrefixedSlice()
                    if (attribute.remaining() < 4) throw IOException("Source stamp attribute has no ID")
                    if (attribute.int == STAMP_TIME_ATTR_ID) {
                        if (attribute.remaining() < 8) throw IOException("Truncated source stamp timestamp")
                        stamp = stamp.copy(timestampEpochSeconds = attribute.long.takeIf { it > 0L })
                    }
                }
            }
            stamp
        } catch (error: Exception) {
            Timber.w(error, "Unable to parse lightweight source stamp declarations")
            stamp.copy(warnings = listOf(error.message ?: error::class.java.simpleName))
        }
    }

    private fun hasDigestSize(size: Long, compressedSize: Long): Boolean = size == SHA256_SIZE.toLong() && compressedSize in 1L..MAX_COMPRESSED_SIZE

    private fun readDigest(input: InputStream): String? {
        val digest = ByteArray(SHA256_SIZE)
        var offset = 0
        while (offset < digest.size) {
            val count = input.read(digest, offset, digest.size - offset)
            if (count <= 0) return null
            offset += count
        }
        if (input.read() != -1) return null
        // The entry already contains a SHA-256 digest; do not hash it again.
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun ByteBuffer.readLengthPrefixedSlice(): ByteBuffer {
        if (remaining() < 4) throw IOException("Truncated source stamp field")
        val size = int
        if (size !in 0..remaining()) throw IOException("Invalid source stamp field size: $size")
        val result = slice().order(ByteOrder.LITTLE_ENDIAN)
        result.limit(size)
        position(position() + size)
        return result
    }

    companion object {
        const val STAMP_ENTRY_NAME = "stamp-cert-sha256"
        internal const val STAMP_BLOCK_ID = 0x6dff800d
        private const val SHA256_SIZE = 32
        private const val MAX_COMPRESSED_SIZE = 1024L
        private const val MAX_CERTIFICATE_SIZE = 64 * 1024
        private const val MAX_ATTRIBUTE_COUNT = 64
        private val STAMP_TIME_ATTR_ID = 0xe43c5946.toInt()
    }
}
