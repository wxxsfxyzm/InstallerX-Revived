// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.exception.SeekableZipException
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.SeekableZipArchive
import com.rosan.installer.domain.engine.model.source.SeekableZipEntry
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Reads ZIP local-file-header metadata without walking entry payloads.
 *
 * This reader is intentionally strict. Entries using data descriptors cannot be skipped safely
 * without inflating or scanning their payload, so they are rejected. ZIP64 sizes are supported
 * when present in the local extra field.
 */
internal class SeekableZipReader {
    fun read(file: File): SeekableZipArchive = read(DataEntity.FileEntity(file.path))

    fun read(file: DataEntity.FileEntity): SeekableZipArchive {
        val physicalFile = if (file is DataEntity.FileDescriptorEntity) null else File(file.path)
        if (physicalFile != null && !physicalFile.isFile) {
            throw SeekableZipException("Not a regular file: ${file.path}")
        }

        return try {
            ChannelRandomAccessReader(file.openChannel()).use { input -> read(input) }
        } catch (e: SeekableZipException) {
            throw e
        } catch (e: IOException) {
            throw SeekableZipException("Failed to read ZIP local headers: ${file.path}", e)
        }
    }

    private fun read(input: ChannelRandomAccessReader): SeekableZipArchive {
        val fileSize = input.length
        if (fileSize == 0L) {
            throw SeekableZipException("Empty file is not a ZIP archive")
        }
        val entries = mutableListOf<SeekableZipEntry>()
        var offset = 0L
        var metadataBytes = 0L

        while (true) {
            if (offset == fileSize) {
                return SeekableZipArchive(entries, hasCentralDirectory = false)
            }
            if (fileSize - offset < SIGNATURE_SIZE) {
                throw SeekableZipException("Truncated ZIP signature at offset $offset")
            }

            input.position = offset
            when (val signature = input.readUnsignedIntLittleEndian()) {
                LOCAL_FILE_HEADER_SIGNATURE -> {
                    if (entries.size >= MAX_ENTRY_COUNT) {
                        throw SeekableZipException("ZIP local-header count exceeds $MAX_ENTRY_COUNT")
                    }
                    val entry = readLocalEntry(input, offset, fileSize)
                    metadataBytes = checkedAdd(
                        metadataBytes,
                        entry.dataOffset - offset - LOCAL_FILE_HEADER_SIZE,
                    )
                    if (metadataBytes > MAX_METADATA_BYTES) {
                        throw SeekableZipException(
                            "ZIP local-header metadata exceeds $MAX_METADATA_BYTES bytes",
                        )
                    }
                    entries += entry
                    offset = checkedAdd(entry.dataOffset, entry.compressedSize)
                }

                CENTRAL_DIRECTORY_SIGNATURE,
                END_OF_CENTRAL_DIRECTORY_SIGNATURE,
                ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE,
                ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE,
                -> {
                    return SeekableZipArchive(entries, hasCentralDirectory = true)
                }

                else -> {
                    if (isApkSigningBlock(input, offset, fileSize)) {
                        return SeekableZipArchive(entries, hasCentralDirectory = true)
                    }
                    throw SeekableZipException(
                        "Unexpected ZIP signature 0x${signature.toString(16)} at offset $offset",
                    )
                }
            }
        }
    }

    /**
     * APK Signature Scheme v2+ inserts an APK Signing Block between the last entry payload and
     * the central directory. Its leading bytes are a size field rather than a ZIP signature, so
     * local-header traversal must recognize the complete outer envelope before stopping there.
     */
    private fun isApkSigningBlock(input: ChannelRandomAccessReader, blockOffset: Long, fileSize: Long): Boolean {
        val remainingSize = fileSize - blockOffset
        if (remainingSize < APK_SIGNING_BLOCK_MIN_TOTAL_SIZE + SIGNATURE_SIZE) return false

        input.position = blockOffset
        val blockSize = input.readSignedLongLittleEndian()
        if (blockSize < APK_SIGNING_BLOCK_MIN_SIZE) return false
        if (blockSize > remainingSize - LONG_SIZE - SIGNATURE_SIZE) return false

        val blockEnd = blockOffset + LONG_SIZE + blockSize
        input.position = blockEnd - APK_SIGNING_BLOCK_FOOTER_SIZE
        if (input.readSignedLongLittleEndian() != blockSize) return false

        val magic = ByteArray(APK_SIGNING_BLOCK_MAGIC.size).also(input::readFully)
        if (!magic.contentEquals(APK_SIGNING_BLOCK_MAGIC)) return false

        input.position = blockEnd
        return input.readUnsignedIntLittleEndian() == CENTRAL_DIRECTORY_SIGNATURE
    }

