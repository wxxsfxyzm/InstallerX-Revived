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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class SourceResolver(
    private val cacheDirectory: String,
    private val progressFlow: MutableSharedFlow<ProgressEntity>
) : KoinComponent {
    private val context by inject<Context>()
    private val closeables = mutableListOf<Closeable>()
    private val COPY_BUFFER_SIZE = 1 * 1024 * 1024 // 1MB

    fun getTrackedCloseables(): List<Closeable> = closeables

    suspend fun resolve(intent: Intent): List<DataEntity> {
        val uris = extractUris(intent)
        val data = mutableListOf<DataEntity>()
        for (uri in uris) {
            data.addAll(resolveSingleUri(uri))
        }
        return data
    }

    private fun extractUris(intent: Intent): List<Uri> {
        val action = intent.action
        val uris = when (action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    if (!RsConfig.isInternetAccessEnabled) throw ResolvedFailedNoInternetAccessException("No internet access")
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
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path ?: throw Exception("Invalid file URI: $uri")
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
                // Validate read access
                RandomAccessFile(file, "r").use { }
                if (sourcePath.startsWith('/')) {
                    Timber.d("Direct procfs access success: $sourcePath")
                    closeables.add(afd) // Keep open until session ends
                    return listOf(DataEntity.FileEntity(procPath).apply { source = DataEntity.FileEntity(sourcePath) })
                }
            } catch (e: Exception) {
                Timber.w("Direct procfs check failed, falling back to cache.")
            }
        }

        // Fallback: Cache to temp file
        return cacheStream(uri, afd, sourcePath)
    }

    private suspend fun cacheStream(uri: Uri, afd: AssetFileDescriptor, sourcePath: String): List<DataEntity> =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".cache", File(cacheDirectory))
            val totalSize = guessContentLength(uri, afd)

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
        progressFlow.emit(ProgressEntity.InstallPreparing(-1f))
        val url = URL(uri.toString())
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
            connect()
        }

        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")

            // Basic validation logic omitted for brevity (MimeType/Extension check)

            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".apk_cache", File(cacheDirectory))
            conn.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    copyStreamWithProgress(input, output, conn.contentLengthLong)
                }
            }
            listOf(DataEntity.FileEntity(tempFile.absolutePath))
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun copyStreamWithProgress(input: InputStream, output: OutputStream, totalSize: Long) {
        var bytesCopied = 0L
        val buf = ByteArray(COPY_BUFFER_SIZE)
        val step = if (totalSize > 0) (totalSize * 0.01f).toLong().coerceAtLeast(128 * 1024) else Long.MAX_VALUE
        var nextEmit = step
        var lastEmitTime = 0L

        if (totalSize > 0) progressFlow.emit(ProgressEntity.InstallPreparing(0f))

        var read = input.read(buf)
        while (read >= 0) {
            output.write(buf, 0, read)
            bytesCopied += read

            val now = SystemClock.uptimeMillis()
            if (totalSize > 0 && bytesCopied >= nextEmit && now - lastEmitTime >= 200) {
                progressFlow.tryEmit(ProgressEntity.InstallPreparing(bytesCopied.toFloat() / totalSize))
                lastEmitTime = now
                nextEmit = (bytesCopied / step + 1) * step
            }
            read = input.read(buf)
        }
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