// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.model.source.SeekableZipArchive
import com.rosan.installer.domain.engine.model.source.SeekableZipEntry
import com.rosan.installer.domain.engine.model.source.requireSupportedZipCompressionMethod
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry

enum class UnifiedZipBackend {
    COMMONS_CENTRAL_DIRECTORY,
    LOCAL_FILE_HEADERS
}

internal val DataType.allowsLocalHeaderFallback: Boolean
    get() = this != DataType.MODULE_ZIP &&
            this != DataType.MIXED_MODULE_ZIP &&
            this != DataType.MIXED_MODULE_APK

class UnifiedZipEntry internal constructor(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val crc: Long,
    val compressionMethod: Int,
    internal val storedDataRange: StoredDataRange?,
    internal val source: UnifiedZipEntrySource
) {
    override fun toString(): String = name
}

internal data class StoredDataRange(
    val offset: Long,
    val length: Long
)

internal sealed interface UnifiedZipEntrySource {
    data class Commons(val entry: ZipArchiveEntry) : UnifiedZipEntrySource

    data class LocalHeader(val entry: SeekableZipEntry) : UnifiedZipEntrySource
}

/**
 * A single ZIP access surface for Android package analysis.
 *
 * The selected backend is intentionally hidden from callers. Entry streams and install-time
 * [DataEntity] instances keep using the same backend that produced the metadata view.
 */
class UnifiedZipFile internal constructor(
    val file: DataEntity.FileEntity,
    val backend: UnifiedZipBackend,
    val entries: List<UnifiedZipEntry>,
    private val commonsZipFile: ZipFile?,
    private val commonsZipFileProvider: CommonsZipFileProvider
) : Closeable {
    private val entriesByName = entries.groupBy(UnifiedZipEntry::name)
    private var closed = false

    fun getEntry(name: String): UnifiedZipEntry? = entriesByName[name]?.firstOrNull()

    fun openEntry(entry: UnifiedZipEntry): InputStream {
        checkOpen()
        require(entries.any { it === entry }) { "ZIP entry does not belong to ${file.path}: ${entry.name}" }

        return when (val source = entry.source) {
            is UnifiedZipEntrySource.Commons -> commonsZipFileProvider.openEntry(
                requireNotNull(commonsZipFile),
                source.entry
            )

            is UnifiedZipEntrySource.LocalHeader -> source.entry
                .toDataEntity(file)
                .getInputStream()
        }
    }

    fun toDataEntity(
        entry: UnifiedZipEntry,
        parent: DataEntity.FileEntity
    ): DataEntity {
        require(entries.any { it === entry }) { "ZIP entry does not belong to ${file.path}: ${entry.name}" }

        val storedDataRange = entry.storedDataRange ?: resolveCommonsStoredDataRange(entry)
        if (entry.compressionMethod == ZipEntry.STORED &&
            entry.size == entry.compressedSize &&
            storedDataRange != null &&
            parent is DataEntity.FileDescriptorEntity
        ) {
            runCatching {
                parent.subrange(
                    relativeOffset = storedDataRange.offset,
                    subrangeLength = storedDataRange.length
                )
            }.onFailure { error ->
                Timber.d(error, "Stored ZIP entry range is outside the retained descriptor: ${entry.name}")
            }.getOrNull()?.let { descriptorEntry ->
                Timber.d(
                    "Using stored ZIP entry descriptor range without extraction: " +
                            "name=${entry.name}, offset=${descriptorEntry.startOffset}, " +
                            "length=${descriptorEntry.length}"
                )
                return descriptorEntry
            }
        }

        return when (val source = entry.source) {
            is UnifiedZipEntrySource.Commons -> DataEntity.ZipFileEntity(entry.name, parent)
            is UnifiedZipEntrySource.LocalHeader -> source.entry.toDataEntity(parent)
        }
    }

    private fun resolveCommonsStoredDataRange(entry: UnifiedZipEntry): StoredDataRange? {
        val source = entry.source as? UnifiedZipEntrySource.Commons ?: return null
        val zipFile = commonsZipFile ?: return null
        return runCatching {
            commonsZipFileProvider.resolveStoredDataRange(zipFile, source.entry)
        }.onFailure { error ->
            Timber.d(error, "Unable to resolve stored ZIP entry range: ${entry.name}")
        }.getOrNull()
    }

    override fun close() {
        if (closed) return
        closed = true
        commonsZipFile?.close()
    }

    private fun checkOpen() {
        check(!closed) { "ZIP file is already closed: ${file.path}" }
    }

    private fun SeekableZipEntry.toDataEntity(parent: DataEntity.FileEntity) =
        DataEntity.SeekableZipEntryEntity(
            name = name,
            parent = parent,
            dataOffset = dataOffset,
            compressedSize = compressedSize,
            uncompressedSize = uncompressedSize,
            compressionMethod = compressionMethod,
            crc = crc
        )
}

