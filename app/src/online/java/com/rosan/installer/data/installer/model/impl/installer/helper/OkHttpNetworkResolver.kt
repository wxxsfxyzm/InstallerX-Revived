package com.rosan.installer.data.installer.model.impl.installer.helper

import android.net.Uri
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.repo.NetworkResolver
import com.rosan.installer.data.installer.util.copyToWithProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URLDecoder

class OkHttpNetworkResolver : NetworkResolver, KoinComponent {

    private val okHttpClient by inject<OkHttpClient>()

    override suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity> = withContext(Dispatchers.IO) {
        Timber.d("Starting download for HTTP URI: $uri using OkHttp")
        progressFlow.emit(ProgressEntity.InstallPreparing(-1f))

        val request = Request.Builder()
            .url(uri.toString())
            .header("User-Agent", "Mozilla/5.0 (Android) SourceResolver/1.0")
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .build()

        val call = okHttpClient.newCall(request)

        try {
            // Use 'use' block to automatically close the response (and its body)
            // regardless of success or exceptions to prevent connection leaks.
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("HTTP Request failed. Code: ${response.code}")
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body

                // 1. Validation Logic
                val path = uri.path ?: ""
                val isSupportedExtension = listOf(".apk", ".xapk", ".apkm", ".apks", ".zip").any { ext ->
                    path.endsWith(ext, ignoreCase = true)
                }

                val mediaType = body.contentType()
                val contentTypeToCheck = mediaType?.toString() ?: response.header("Content-Type")

                val isSupportedMimeType = contentTypeToCheck != null && (
                        contentTypeToCheck.equals("application/vnd.android.package-archive", ignoreCase = true) ||
                                contentTypeToCheck.equals("application/octet-stream", ignoreCase = true) ||
                                contentTypeToCheck.contains("application/vnd.apkm", ignoreCase = true) ||
                                contentTypeToCheck.contains("application/vnd.apks", ignoreCase = true) ||
                                contentTypeToCheck.contains("application/xapk-package-archive", ignoreCase = true)
                        )

                if (!isSupportedExtension && !isSupportedMimeType) {
                    // No need to manually close body here, the 'use' block handles it.
                    throw ResolveException(
                        action = "Unsupported file type. Path: $path, Content-Type: $contentTypeToCheck",
                        uris = listOf(uri)
                    )
                }

                // 2. Determine file name and create file
                val fileName = getFileNameFromResponse(response, uri)
                var targetFile = File(cacheDirectory, fileName)
                if (targetFile.exists()) targetFile.delete()
                if (!targetFile.createNewFile()) {
                    targetFile = File.createTempFile("dl_", "_$fileName", File(cacheDirectory))
                }

                // 3. Download and Write
                val totalSize = body.contentLength()
                Timber.d("Downloading $totalSize bytes to ${targetFile.absolutePath}")

                // Nested 'use' ensures the stream is closed eagerly after copying.
                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyToWithProgress(output, totalSize, progressFlow)
                    }
                }

                return@use listOf(DataEntity.FileEntity(targetFile.absolutePath))
            }
        } catch (e: Exception) {
            if (!call.isCanceled()) call.cancel()
            throw e
        }
    }

    private fun getFileNameFromResponse(response: Response, uri: Uri): String {
        var fileName: String? = null
        val contentDisposition = response.header("Content-Disposition")

        if (contentDisposition != null) {
            val dispositionHeader = contentDisposition.lowercase()
            if (dispositionHeader.contains("filename=")) {
                val split = contentDisposition.split("filename=")
                if (split.size > 1) {
                    fileName = split[1].trim().replace("\"", "")
                }
            }
        }

        if (fileName.isNullOrBlank()) fileName = uri.lastPathSegment
        if (fileName.isNullOrBlank()) fileName = "downloaded_file.apk"

        try {
            fileName = URLDecoder.decode(fileName, "UTF-8")
        } catch (_: Exception) {
        }

        return fileName!!.replace("/", "_").replace("\\", "_")
    }
}