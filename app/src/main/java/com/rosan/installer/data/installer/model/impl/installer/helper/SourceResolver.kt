package com.rosan.installer.data.installer.model.impl.installer.helper

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import androidx.core.net.toUri
import com.rosan.installer.build.RsConfig
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.model.exception.ResolvedFailedNoInternetAccessException
import com.rosan.installer.data.installer.util.getRealPathFromUri
import com.rosan.installer.data.installer.util.pathUnify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.UUID

class SourceResolver(
    private val cacheDirectory: String,
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) : KoinComponent {
    private val context by inject<Context>()
    private val okHttpClient by inject<OkHttpClient>()
    private val closeables = mutableListOf<Closeable>()
    private val COPY_BUFFER_SIZE = 1 * 1024 * 1024 // 1MB

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
        val uris = when (action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    if (!RsConfig.isInternetAccessEnabled) {
                        Timber.d("Internet access is disabled in the app settings.")
                        throw ResolvedFailedNoInternetAccessException("No internet access to download files.")
                    }
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotBlank() }?.let { listOf(it.toUri()) }
                        ?: emptyList()
                } else {
                    val uri = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    listOfNotNull(uri)
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)) ?: emptyList()
            }

            else -> listOfNotNull(intent.data)
        }
        if (uris.isEmpty()) throw ResolveException(action, uris)
        return uris
    }

    private suspend fun resolveSingleUri(uri: Uri): List<DataEntity> {
        Timber.d("Source URI: $uri")
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path ?: throw Exception("Invalid file URI: $uri")
                Timber.d("Resolving direct file URI: $path")
                listOf(DataEntity.FileEntity(path).apply { source = DataEntity.FileEntity(path) })
            }

            "content" -> resolveContentUri(uri)
            "http", "https" -> downloadHttpUri(uri)
            else -> throw ResolveException("Unsupported scheme: ${uri.scheme}", listOf(uri))
        }
    }

    private suspend fun resolveContentUri(uri: Uri): List<DataEntity> {
        val afd = context.contentResolver?.openAssetFileDescriptor(uri, "r")
            ?: throw IOException("Cannot open file descriptor: $uri")

        // Try direct procfs access (Zero-Copy)
        val procPath = "/proc/${Os.getpid()}/fd/${afd.parcelFileDescriptor.fd}"
        val file = File(procPath)
        val sourcePath = runCatching { Os.readlink(procPath).getRealPathFromUri(uri).pathUnify() }.getOrDefault("")

        if (file.canRead()) {
            try {
                // Validate read access. RandomAccessFile will throw if permission denied.
                RandomAccessFile(file, "r").use { }
                if (sourcePath.startsWith('/')) {
                    Timber.d("Success! Got direct, usable file access through procfs: $sourcePath")
                    closeables.add(afd) // Keep open until session ends
                    return listOf(DataEntity.FileEntity(procPath).apply { source = DataEntity.FileEntity(sourcePath) })
                }
            } catch (e: Exception) {
                Timber.w(e, "Direct procfs access test failed. Falling back to cache.")
            }
        } else {
            Timber.d("Direct file access failed or file not readable. Falling back to caching with progress.")
        }

        // Fallback: Cache to temp file
        return cacheStream(uri, afd, sourcePath)
    }

    private suspend fun cacheStream(uri: Uri, afd: AssetFileDescriptor, sourcePath: String): List<DataEntity> =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".cache", File(cacheDirectory))
            val totalSize = guessContentLength(uri, afd)

            Timber.d("Caching content to: ${tempFile.absolutePath}, Size: $totalSize")

            afd.use { descriptor ->
                descriptor.createInputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        copyStreamWithProgress(input, output, totalSize)
                    }
                }
            }
            listOf(DataEntity.FileEntity(tempFile.absolutePath).apply { source = DataEntity.FileEntity(sourcePath) })
        }

    private suspend fun downloadHttpUri(uri: Uri): List<DataEntity> = withContext(Dispatchers.IO) {
        Timber.d("Starting download for HTTP URI: $uri using OkHttp")
        progressFlow.emit(ProgressEntity.InstallPreparing(-1f))

        val request = Request.Builder().url(uri.toString()).build()
        val call = okHttpClient.newCall(request)

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                Timber.e("HTTP Request failed. Code: ${response.code}")
                throw IOException("HTTP ${response.code}")
            }

            val body = response.body

            // 1. Check Extension
            val path = uri.path ?: ""
            Timber.d("[Validation] Checking URI path: $path")

            val isSupportedExtension = listOf(".apk", ".xapk", ".apkm", ".apks", ".zip").any { ext ->
                path.endsWith(ext, ignoreCase = true)
            }
            Timber.d("[Validation] Extension check passed: $isSupportedExtension")

            // 2. Check Content-Type
            // Get MediaType object from OkHttp
            val mediaType = body.contentType()
            // Get raw header as fallback
            val headerContentType = response.header("Content-Type")

            // Determine which string we are checking
            val contentTypeToCheck = mediaType?.toString() ?: headerContentType

            Timber.d("[Validation] Server Content-Type (MediaType): ${mediaType?.toString()}")
            Timber.d("[Validation] Server Content-Type (Header): $headerContentType")
            Timber.d("[Validation] Final Content-Type string used for check: $contentTypeToCheck")

            val isSupportedMimeType = contentTypeToCheck != null && (
                    contentTypeToCheck.equals("application/vnd.android.package-archive", ignoreCase = true) ||
                            contentTypeToCheck.equals("application/octet-stream", ignoreCase = true) ||
                            contentTypeToCheck.equals("application/vnd.apkm", ignoreCase = true) ||
                            contentTypeToCheck.equals("application/vnd.apks", ignoreCase = true) ||
                            contentTypeToCheck.equals("application/xapk-package-archive", ignoreCase = true)
                    )
            Timber.d("[Validation] MIME type check passed: $isSupportedMimeType")

            // 3. Final Decision
            if (!isSupportedExtension && !isSupportedMimeType) {
                Timber.w("[Validation] Validation FAILED. Neither extension nor MIME type is supported.")
                // Close body before throwing to avoid connection leaks
                body.close()
                throw ResolveException(
                    action = "Unsupported file type from URL. Path: $path, Content-Type: $contentTypeToCheck",
                    uris = listOf(uri)
                )
            } else {
                Timber.d("[Validation] Validation SUCCESS. Proceeding with download.")
            }
            // --- End Validation Logic ---

            val totalSize = body.contentLength()
            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".apk_cache", File(cacheDirectory))
            Timber.d("Downloading $totalSize bytes to ${tempFile.absolutePath}")

            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    copyStreamWithProgress(input, output, totalSize)
                }
            }

            Timber.d("URL download and caching complete.")
            listOf(DataEntity.FileEntity(tempFile.absolutePath))

        } catch (e: Exception) {
            Timber.e(e, "Download failed or cancelled")
            call.cancel()
            throw e
        }
    }

    private suspend fun copyStreamWithProgress(input: InputStream, output: OutputStream, totalSize: Long) {
        var bytesCopied = 0L
        val buf = ByteArray(COPY_BUFFER_SIZE)
        val step = if (totalSize > 0) (totalSize * 0.01f).toLong().coerceAtLeast(128 * 1024) else Long.MAX_VALUE
        var nextEmit = step
        var lastEmitTime = 0L

        if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(0f))

        // Check for cancellation before starting loop
        if (!currentCoroutineContext().isActive) throw CancellationException()

        var read = input.read(buf)
        while (read >= 0) {
            output.write(buf, 0, read)
            bytesCopied += read

            val now = SystemClock.uptimeMillis()
            if (totalSize > 0 && bytesCopied >= nextEmit && now - lastEmitTime >= 200) {
                // Check cancellation during progress updates
                if (!currentCoroutineContext().isActive) throw CancellationException()

                progressFlow.tryEmit(ProgressEntity.InstallPreparing(bytesCopied.toFloat() / totalSize))
                lastEmitTime = now
                nextEmit = (bytesCopied / step + 1) * step
            }
            read = input.read(buf)
        }
        if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(1f))
    }

    private fun guessContentLength(uri: Uri, afd: AssetFileDescriptor?): Long {
        if (afd != null && afd.declaredLength > 0) return afd.declaredLength

        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                if (pfd.statSize > 0) return pfd.statSize
            }
        }

        fun queryLong(projection: Array<String>): Long {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    for (col in projection) {
                        val idx = c.getColumnIndex(col)
                        if (idx != -1) {
                            val v = c.getLong(idx)
                            if (v > 0) return v
                        }
                    }
                }
            }
            return -1L
        }

        val fromOpenable = queryLong(arrayOf(OpenableColumns.SIZE))
        if (fromOpenable > 0) return fromOpenable

        if (DocumentsContract.isDocumentUri(context, uri)) {
            val fromDoc = queryLong(arrayOf(DocumentsContract.Document.COLUMN_SIZE))
            if (fromDoc > 0) return fromDoc
        }

        runCatching {
            afd?.parcelFileDescriptor?.fileDescriptor?.let { fd ->
                val st = Os.fstat(fd)
                if (st.st_size > 0) return st.st_size
            }
        }
        return -1L
    }
}