    private fun readLocalEntry(input: ChannelRandomAccessReader, localHeaderOffset: Long, fileSize: Long): SeekableZipEntry {
        // One bulk read per header: byte-at-a-time channel reads cost ~30 syscalls per entry.
        val header = input.readBytes(LOCAL_HEADER_REMAINING_SIZE)
        val flags = header.readUnsignedShortLittleEndian(2)
        val compressionMethod = header.readUnsignedShortLittleEndian(4)
        val crc = header.readUnsignedIntLittleEndian(10)
        var compressedSize = header.readUnsignedIntLittleEndian(14)
        var uncompressedSize = header.readUnsignedIntLittleEndian(18)
        val nameLength = header.readUnsignedShortLittleEndian(22)
        val extraLength = header.readUnsignedShortLittleEndian(24)

        if (flags and ENCRYPTED_FLAG != 0) {
            throw SeekableZipException("Encrypted ZIP entry is unsupported at offset $localHeaderOffset")
        }
        if (flags and DATA_DESCRIPTOR_FLAG != 0) {
            throw SeekableZipException("ZIP entry uses a data descriptor at offset $localHeaderOffset")
        }
        if (nameLength == 0) {
            throw SeekableZipException("ZIP entry has an empty name at offset $localHeaderOffset")
        }

        val metadataEnd = checkedAdd(input.position, nameLength.toLong(), extraLength.toLong())
        if (metadataEnd > fileSize) {
            throw SeekableZipException("ZIP entry metadata exceeds file size at offset $localHeaderOffset")
        }

        val nameBytes = ByteArray(nameLength).also(input::readFully)
        val extraBytes = ByteArray(extraLength).also(input::readFully)
        val nameCharset = if (flags and UTF8_FLAG != 0) StandardCharsets.UTF_8 else CP437
        val name = String(nameBytes, nameCharset)
        if ('\u0000' in name) {
            throw SeekableZipException("ZIP entry name contains a NUL byte at offset $localHeaderOffset")
        }

        if (compressedSize == UINT32_MAX || uncompressedSize == UINT32_MAX) {
            val zip64Sizes = readZip64Sizes(
                extraBytes = extraBytes,
                needsUncompressedSize = uncompressedSize == UINT32_MAX,
                needsCompressedSize = compressedSize == UINT32_MAX,
                localHeaderOffset = localHeaderOffset,
            )
            if (uncompressedSize == UINT32_MAX) uncompressedSize = zip64Sizes.uncompressedSize
            if (compressedSize == UINT32_MAX) compressedSize = zip64Sizes.compressedSize
        }

        if (compressionMethod == STORED_METHOD && compressedSize != uncompressedSize) {
            throw SeekableZipException("Stored ZIP entry has mismatched sizes: $name")
        }

        val dataEnd = checkedAdd(metadataEnd, compressedSize)
        if (dataEnd > fileSize) {
            throw SeekableZipException(
                "ZIP entry payload exceeds file size: $name, end=$dataEnd, fileSize=$fileSize",
            )
        }

        return SeekableZipEntry(
            name = name,
            localHeaderOffset = localHeaderOffset,
            dataOffset = metadataEnd,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            crc = crc,
            compressionMethod = compressionMethod,
            flags = flags,
        )
    }

    private fun readZip64Sizes(
        extraBytes: ByteArray,
        needsUncompressedSize: Boolean,
        needsCompressedSize: Boolean,
        localHeaderOffset: Long,
    ): Zip64Sizes {
        var offset = 0
        while (offset + EXTRA_FIELD_HEADER_SIZE <= extraBytes.size) {
            val headerId = extraBytes.readUnsignedShortLittleEndian(offset)
            val dataSize = extraBytes.readUnsignedShortLittleEndian(offset + 2)
            val dataOffset = offset + EXTRA_FIELD_HEADER_SIZE
            val dataEnd = dataOffset + dataSize
            if (dataEnd > extraBytes.size) {
                throw SeekableZipException("Malformed ZIP extra field at offset $localHeaderOffset")
            }

            if (headerId == ZIP64_EXTRA_FIELD_ID) {
                // APPNOTE 4.5.3: a local-header ZIP64 field carries the original (uncompressed)
                // size first, then the compressed size. Tolerate writers that emit only the
                // single masked value when just one size overflowed.
                val requiredSize =
                    if (needsUncompressedSize && needsCompressedSize) 2 * LONG_SIZE else LONG_SIZE
                if (dataSize < requiredSize) {
                    throw SeekableZipException("ZIP64 sizes are missing at offset $localHeaderOffset")
                }
                val uncompressedSize = if (needsUncompressedSize) {
                    extraBytes.readSignedLongLittleEndian(dataOffset)
                } else {
                    0L
                }
                val compressedSize = when {
                    !needsCompressedSize -> 0L

                    dataSize >= 2 * LONG_SIZE ->
                        extraBytes.readSignedLongLittleEndian(dataOffset + LONG_SIZE)

                    else -> extraBytes.readSignedLongLittleEndian(dataOffset)
                }
                if (uncompressedSize < 0 || compressedSize < 0) {
                    throw SeekableZipException("ZIP64 entry size exceeds supported range at offset $localHeaderOffset")
                }
                return Zip64Sizes(uncompressedSize, compressedSize)
            }
            offset = dataEnd
        }
        throw SeekableZipException("ZIP64 sizes are missing at offset $localHeaderOffset")
    }

