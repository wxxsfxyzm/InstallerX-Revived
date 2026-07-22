// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal data class SeekableZipArchive(
    val entries: List<SeekableZipEntry>,
    val hasCentralDirectory: Boolean
)

internal data class SeekableZipEntry(
    val name: String,
    val localHeaderOffset: Long,
    val dataOffset: Long,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val crc: Long,
    val compressionMethod: Int,
    val flags: Int
) {
    val isDirectory: Boolean
        get() = name.endsWith('/')
}

internal class SeekableZipException(message: String, cause: Throwable? = null) : IOException(message, cause)

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
        val entries = mutableListOf<SeekableZipEntry>()
        var offset = 0L

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
                    entries += entry
                    offset = checkedAdd(entry.dataOffset, entry.compressedSize)
                }

                CENTRAL_DIRECTORY_SIGNATURE,
                END_OF_CENTRAL_DIRECTORY_SIGNATURE,
                ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE,
                ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE -> {
                    return SeekableZipArchive(entries, hasCentralDirectory = true)
                }

                else -> throw SeekableZipException(
                    "Unexpected ZIP signature 0x${signature.toString(16)} at offset $offset"
                )
            }
        }
    }

    private fun readLocalEntry(
        input: ChannelRandomAccessReader,
        localHeaderOffset: Long,
        fileSize: Long
    ): SeekableZipEntry {
        input.readUnsignedShortLittleEndian() // version needed to extract
        val flags = input.readUnsignedShortLittleEndian()
        val compressionMethod = input.readUnsignedShortLittleEndian()
        input.readUnsignedShortLittleEndian() // modification time
        input.readUnsignedShortLittleEndian() // modification date
        val crc = input.readUnsignedIntLittleEndian()
        var compressedSize = input.readUnsignedIntLittleEndian()
        var uncompressedSize = input.readUnsignedIntLittleEndian()
        val nameLength = input.readUnsignedShortLittleEndian()
        val extraLength = input.readUnsignedShortLittleEndian()

        if (flags and ENCRYPTED_FLAG != 0) {
            throw SeekableZipException("Encrypted ZIP entry is unsupported at offset $localHeaderOffset")
        }
        if (flags and DATA_DESCRIPTOR_FLAG != 0) {
            throw SeekableZipException(
                "ZIP entry uses a data descriptor and cannot be seeked safely at offset $localHeaderOffset"
            )
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
                localHeaderOffset = localHeaderOffset
            )
            if (uncompressedSize == UINT32_MAX) uncompressedSize = zip64Sizes.uncompressedSize
            if (compressedSize == UINT32_MAX) compressedSize = zip64Sizes.compressedSize
        }

        if (compressionMethod == STORED_METHOD && compressedSize != uncompressedSize) {
            throw SeekableZipException("Stored ZIP entry has mismatched sizes: $name")
        }

        val dataOffset = metadataEnd
        val dataEnd = checkedAdd(dataOffset, compressedSize)
        if (dataEnd > fileSize) {
            throw SeekableZipException(
                "ZIP entry payload exceeds file size: $name, end=$dataEnd, fileSize=$fileSize"
            )
        }

        return SeekableZipEntry(
            name = name,
            localHeaderOffset = localHeaderOffset,
            dataOffset = dataOffset,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            crc = crc,
            compressionMethod = compressionMethod,
            flags = flags
        )
    }

    private fun readZip64Sizes(
        extraBytes: ByteArray,
        needsUncompressedSize: Boolean,
        needsCompressedSize: Boolean,
        localHeaderOffset: Long
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
                var valueOffset = dataOffset
                val uncompressedSize = if (needsUncompressedSize) {
                    extraBytes.readSignedLongLittleEndian(valueOffset).also { valueOffset += LONG_SIZE }
                } else {
                    0L
                }
                val compressedSize = if (needsCompressedSize) {
                    extraBytes.readSignedLongLittleEndian(valueOffset)
                } else {
                    0L
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

    private fun ChannelRandomAccessReader.readUnsignedShortLittleEndian(): Int {
        val low = read()
        val high = read()
        if (low < 0 || high < 0) throw SeekableZipException("Unexpected end of ZIP local header")
        return low or (high shl Byte.SIZE_BITS)
    }

    private fun ChannelRandomAccessReader.readUnsignedIntLittleEndian(): Long {
        var result = 0L
        repeat(INT_SIZE) { index ->
            val value = read()
            if (value < 0) throw SeekableZipException("Unexpected end of ZIP local header")
            result = result or (value.toLong() shl (index * Byte.SIZE_BITS))
        }
        return result
    }

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

    private data class Zip64Sizes(
        val uncompressedSize: Long,
        val compressedSize: Long
    )

    private class ChannelRandomAccessReader(
        private val channel: SeekableByteChannel
    ) : Closeable {
        private val singleByte = ByteBuffer.allocate(1)

        val length: Long
            get() = channel.size()

        var position: Long
            get() = channel.position()
            set(value) {
                channel.position(value)
            }

        fun read(): Int {
            singleByte.clear()
            while (true) {
                when (val count = channel.read(singleByte)) {
                    -1 -> return -1
                    0 -> continue
                    else -> {
                        check(count == 1)
                        return singleByte.array()[0].toInt() and 0xFF
                    }
                }
            }
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
        const val MAX_ENTRY_COUNT = 10_000
        const val SIGNATURE_SIZE = 4L
        const val EXTRA_FIELD_HEADER_SIZE = 4
        const val SHORT_SIZE = 2
        const val INT_SIZE = 4
        const val LONG_SIZE = 8
        val CP437: Charset = runCatching { Charset.forName("Cp437") }
            .getOrDefault(StandardCharsets.ISO_8859_1)
    }
}
