// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.model.source.SeekableZipArchive
import com.rosan.installer.domain.engine.model.source.SeekableZipEntry
import com.rosan.installer.domain.engine.model.source.ZipEntryMetadata
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.Collections
import java.util.IdentityHashMap
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
    private val entriesByName = HashMap<String, UnifiedZipEntry>().apply {
        this@UnifiedZipFile.entries.forEach { entry -> putIfAbsent(entry.name, entry) }
    }
    private val ownedEntries = Collections.newSetFromMap(IdentityHashMap<UnifiedZipEntry, Boolean>()).apply {
        addAll(entries)
    }
    private var closed = false

    fun getEntry(name: String): UnifiedZipEntry? = entriesByName[name]

    fun openEntry(entry: UnifiedZipEntry): InputStream {
        checkOpen()
        require(entry in ownedEntries) { "ZIP entry does not belong to ${file.path}: ${entry.name}" }

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
        require(entry in ownedEntries) { "ZIP entry does not belong to ${file.path}: ${entry.name}" }

        val dataRange = entry.storedDataRange ?: resolveCommonsDataRange(entry)
        if (entry.compressionMethod == ZipEntry.STORED &&
            entry.size == entry.compressedSize &&
            dataRange != null &&
            parent is DataEntity.FileDescriptorEntity
        ) {
            runCatching {
                parent.subrange(
                    relativeOffset = dataRange.offset,
                    subrangeLength = dataRange.length,
                    zipEntryMetadata = entry.toMetadata(),
                    archiveEntryName = entry.name
                )
            }.onFailure { error ->
                Timber.d(error, "Stored ZIP entry range is outside the retained descriptor: ${entry.name}")
            }.getOrNull()?.let { descriptorEntry ->
                Timber.d(
                    "Using stored ZIP entry descriptor range without extraction: " +
                            "container=${parent.path}, entry=${entry.name}, backend=$backend, " +
                            "relativeOffset=${dataRange.offset}, " +
                            "descriptorOffset=${descriptorEntry.startOffset}, " +
                            "length=${descriptorEntry.length}"
                )
                return descriptorEntry
            }
        }

        if (dataRange != null &&
            entry.size >= 0L &&
            entry.compressedSize >= 0L &&
            entry.crc >= 0L &&
            (entry.compressionMethod == ZipEntry.STORED || entry.compressionMethod == ZipEntry.DEFLATED)
        ) {
            return DataEntity.SeekableZipEntryEntity(
                name = entry.name,
                parent = parent,
                dataOffset = dataRange.offset,
                compressedSize = entry.compressedSize,
                uncompressedSize = entry.size,
                compressionMethod = entry.compressionMethod,
                crc = entry.crc
            )
        }

        return when (val source = entry.source) {
            is UnifiedZipEntrySource.Commons -> DataEntity.ZipFileEntity(
                name = entry.name,
                parent = parent,
                zipEntryMetadata = entry.toMetadata()
            )
            is UnifiedZipEntrySource.LocalHeader -> source.entry.toDataEntity(parent)
        }
    }

    private fun resolveCommonsDataRange(entry: UnifiedZipEntry): StoredDataRange? {
        val source = entry.source as? UnifiedZipEntrySource.Commons ?: return null
        val zipFile = commonsZipFile ?: return null
        return runCatching {
            commonsZipFileProvider.resolveDataRange(zipFile, source.entry)
        }.onFailure { error ->
            Timber.d(error, "Unable to resolve ZIP entry data range: ${entry.name}")
        }.getOrNull()
    }

    private fun UnifiedZipEntry.toMetadata() = ZipEntryMetadata(
        uncompressedSize = size,
        compressedSize = compressedSize,
        crc = crc,
        compressionMethod = compressionMethod
    )

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
            if (allowLocalHeaderFallback) {
                recoverNestedCentralDirectoryView(file, commonsView)?.let { return it }
            }
            Timber.d(
                "Unified ZIP selected central-directory backend: ${file.archiveLogContext()}, " +
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
            "Unified ZIP selected local-header fallback: ${file.archiveLogContext()}, " +
                    "reason=${commonsResult.exceptionOrNull()?.message ?: "central directory unavailable"}"
        )
        return localView.toUnifiedZipFile(file, commonsZipFileProvider)
    }

    /**
     * A truncated container whose last stored entry is itself a ZIP/APK still opens through
     * Commons: the nested entry's central directory is found instead of the missing outer one.
     * Such a view never covers the start of the file, so a suffix-only central directory triggers
     * one local-header scan. The scan wins when it proves the outer file starts with a clean chain
     * of local headers. A central-directory marker after that chain may be the truncated outer
     * directory, so its presence alone must not discard the recoverable entries.
     */
    private fun recoverNestedCentralDirectoryView(
        file: DataEntity.FileEntity,
        commonsView: CommonsView
    ): UnifiedZipFile? {
        if (!commonsView.isSuffixOnlyView()) return null

        val localView = runCatching { seekableZipReader.read(file) }.getOrNull() ?: return null
        if (localView.entries.isEmpty()) return null

        Timber.w(
            "Unified ZIP recovered outer local headers hidden by a nested central directory: " +
                    "${file.archiveLogContext()}, nestedEntries=${commonsView.entries.size}, " +
                    "outerEntries=${localView.entries.size}"
        )
        commonsView.zipFile.close()
        return localView.toUnifiedZipFile(file, commonsZipFileProvider)
    }

    private fun CommonsView.isSuffixOnlyView(): Boolean =
        zipFile.firstLocalFileHeaderOffset > 0L ||
                entries.isNotEmpty() && entries.minOf { it.localHeaderOffset } > 0L

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
        // Compression methods are validated when an entry is opened, not here: one exotic entry
        // (e.g. a bzip2 readme) must not make the whole container unreadable.
        val unifiedEntries = entries.map { entry ->
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

private fun DataEntity.FileEntity.archiveLogContext(): String = buildString {
    append("path=")
    append(path)
    if (this@archiveLogContext is DataEntity.FileDescriptorEntity) {
        archiveEntryName?.let {
            append(", entry=")
            append(it)
        }
        append(", rangeOffset=")
        append(startOffset)
        append(", rangeLength=")
        append(this@archiveLogContext.length)
    }
}
