// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.requireSupportedZipCompressionMethod
import org.apache.commons.compress.archivers.EntryStreamOffsets
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

internal class CommonsZipException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Opens ZIP archives through Commons Compress using an explicit seekable channel.
 *
 * Callers own the returned [ZipFile]. If opening fails, the channel is closed here.
 */
internal class CommonsZipFileProvider {
    fun open(path: String): ZipFile = open(File(path), ignoreLocalFileHeaders = false)

    fun open(file: File): ZipFile = open(file, ignoreLocalFileHeaders = false)

    fun open(file: DataEntity.FileEntity): ZipFile = open(file, ignoreLocalFileHeaders = false)

    /**
     * Opens only the central directory up front. Entry local headers are resolved lazily if their
     * payloads are requested, which keeps metadata-only analysis fast for large APKs.
     */
    fun openMetadata(path: String): ZipFile =
        open(File(path), ignoreLocalFileHeaders = true)

    fun openMetadata(file: File): ZipFile =
        open(file, ignoreLocalFileHeaders = true)

    fun openMetadata(file: DataEntity.FileEntity): ZipFile =
        open(file, ignoreLocalFileHeaders = true)

    /** Opens an entry payload after enforcing InstallerX's STORE/DEFLATE-only policy. */
    fun openEntry(zipFile: ZipFile, entry: ZipArchiveEntry): InputStream {
        validateEntry(entry)
        return synchronized(zipFile) {
            zipFile.getInputStream(entry)
        }
    }

    /** Resolves the raw byte range of a stored entry without reading its payload. */
    fun resolveStoredDataRange(zipFile: ZipFile, entry: ZipArchiveEntry): StoredDataRange? {
        if (entry.method != java.util.zip.ZipEntry.STORED ||
            entry.size <= 0L ||
            entry.size != entry.compressedSize
        ) {
            return null
        }

        return synchronized(zipFile) {
            // Commons resolves dataOffset lazily from the local header when metadata-only mode is used.
            zipFile.getRawInputStream(entry)?.close()
            entry.dataOffset
                .takeUnless { it == EntryStreamOffsets.OFFSET_UNKNOWN }
                ?.let { offset -> StoredDataRange(offset, entry.compressedSize) }
        }
    }

    fun validateEntries(entries: Iterable<ZipArchiveEntry>) {
        entries.forEach(::validateEntry)
    }

    fun validateEntry(entry: ZipArchiveEntry) {
        requireSupportedZipCompressionMethod(entry.method, entry.name)
    }

    private fun open(file: File, ignoreLocalFileHeaders: Boolean): ZipFile {
        val channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
        return open(channel, file.path, ignoreLocalFileHeaders)
    }

    private fun open(file: DataEntity.FileEntity, ignoreLocalFileHeaders: Boolean): ZipFile =
        open(file.openChannel(), file.path, ignoreLocalFileHeaders)

    private fun open(
        channel: java.nio.channels.SeekableByteChannel,
        displayName: String,
        ignoreLocalFileHeaders: Boolean
    ): ZipFile {
        return try {
            ZipFile.builder()
                .setSeekableByteChannel(channel)
                .setIgnoreLocalFileHeader(ignoreLocalFileHeaders)
                .get()
        } catch (error: Exception) {
            try {
                channel.close()
            } catch (closeError: Exception) {
                error.addSuppressed(closeError)
            }
            throw CommonsZipException("Failed to open ZIP archive: $displayName", error)
        }
    }
}