    private fun ByteArray.readUnsignedShortLittleEndian(offset: Int): Int {
        if (offset < 0 || offset + SHORT_SIZE > size) {
            throw SeekableZipException("Truncated ZIP extra field")
        }
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl Byte.SIZE_BITS)
    }

    private fun ByteArray.readUnsignedIntLittleEndian(offset: Int): Long {
        if (offset < 0 || offset + INT_SIZE > size) {
            throw SeekableZipException("Truncated ZIP local header")
        }
        var result = 0L
        repeat(INT_SIZE) { index ->
            result = result or ((this[offset + index].toLong() and 0xFF) shl (index * Byte.SIZE_BITS))
        }
        return result
    }

    private fun ByteArray.readSignedLongLittleEndian(offset: Int): Long {
        if (offset < 0 || offset + LONG_SIZE > size) {
            throw SeekableZipException("Truncated ZIP64 extra field")
        }
        var result = 0L
        repeat(LONG_SIZE) { index ->
            result = result or ((this[offset + index].toLong() and 0xFF) shl (index * Byte.SIZE_BITS))
        }
        return result
    }

    private fun ChannelRandomAccessReader.readBytes(count: Int): ByteArray = ByteArray(count).also(::readFully)

    private fun ChannelRandomAccessReader.readUnsignedIntLittleEndian(): Long = readBytes(INT_SIZE).readUnsignedIntLittleEndian(0)

    private fun ChannelRandomAccessReader.readSignedLongLittleEndian(): Long = readBytes(LONG_SIZE).readSignedLongLittleEndian(0)

    private fun checkedAdd(vararg values: Long): Long {
        var result = 0L
        values.forEach { value ->
            if (value < 0 || result > Long.MAX_VALUE - value) {
                throw SeekableZipException("ZIP offset overflow")
            }
            result += value
        }
        return result
    }

    private data class Zip64Sizes(val uncompressedSize: Long, val compressedSize: Long)

    private class ChannelRandomAccessReader(private val channel: SeekableByteChannel) : Closeable {
        val length: Long
            get() = channel.size()

        var position: Long
            get() = channel.position()
            set(value) {
                channel.position(value)
            }

        fun readFully(bytes: ByteArray) {
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                when (channel.read(buffer)) {
                    -1 -> throw EOFException("Unexpected end of ZIP local header")
                    0 -> continue
                }
            }
        }

        override fun close() = channel.close()
    }

    private companion object {
        const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50L
        const val CENTRAL_DIRECTORY_SIGNATURE = 0x02014B50L
        const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50L
        const val ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06064B50L
        const val ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE = 0x07064B50L
        const val ZIP64_EXTRA_FIELD_ID = 0x0001
        const val UINT32_MAX = 0xFFFF_FFFFL
        const val ENCRYPTED_FLAG = 1 shl 0
        const val DATA_DESCRIPTOR_FLAG = 1 shl 3
        const val UTF8_FLAG = 1 shl 11
        const val STORED_METHOD = 0
        const val MAX_ENTRY_COUNT = 100_000
        const val MAX_METADATA_BYTES = 64L * 1024L * 1024L
        const val SIGNATURE_SIZE = 4L
        const val LOCAL_FILE_HEADER_SIZE = 30L
        const val LOCAL_HEADER_REMAINING_SIZE = 26
        const val EXTRA_FIELD_HEADER_SIZE = 4
        const val SHORT_SIZE = 2
        const val INT_SIZE = 4
        const val LONG_SIZE = 8
        const val APK_SIGNING_BLOCK_MIN_SIZE = 24L
        const val APK_SIGNING_BLOCK_MIN_TOTAL_SIZE = LONG_SIZE + APK_SIGNING_BLOCK_MIN_SIZE
        const val APK_SIGNING_BLOCK_FOOTER_SIZE = LONG_SIZE + 16L
        val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(StandardCharsets.US_ASCII)
        val CP437: Charset = runCatching { Charset.forName("Cp437") }
            .getOrDefault(StandardCharsets.ISO_8859_1)
    }
}