/**
 * Opens Android package ZIPs through Commons Compress.
 *
 * Local-file-header traversal is only a recovery path for archives whose central directory is
 * missing or cannot be opened. Valid central-directory archives do not pay the cost of a second
 * full metadata scan.
 */
class UnifiedZipFileProvider internal constructor(
    private val commonsZipFileProvider: CommonsZipFileProvider,
    private val seekableZipReader: SeekableZipReader
) {
    fun open(
        path: String,
        allowLocalHeaderFallback: Boolean = true
    ): UnifiedZipFile = open(File(path), allowLocalHeaderFallback)

    fun open(
        file: File,
        allowLocalHeaderFallback: Boolean = true
    ): UnifiedZipFile = open(DataEntity.FileEntity(file.path), allowLocalHeaderFallback)

    fun open(
        file: DataEntity.FileEntity,
        allowLocalHeaderFallback: Boolean = true
    ): UnifiedZipFile {
        val commonsResult = runCatching { openCommonsView(file) }
        commonsResult.getOrNull()?.let { commonsView ->
            Timber.d(
                "Unified ZIP selected central-directory backend: path=${file.path}, " +
                        "entries=${commonsView.entries.size}"
            )
            return commonsView.toUnifiedZipFile(file, commonsZipFileProvider)
        }

        if (!allowLocalHeaderFallback) throw commonsResult.exceptionOrNull()!!

        val localResult = runCatching { seekableZipReader.read(file) }
        val localView = localResult.getOrNull()
        if (localView == null) {
            val error = commonsResult.exceptionOrNull()!!
            localResult.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }

        Timber.d(
            "Unified ZIP selected local-header fallback: path=${file.path}, " +
                    "reason=${commonsResult.exceptionOrNull()?.message ?: "central directory unavailable"}"
        )
        return localView.toUnifiedZipFile(file, commonsZipFileProvider)
    }

    private fun openCommonsView(file: DataEntity.FileEntity): CommonsView {
        val zipFile = commonsZipFileProvider.openMetadata(file)
        return try {
            CommonsView(zipFile, zipFile.entries.asSequence().toList())
        } catch (error: Exception) {
            zipFile.close()
            throw error
        }
    }

    private fun CommonsView.toUnifiedZipFile(
        file: DataEntity.FileEntity,
        provider: CommonsZipFileProvider
    ): UnifiedZipFile = try {
        val unifiedEntries = entries.map { entry ->
            requireSupportedZipCompressionMethod(entry.method, entry.name)
            UnifiedZipEntry(
                name = entry.name,
                isDirectory = entry.isDirectory,
                size = entry.size,
                compressedSize = entry.compressedSize,
                crc = entry.crc,
                compressionMethod = entry.method,
                storedDataRange = null,
                source = UnifiedZipEntrySource.Commons(entry)
            )
        }
        UnifiedZipFile(
            file = file,
            backend = UnifiedZipBackend.COMMONS_CENTRAL_DIRECTORY,
            entries = unifiedEntries,
            commonsZipFile = zipFile,
            commonsZipFileProvider = provider
        )
    } catch (error: Exception) {
        zipFile.close()
        throw error
    }

    private fun SeekableZipArchive.toUnifiedZipFile(
        file: DataEntity.FileEntity,
        provider: CommonsZipFileProvider
    ): UnifiedZipFile {
        val unifiedEntries = entries.map { entry ->
            requireSupportedZipCompressionMethod(entry.compressionMethod, entry.name)
            UnifiedZipEntry(
                name = entry.name,
                isDirectory = entry.isDirectory,
                size = entry.uncompressedSize,
                compressedSize = entry.compressedSize,
                crc = entry.crc,
                compressionMethod = entry.compressionMethod,
                storedDataRange = entry.storedDataRange(),
                source = UnifiedZipEntrySource.LocalHeader(entry)
            )
        }
        return UnifiedZipFile(
            file = file,
            backend = UnifiedZipBackend.LOCAL_FILE_HEADERS,
            entries = unifiedEntries,
            commonsZipFile = null,
            commonsZipFileProvider = provider
        )
    }

    private fun SeekableZipEntry.storedDataRange(): StoredDataRange? =
        if (compressionMethod == ZipEntry.STORED && compressedSize == uncompressedSize) {
            StoredDataRange(dataOffset, compressedSize)
        } else {
            null
        }

    private data class CommonsView(
        val zipFile: ZipFile,
        val entries: List<ZipArchiveEntry>
    )
}
