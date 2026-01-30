package com.rosan.installer.data.installer.model.impl.installer.helper

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import android.system.OsConstants
import androidx.core.net.toUri
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.exception.ResolvedFailedNoInternetAccessException
import com.rosan.installer.data.installer.repo.NetworkResolver
import com.rosan.installer.data.installer.util.copyToWithProgress
import com.rosan.installer.data.installer.util.getRealPathFromUri
import com.rosan.installer.data.installer.util.pathUnify
import com.rosan.installer.data.installer.util.transferWithProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID

class SourceResolver(
    private val cacheDirectory: String,
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) : KoinComponent {
    private val context by inject<Context>()
    private val closeables = mutableListOf<Closeable>()

    fun getTrackedCloseables(): List<Closeable> = closeables

    suspend fun resolve(intent: Intent): List<DataEntity> {
        val uris = extractUris(intent)
        Timber.d("resolve: URIs extracted from intent (${uris.size}).")

        val data = mutableListOf<DataEntity>()
        for (uri in uris) {
            // Check cancellation between items
            if (!currentCoroutineContext().isActive) throw CancellationException()
            data.addAll(resolveSingleUri(uri))
        }
        return data
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
                    val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }

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
                val streams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
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

        if (uris.isEmpty()) throw ResolveException(action, uris)
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
                if (!RsConfig.isInternetAccessEnabled) {
                    Timber.d("Internet access is disabled in app settings. Aborting network request.")
                    throw ResolvedFailedNoInternetAccessException("No internet access to download files.")
                }

                get<NetworkResolver>().resolve(uri, cacheDirectory, progressFlow)
            }

            else -> throw ResolveException("Unsupported scheme: $scheme", listOf(uri))
        }
    }

    private suspend fun resolveContentUri(uri: Uri): List<DataEntity> {
        val afd = context.contentResolver?.openAssetFileDescriptor(uri, "r")
            ?: throw IOException("Cannot open file descriptor: $uri")

        // Resolve real path
        val fd = afd.parcelFileDescriptor.fd
        val procPath = "/proc/${Os.getpid()}/fd/$fd"
        // Attempt to resolve the real path, e.g., /storage/... or /data/...
        val realPath = runCatching {
            Os.readlink(procPath).getRealPathFromUri(uri).pathUnify()
        }.getOrDefault("")

        // If the path exists and we have permission to read it directly, detach immediately
        if (realPath.isNotEmpty() && File(realPath).canRead()) {
            // Perform a defensive check to prevent File.canRead() from reporting false positives in certain SELinux contexts
            val detachedSuccess = runCatching {
                RandomAccessFile(realPath, "r").use { }
                true
            }.getOrDefault(false)

            if (detachedSuccess) {
                Timber.d("Detached mode success! File is directly readable: $realPath")
                // Close the afd immediately to completely cut off the Binder connection with the ContentProvider (e.g., MT Manager)
                // This ensures that even if the provider app is killed, the system won't kill this process due to the association mechanism
                afd.close()
                return listOf(DataEntity.FileEntity(realPath).apply { source = DataEntity.FileEntity(realPath) })
            }
        }

        // If we reach here, the file is likely in a private directory (EACCES) or the path could not be resolved.
        // To prevent association killing, we no longer attempt to read via /proc/self/fd/ in a "dependent" manner.
        // Fall back to caching mode: copy the data out and disconnect.
        Timber.d("File is private or unreadable ($realPath). Falling back to Cache to avoid dependency kill.")

        return cacheStream(uri, afd, realPath)
    }

    private suspend fun cacheStream(uri: Uri, afd: AssetFileDescriptor, sourcePath: String): List<DataEntity> =
        withContext(Dispatchers.IO) {
            afd.use { descriptor ->
                val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".cache", File(cacheDirectory))
                val knownLength =
                    if (descriptor.declaredLength != AssetFileDescriptor.UNKNOWN_LENGTH) descriptor.declaredLength else guessContentLength(
                        uri,
                        descriptor
                    )

                Timber.d("Caching content to: ${tempFile.absolutePath}, Size: $knownLength")

                var nioSuccess = false
                try {
                    val fd = descriptor.fileDescriptor
                    if (fd != null && fd.valid() && knownLength > 0) {
                        Timber.d("Attempting NIO FileChannel transfer...")
                        transferWithProgress(
                            sourceFd = fd,
                            sourceOffset = descriptor.startOffset,
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
                    descriptor.createInputStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyToWithProgress(output, knownLength, progressFlow)
                        }
                    }
                }

                listOf(DataEntity.FileEntity(tempFile.absolutePath).apply { source = DataEntity.FileEntity(sourcePath) })
            }
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