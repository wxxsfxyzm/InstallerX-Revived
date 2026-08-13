// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.SignatureVerificationStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import timber.log.Timber
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel

/**
 * Reads signer certificate declarations from APK Signature Scheme v2/v3 blocks.
 *
 * This deliberately does not verify signer signatures or APK content digests. The result can
 * support certificate comparison and profile restrictions, while PackageInstaller remains the
 * authority for final signature verification.
 */
class LightweightApkSignatureReader(
    private val certificateFormatter: CertificateFormatter
) {
    fun read(data: DataEntity.FileDescriptorEntity): AppSignatureInfo {
        val declarations = runCatching {
            data.openChannel().use(::readDeclarations)
        }.onFailure { error ->
            Timber.w(error, "Unable to read lightweight APK signer declarations: ${data.path}")
        }.getOrDefault(SignatureDeclarations())

        val uniqueCertificates = declarations.certificates.fold(mutableListOf<ByteArray>()) { result, certificate ->
            if (result.none(certificate::contentEquals)) result += certificate
            result
        }
        val certificates = uniqueCertificates
            .map(certificateFormatter::format)

        return AppSignatureInfo(
            verified = false,
            signerSha256Set = certificates.mapTo(linkedSetOf()) { it.sha256 },
            certificates = certificates,
            hasMultipleSigners = certificates.size > 1,
            declaredSchemes = declarations.schemes,
            verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY
        )
    }

    private fun readDeclarations(channel: SeekableByteChannel): SignatureDeclarations {
        val fileSize = channel.size()
        if (fileSize < ZIP_EOCD_MIN_SIZE) return SignatureDeclarations()

        val eocd = findEocd(channel, fileSize) ?: return SignatureDeclarations()
        val centralDirectoryOffset = eocd.getUnsignedInt(ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET)
        if (centralDirectoryOffset < APK_SIGNING_BLOCK_FOOTER_SIZE || centralDirectoryOffset > fileSize) {
            return SignatureDeclarations()
        }

        val footer = channel.readFully(
            centralDirectoryOffset - APK_SIGNING_BLOCK_FOOTER_SIZE,
            APK_SIGNING_BLOCK_FOOTER_SIZE
        )
        if (!footer.matchesMagic(APK_SIGNING_BLOCK_MAGIC_OFFSET, APK_SIGNING_BLOCK_MAGIC)) {
            return SignatureDeclarations()
        }

        val sizeInFooter = footer.getLong(0)
        if (sizeInFooter < APK_SIGNING_BLOCK_FOOTER_SIZE || sizeInFooter > MAX_SIGNING_BLOCK_SIZE - 8L) {
            throw IOException("APK Signing Block size is outside the lightweight reader limit: $sizeInFooter")
        }
        val totalSize = sizeInFooter + 8L
        val blockOffset = centralDirectoryOffset - totalSize
        if (blockOffset < 0L) throw IOException("APK Signing Block starts before the APK")

        val block = channel.readFully(blockOffset, totalSize.toInt())
        if (block.getLong(0) != sizeInFooter) throw IOException("APK Signing Block size fields differ")
        if (!block.matchesMagic(block.limit() - APK_SIGNING_BLOCK_MAGIC.size, APK_SIGNING_BLOCK_MAGIC)) {
            throw IOException("APK Signing Block magic is missing")
        }

        val pairs = block.sliceRange(8, block.limit() - APK_SIGNING_BLOCK_FOOTER_SIZE)
        val certificates = mutableListOf<ByteArray>()
        val schemes = linkedSetOf<String>()
        while (pairs.hasRemaining()) {
            if (pairs.remaining() < 8) throw IOException("Truncated APK Signing Block pair")
            val pairSize = pairs.long.toBoundedInt(pairs.remaining(), "APK Signing Block pair")
            val pair = pairs.readSlice(pairSize)
            if (pair.remaining() < 4) throw IOException("APK Signing Block pair has no ID")
            val id = pair.int
            val scheme = SCHEMES[id] ?: continue
            certificates += parseSchemeSigners(pair, scheme.hasSdkRange)
            schemes += scheme.label
        }
        return SignatureDeclarations(certificates, schemes.toList())
    }

    private fun parseSchemeSigners(block: ByteBuffer, hasSdkRange: Boolean): List<ByteArray> {
        val signers = block.readLengthPrefixedSlice()
        val certificates = mutableListOf<ByteArray>()
        var signerCount = 0
        while (signers.hasRemaining()) {
            if (++signerCount > MAX_SIGNER_COUNT) throw IOException("Too many APK signer declarations")
            val signer = signers.readLengthPrefixedSlice()
            val signedData = signer.readLengthPrefixedSlice()
            if (hasSdkRange) {
                signer.requireRemaining(8, "signer SDK range")
                signer.position(signer.position() + 8)
            }
            signer.readLengthPrefixedSlice() // signatures
            signer.readLengthPrefixedBytes() // public key
            parseSignedDataSignerCertificate(signedData, hasSdkRange)?.let(certificates::add)
        }
        return certificates
    }

    private fun parseSignedDataSignerCertificate(
        signedData: ByteBuffer,
        hasSdkRange: Boolean
    ): ByteArray? {
        signedData.readLengthPrefixedSlice() // content digests; deliberately not verified
        val certificateSequence = signedData.readLengthPrefixedSlice()
        if (hasSdkRange) {
            signedData.requireRemaining(8, "signed-data SDK range")
            signedData.position(signedData.position() + 8)
        }
        signedData.readLengthPrefixedSlice() // additional attributes

        var signerCertificate: ByteArray? = null
        var certificateCount = 0
        while (certificateSequence.hasRemaining()) {
            if (++certificateCount > MAX_CERTIFICATE_COUNT) {
                throw IOException("Too many certificates in an APK signer declaration")
            }
            val certificate = certificateSequence.readLengthPrefixedSlice()
            if (certificate.remaining() > MAX_CERTIFICATE_SIZE) {
                throw IOException("APK signer certificate is too large: ${certificate.remaining()}")
            }
            if (signerCertificate == null) {
                signerCertificate = ByteArray(certificate.remaining()).also(certificate::get)
            }
        }
        return signerCertificate
    }

    private fun findEocd(channel: SeekableByteChannel, fileSize: Long): ByteBuffer? {
        val tailSize = minOf(fileSize, ZIP_EOCD_MAX_SIZE.toLong()).toInt()
        val tail = channel.readFully(fileSize - tailSize, tailSize)
        for (offset in tail.limit() - ZIP_EOCD_MIN_SIZE downTo 0) {
            if (tail.getInt(offset) != ZIP_EOCD_SIGNATURE) continue
            val commentLength = tail.getUnsignedShort(offset + ZIP_EOCD_COMMENT_LENGTH_OFFSET)
            if (offset + ZIP_EOCD_MIN_SIZE + commentLength == tail.limit()) {
                return tail.sliceRange(offset, tail.limit())
            }
        }
        return null
    }

    private fun SeekableByteChannel.readFully(offset: Long, size: Int): ByteBuffer {
        if (offset < 0L || size < 0 || offset > this.size() || size.toLong() > this.size() - offset) {
            throw IOException("Requested APK range is outside the source: offset=$offset, size=$size")
        }
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        position(offset)
        while (buffer.hasRemaining()) {
            when (read(buffer)) {
                -1 -> throw EOFException("Unexpected end of APK while reading signer declarations")
                0 -> continue
            }
        }
        buffer.flip()
        return buffer
    }

    private fun ByteBuffer.readLengthPrefixedSlice(): ByteBuffer {
        requireRemaining(4, "length-prefixed field")
        return readSlice(int.toBoundedInt(remaining(), "length-prefixed field"))
    }

    private fun ByteBuffer.readLengthPrefixedBytes(): ByteArray {
        val value = readLengthPrefixedSlice()
        return ByteArray(value.remaining()).also(value::get)
    }

    private fun ByteBuffer.readSlice(size: Int): ByteBuffer {
        requireRemaining(size, "field")
        val result = slice().order(ByteOrder.LITTLE_ENDIAN)
        result.limit(size)
        position(position() + size)
        return result
    }

    private fun ByteBuffer.sliceRange(start: Int, end: Int): ByteBuffer {
        if (start < 0 || end < start || end > limit()) throw IOException("Invalid APK buffer range")
        val duplicate = duplicate().order(ByteOrder.LITTLE_ENDIAN)
        duplicate.position(start)
        duplicate.limit(end)
        return duplicate.slice().order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun ByteBuffer.requireRemaining(size: Int, label: String) {
        if (size < 0 || remaining() < size) {
            throw IOException("Truncated $label: need=$size, remaining=${remaining()}")
        }
    }

    private fun Long.toBoundedInt(available: Int, label: String): Int {
        if (this < 0L || this > available.toLong() || this > Int.MAX_VALUE.toLong()) {
            throw IOException("Invalid $label size: $this, available=$available")
        }
        return toInt()
    }

    private fun Int.toBoundedInt(available: Int, label: String): Int {
        if (this < 0 || this > available) throw IOException("Invalid $label size: $this, available=$available")
        return this
    }

    private fun ByteBuffer.getUnsignedInt(offset: Int): Long = getInt(offset).toLong() and 0xffffffffL

    private fun ByteBuffer.getUnsignedShort(offset: Int): Int = getShort(offset).toInt() and 0xffff

    private fun ByteBuffer.matchesMagic(offset: Int, magic: ByteArray): Boolean =
        offset >= 0 && offset + magic.size <= limit() && magic.indices.all { index ->
            get(offset + index) == magic[index]
        }

    private data class SignatureDeclarations(
        val certificates: List<ByteArray> = emptyList(),
        val schemes: List<String> = emptyList()
    )

    private data class Scheme(val label: String, val hasSdkRange: Boolean)

    private companion object {
        const val ZIP_EOCD_SIGNATURE = 0x06054b50
        const val ZIP_EOCD_MIN_SIZE = 22
        const val ZIP_EOCD_MAX_SIZE = ZIP_EOCD_MIN_SIZE + 0xffff
        const val ZIP_EOCD_CENTRAL_DIRECTORY_OFFSET = 16
        const val ZIP_EOCD_COMMENT_LENGTH_OFFSET = 20
        const val APK_SIGNING_BLOCK_FOOTER_SIZE = 24
        const val APK_SIGNING_BLOCK_MAGIC_OFFSET = 8
        const val MAX_SIGNING_BLOCK_SIZE = 8 * 1024 * 1024
        const val MAX_SIGNER_COUNT = 32
        const val MAX_CERTIFICATE_COUNT = 64
        const val MAX_CERTIFICATE_SIZE = 64 * 1024
        val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
        val SCHEMES = mapOf(
            0x7109871a to Scheme("V2", hasSdkRange = false),
            0xf05368c0.toInt() to Scheme("V3", hasSdkRange = true),
            0x1b93ad61 to Scheme("V3.1", hasSdkRange = true)
        )
    }
}
