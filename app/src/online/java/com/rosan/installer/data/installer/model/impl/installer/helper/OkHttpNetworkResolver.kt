package com.rosan.installer.data.installer.model.impl.installer.helper

import android.net.Uri
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.entity.HttpProfile
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.exception.HttpNotAllowedException
import com.rosan.installer.data.installer.model.exception.HttpRestrictedForLocalhostException
import com.rosan.installer.data.installer.model.exception.ResolveException
import com.rosan.installer.data.installer.repo.NetworkResolver
import com.rosan.installer.data.installer.util.copyToWithProgress
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID

class OkHttpNetworkResolver : NetworkResolver, KoinComponent {

    private val okHttpClient by inject<OkHttpClient>()
    private val appDataStore by inject<AppDataStore>()

    override suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity> = withContext(Dispatchers.IO) {
        Timber.d("Starting download for HTTP URI: $uri")
        progressFlow.emit(ProgressEntity.InstallPreparing(-1f))

        // Load Profile Config (Only Profile, no path config here)
        val httpProfileName = appDataStore.getString(AppDataStore.LAB_HTTP_PROFILE).first()
        val httpProfile = HttpProfile.fromString(httpProfileName)

        // Validate Security Profile
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase() ?: ""
        if (scheme == "http") validateHttpProfile(httpProfile, host)

        // Configure Client
        val client = if (scheme == "http" && httpProfile != HttpProfile.ALLOW_SECURE) {
            okHttpClient.newBuilder()
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                .build()
        } else {
            okHttpClient
        }

        val requestBuilder = Request.Builder()
            .url(uri.toString())
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
            )

        // MiXplorer 特殊处理
        if (uri.toString().startsWith("http://127.0.0.1:34859")) {
            // MiXplorer 不能设置为 application/vnd.android.package-archive
            requestBuilder.header(
                "Accept",
                "application/octet-stream, */*"
            )
        } else {
            // 默认的 Accept 头
            requestBuilder.header(
                "Accept",
                "application/vnd.android.package-archive, application/octet-stream, */*"
            )
        }

        val request = requestBuilder.build()

        val call = client.newCall(request)

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("HTTP Request failed. Code: ${response.code}")
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body

                // Validation Logic
                // Use the final URL to handle redirects correctly
                val finalUrlPath = response.request.url.encodedPath
                val originalPath = uri.path ?: ""

                // Check extension on BOTH original and final URL
                val isSupportedExtension = listOf(".apk", ".xapk", ".apkm", ".apks", ".zip").any { ext ->
                    originalPath.endsWith(ext, ignoreCase = true) || finalUrlPath.endsWith(ext, ignoreCase = true)
                }

                val mediaType = body.contentType()
                val responseHeader = response.header("Content-Type")
                Timber.d("responseHeader is:", responseHeader)
                val contentTypeToCheck = mediaType?.toString() ?: response.header("Content-Type")

                val isSupportedMimeType = contentTypeToCheck != null && (
                        contentTypeToCheck.equals("application/vnd.android.package-archive", ignoreCase = true) ||
                                contentTypeToCheck.equals("application/octet-stream", ignoreCase = true) ||
                                contentTypeToCheck.contains("application/vnd.apkm", ignoreCase = true) ||
                                contentTypeToCheck.contains("application/vnd.apks", ignoreCase = true) ||
                                contentTypeToCheck.contains("application/xapk-package-archive", ignoreCase = true)
                        )

                // Validation: Must be a supported extension OR a supported MIME type.
                // This allows generic URLs if the server returns correct MIME,
                // and allows bad MIME servers if the URL extension is correct.
                if (!isSupportedExtension && !isSupportedMimeType) {
                    throw ResolveException(
                        action = "Unsupported file type. Path: $originalPath, Final: $finalUrlPath, Type: $contentTypeToCheck",
                        uris = listOf(uri)
                    )
                }

                // Create Temp File (UUID)
                // Download directly to cache using a random UUID. Analysis will handle identification.
                val tempFile = File(cacheDirectory, UUID.randomUUID().toString())

                // Ensure directory exists
                tempFile.parentFile?.let { parent ->
                    if (!parent.exists()) parent.mkdirs()
                }

                val totalSize = body.contentLength()
                Timber.d("Downloading to temp file: ${tempFile.absolutePath}, Size: $totalSize")

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyToWithProgress(output, totalSize, progressFlow)
                    }
                }

                // Return the temp file entity
                return@use listOf(DataEntity.FileEntity(tempFile.absolutePath))
            }
        } catch (e: Exception) {
            if (!call.isCanceled()) call.cancel()
            throw e
        }
    }

    private fun validateHttpProfile(profile: HttpProfile, host: String) {
        when (profile) {
            HttpProfile.ALLOW_SECURE -> throw HttpNotAllowedException("Cleartext HTTP not allowed.")
            HttpProfile.ALLOW_LOCAL -> {
                if (host != "localhost" && host != "127.0.0.1" && host != "::1") {
                    throw HttpRestrictedForLocalhostException("Cleartext HTTP allowed only for localhost.")
                }
            }

            HttpProfile.ALLOW_ALL -> { /* Allowed */
            }
        }
    }
}