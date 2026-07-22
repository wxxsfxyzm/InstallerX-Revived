// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import android.system.OsConstants
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.data.session.util.copyToWithProgress
import com.rosan.installer.data.session.util.getRealPathFromUri
import com.rosan.installer.data.session.util.pathUnify
import com.rosan.installer.data.session.util.transferWithProgress
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.session.exception.ResolveException
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.ResolveErrorType
import com.rosan.installer.domain.session.model.ResolveResult
import com.rosan.installer.domain.session.repository.NetworkResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class SourceResolver(
    private val context: Context,
    private val networkResolver: NetworkResolver,
    private val cacheDirectory: String,
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) {
    private val closeables = mutableListOf<Closeable>()

    fun getTrackedCloseables(): List<Closeable> = closeables

    suspend fun resolve(intent: Intent): ResolveResult {
        val uris = extractUris(intent)
        Timber.d("resolve: URIs extracted from intent (${uris.size}).")

        val data = mutableListOf<DataEntity>()
        for (uri in uris) {
            // Check cancellation between items
            if (!currentCoroutineContext().isActive) throw CancellationException()
            data.addAll(resolveSingleUri(uri))
        }

        // Return the packaged result
        return ResolveResult(
            uris = uris.map { it.toString() },
            data = data
        )
    }

    private fun extractUris(intent: Intent): List<Uri> {
        val action = intent.action
        val uris = mutableListOf<Uri>()

        when (action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()

                // 1. Prioritize HTTP/HTTPS URLs (Fixes Chrome sharing screenshot + URL)
                if (!text.isNullOrBlank() && (text.startsWith("http", true) || text.startsWith("https", true))) {
                    try {
                        val textUri = text.toUri()
                        if (!textUri.scheme.isNullOrBlank()) uris.add(textUri)
                    } catch (_: Exception) {
                        Timber.w("Failed to parse EXTRA_TEXT as URI: $text")
                    }
                }

                // 2. Fallback to Stream/ClipData if no URL found (Handles file sharing, including text files like logcat.txt)
                if (uris.isEmpty()) {
                    val streamUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)

                    if (streamUri != null) uris.add(streamUri)

                    // 3. Check ClipData (Common in modern Android sharing)
                    if (uris.isEmpty()) {
                        intent.clipData?.let { clip ->
                            for (i in 0 until clip.itemCount) {
                                clip.getItemAt(i).uri?.let { uris.add(it) }
                            }
                        }
                    }

                    // 4. Fallback to EXTRA_TEXT only if no file stream was found and type indicates text
                    if (uris.isEmpty() && intent.type?.startsWith("text/") == true) {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                        if (!text.isNullOrBlank()) {
                            try {
                                // Attempt to parse text as a URI
                                val textUri = text.toUri()
                                // Simple check to see if it looks like a valid URI scheme (http, file, content, etc.)
                                if (!textUri.scheme.isNullOrBlank()) {
                                    uris.add(textUri)
                                } else {
                                    Timber.w("Ignored plain text extra (no scheme): $text")
                                }
                            } catch (_: Exception) {
                                Timber.w("Failed to parse EXTRA_TEXT as URI: $text")
                            }
                        }
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val streams = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                streams?.filterNotNull()?.let { uris.addAll(it) }

                if (uris.isEmpty()) {
                    intent.clipData?.let { clip ->
                        for (i in 0 until clip.itemCount) {
                            clip.getItemAt(i).uri?.let { uris.add(it) }
                        }
                    }
                }
            }

            else -> {
                intent.data?.let { uris.add(it) }
                if (uris.isEmpty()) {
                    intent.clipData?.let { clip ->
                        if (clip.itemCount > 0) clip.getItemAt(0).uri?.let { uris.add(it) }
                    }
                }
            }
        }

        if (uris.isEmpty()) throw ResolveException(
            errorType = ResolveErrorType.GENERIC_FAILED,
            message = "action: $action, uri: $uris"
        )
        return uris
    }

    private suspend fun resolveSingleUri(uri: Uri): List<DataEntity> {
        Timber.d("Source URI: $uri")

        // Handle null scheme (unlikely but safe to handle)
        val scheme = uri.scheme?.lowercase() ?: return emptyList()

        return when (scheme) {
            "file" -> {
                val path = uri.path ?: throw Exception("Invalid file URI: $uri")
                Timber.d("Resolving direct file URI: $path")
                listOf(DataEntity.FileEntity(path).apply { source = DataEntity.FileEntity(path) })
            }

            "content" -> resolveContentUri(uri)

            "http", "https" -> {
                if (!AppConfig.isInternetAccessEnabled) {
                    Timber.d("Internet access is disabled in app settings. Aborting network request.")
                    throw ResolveException(
                        errorType = ResolveErrorType.NO_INTERNET_ACCESS,
                        message = "No internet access to download files."
                    )
                }

                networkResolver.resolve(uri, cacheDirectory, progressFlow)
            }

            else -> throw ResolveException(
                errorType = ResolveErrorType.GENERIC_FAILED,
                message = "Unsupported scheme: $scheme, uris: $uri"
            )
        }
    }

    private suspend fun resolveContentUri(uri: Uri): List<DataEntity> {
        val providerAsset = try {
            openUnstableProviderAsset(uri)
        } catch (e: SecurityException) {
            val message = e.message.orEmpty()
            val isUriPermissionDenial =
                message.contains("grantUriPermission", ignoreCase = true) ||
                        message.contains("Permission Denial", ignoreCase = true) ||
                        message.contains("not exported", ignoreCase = true)

            if (isUriPermissionDenial) {
                Timber.w(e, "Content URI permission denied. Installer is likely hidden from initiator.")
                throw ResolveException(
                    errorType = ResolveErrorType.INITIATOR_NOT_VISIBLE,
                    cause = e
                )
            }

            // Rethrow if it doesn't match the signature or initiator is unknown
            throw e
        }

        var retainProviderAsset = false
        try {
            val afd = providerAsset.descriptor
            val fd = afd.parcelFileDescriptor.fd
            val procPath = "/proc/${Os.getpid()}/fd/$fd"
            val realPath = runCatching {
                Os.readlink(procPath).getRealPathFromUri(uri).pathUnify()
            }.getOrDefault("")

            val range = resolveDescriptorRange(providerAsset)
            if (range != null) {
                val displayPath = realPath.ifBlank { uri.lastPathSegment ?: uri.toString() }
                val entity = DataEntity.FileDescriptorEntity(
                    path = displayPath,
                    startOffset = range.offset,
                    length = range.length,
                    channelFactory = { providerAsset.openChannel(range) },
                    descriptorFactory = providerAsset::duplicateDescriptor
                ).apply {
                    source = DataEntity.FileEntity(displayPath)
                }
                closeables += providerAsset
                retainProviderAsset = true
                Timber.d(
                    "Using retained content descriptor without cache or path reopen: " +
                            "path=$displayPath, offset=${range.offset}, length=${range.length}"
                )
                return listOf(entity)
            }

            Timber.d("Content descriptor is not seekable ($realPath). Falling back to cache.")
            return cacheStream(uri, afd, realPath)
        } finally {
            if (!retainProviderAsset) providerAsset.close()
        }
    }

    private fun openUnstableProviderAsset(uri: Uri): ProviderAsset {
        // ContentResolver.openAssetFileDescriptor() upgrades a successful open to a stable
        // provider reference and keeps it until the returned descriptor is closed. Open through
        // an unstable client instead, then release the client immediately and retain only the AFD.
        val client = context.contentResolver.acquireUnstableContentProviderClient(uri)
            ?: throw IOException("Cannot acquire content provider: $uri")

        return try {
            val descriptor = client.openAssetFile(uri, "r", null)
                ?: throw IOException("Cannot open file descriptor: $uri")
            ProviderAsset(descriptor)
        } catch (e: Exception) {
            if (e is RemoteException) throw IOException("Content provider died while opening: $uri", e)
            throw e
        } finally {
            client.close()
        }
    }

    private fun resolveDescriptorRange(providerAsset: ProviderAsset): ContentRange? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Timber.d("Descriptor-backed APK parsing requires Android 9 or newer.")
            return null
        }

        return runCatching {
            val afd = providerAsset.descriptor
            val stat = Os.fstat(afd.fileDescriptor)
            if (!OsConstants.S_ISREG(stat.st_mode)) throw IOException("Descriptor is not a regular file")

            val offset = afd.startOffset
            if (offset < 0L) throw IOException("Descriptor has a negative start offset")
            val length = if (afd.declaredLength != AssetFileDescriptor.UNKNOWN_LENGTH) {
                afd.declaredLength
            } else {
                stat.st_size - offset
            }
            if (length <= 0L) throw IOException("Descriptor length is unknown or empty")
            if (stat.st_size > 0L && (offset > stat.st_size || length > stat.st_size - offset)) {
                throw IOException("Descriptor range exceeds the underlying file")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                (offset != 0L || stat.st_size <= 0L || length != stat.st_size)
            ) {
                throw IOException("Android 9 and 10 require a whole-file descriptor")
            }

            ContentRange(offset, length).also { range ->
                providerAsset.openChannel(range).use { channel ->
                    val firstByte = ByteBuffer.allocate(1)
                    while (true) {
                        when (channel.read(firstByte)) {
                            -1 -> throw IOException("Descriptor is empty")
                            0 -> continue
                            else -> break
                        }
                    }
                }
            }
        }.onFailure { error ->
            Timber.d(error, "Content descriptor cannot provide positional reads.")
        }.getOrNull()
    }

    private data class ContentRange(val offset: Long, val length: Long)

    private class ProviderAsset(
        val descriptor: AssetFileDescriptor
    ) : Closeable {
        private val closed = AtomicBoolean(false)

        fun openChannel(range: ContentRange): SeekableByteChannel {
            ensureOpen()
            val duplicate = ParcelFileDescriptor.dup(descriptor.fileDescriptor)
            val input = ParcelFileDescriptor.AutoCloseInputStream(duplicate)
            return try {
                PositionalFileDescriptorChannel(input, range.offset, range.length)
            } catch (error: Exception) {
                input.close()
                throw error
            }
        }

        fun duplicateDescriptor(): DataEntity.OwnedFileDescriptor {
            ensureOpen()
            val duplicate = ParcelFileDescriptor.dup(descriptor.fileDescriptor)
            return DataEntity.OwnedFileDescriptor(duplicate.fileDescriptor, duplicate::close)
        }

        private fun ensureOpen() {
            if (closed.get()) throw IOException("Content descriptor is already closed")
        }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            descriptor.close()
        }
    }

    private class PositionalFileDescriptorChannel(
        private val input: ParcelFileDescriptor.AutoCloseInputStream,
        private val startOffset: Long,
        private val rangeLength: Long
    ) : SeekableByteChannel {
        private val channel = input.channel
        private var position = 0L
        private val closed = AtomicBoolean(false)

        override fun read(destination: ByteBuffer): Int {
            ensureOpen()
            if (position >= rangeLength) return -1
            if (!destination.hasRemaining()) return 0

            val bytesToRead = min(destination.remaining().toLong(), rangeLength - position).toInt()
            val originalLimit = destination.limit()
            return try {
                destination.limit(destination.position() + bytesToRead)
                channel.read(destination, startOffset + position).also { count ->
                    if (count > 0) position += count
                }
            } finally {
                destination.limit(originalLimit)
            }
        }

        override fun write(source: ByteBuffer): Int = throw NonWritableChannelException()

        override fun position(): Long {
            ensureOpen()
            return position
        }

        override fun position(newPosition: Long): SeekableByteChannel {
            ensureOpen()
            require(newPosition >= 0L) { "position must be non-negative" }
            position = newPosition
            return this
        }

        override fun size(): Long {
            ensureOpen()
            return rangeLength
        }

        override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()

        override fun isOpen(): Boolean = !closed.get() && channel.isOpen

        override fun close() {
            if (closed.compareAndSet(false, true)) input.close()
        }

        private fun ensureOpen() {
            if (!isOpen) throw ClosedChannelException()
        }
    }

    private suspend fun cacheStream(uri: Uri, afd: AssetFileDescriptor, sourcePath: String): List<DataEntity> =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".cache", File(cacheDirectory))
            val knownLength =
                if (afd.declaredLength != AssetFileDescriptor.UNKNOWN_LENGTH) afd.declaredLength else guessContentLength(
                    uri,
                    afd
                )

            Timber.d("Caching content to: ${tempFile.absolutePath}, Size: $knownLength")

            var nioSuccess = false
            try {
                val fd = afd.fileDescriptor
                if (fd.valid() && knownLength > 0) {
                    Timber.d("Attempting NIO FileChannel transfer...")
                    transferWithProgress(
                        sourceFd = fd,
                        sourceOffset = afd.startOffset,
                        destFile = tempFile,
                        totalSize = knownLength,
                        progressFlow = progressFlow
                    )
                    nioSuccess = true
                    Timber.d("NIO transfer successful.")
                }
            } catch (e: Exception) {
                Timber.w(e, "NIO transfer failed, falling back to legacy stream copy.")
                if (tempFile.exists()) tempFile.delete()
                tempFile.createNewFile()
            }

            if (!nioSuccess) {
                Timber.d("Using legacy Stream copy.")
                afd.createInputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyToWithProgress(output, knownLength, progressFlow)
                    }
                }
            }

            listOf(DataEntity.FileEntity(tempFile.absolutePath).apply { source = DataEntity.FileEntity(sourcePath) })
        }

    /**
     * Optimally guesses the content length.
     * Priority:
     * 1. AssetFileDescriptor (Zero overhead)
     * 2. ContentResolver Query (IPC overhead, but standard)
     * 3. Syscall fstat (Zero overhead, but only works for raw files)
     */
    private fun guessContentLength(uri: Uri, afd: AssetFileDescriptor?): Long {
        // 1. Trust AssetFileDescriptor first.
        // If it has a declared length (not UNKNOWN), it's the most accurate source.
        if (afd != null && afd.declaredLength != AssetFileDescriptor.UNKNOWN_LENGTH) {
            return afd.declaredLength
        }

        // 2. Try ContentResolver Query.
        // We combine OpenableColumns.SIZE and Document.COLUMN_SIZE into a single query to minimize IPC calls.
        // This is the standard way to get size for content URIs.
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_SIZE),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Check OpenableColumns.SIZE first
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) return size
                    }

                    // Fallback to DocumentsContract.Document.COLUMN_SIZE
                    val docSizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    if (docSizeIndex != -1 && !cursor.isNull(docSizeIndex)) {
                        val size = cursor.getLong(docSizeIndex)
                        if (size > 0) return size
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore query failures (e.g., SecurityException or remote provider crash)
            Timber.w(e, "Failed to query content length from ContentResolver.")
        }

        // 3. Last Resort: fstat on the existing FileDescriptor.
        // We strictly use the passed 'afd' instead of opening a new one to save resources.
        if (afd != null) {
            try {
                val fd = afd.fileDescriptor
                if (fd.valid()) {
                    val st = Os.fstat(fd)
                    // Only return size if it is a regular file (S_ISREG).
                    // Avoid using fstat on pipes/sockets as st_size might be undefined or 0.
                    if (OsConstants.S_ISREG(st.st_mode) && st.st_size > 0) {
                        // For AssetFileDescriptor, if startOffset is 0, st_size represents the full file.
                        // If startOffset > 0, we can't trust st_size to be the content length of the stream alone
                        // (it might be the whole APK size), so we only use this if we are reading the whole file.
                        if (afd.startOffset == 0L) {
                            return st.st_size
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore syscall failures
            }
        }

        return -1L
    }
}
