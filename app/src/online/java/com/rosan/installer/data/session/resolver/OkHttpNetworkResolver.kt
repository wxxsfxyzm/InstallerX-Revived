// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.rosan.installer.data.session.util.copyToWithProgress
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.session.exception.ResolveException
import com.rosan.installer.domain.session.model.ProgressEntity
import com.rosan.installer.domain.session.model.ResolveErrorType
import com.rosan.installer.domain.session.repository.NetworkResolver
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.StringSetting
import com.rosan.installer.util.isZipMagicNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

class OkHttpNetworkResolver(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val appSettingsRepo: AppSettingsRepository
) : NetworkResolver {
    // Mutex to ensure thread-safe progress emission
    private val progressMutex = Mutex()

    private companion object {
        const val SIZE_THRESHOLD_SMALL = 5 * 1024 * 1024L        // 5MB
        const val SIZE_THRESHOLD_MEDIUM = 20 * 1024 * 1024L      // 20MB
        const val SIZE_THRESHOLD_LARGE = 50 * 1024 * 1024L       // 50MB
        const val SIZE_THRESHOLD_XLARGE = 100 * 1024 * 1024L     // 100MB

        const val MIN_CHUNK_SIZE = 2 * 1024 * 1024L              // 2MB
        const val DOWNLOAD_BUFFER_SIZE = 1024 * 1024             // 1MB
    }

    private enum class NetworkType { WIFI, MOBILE, ETHERNET, UNKNOWN }

    private data class ChunkRange(val start: Long, val end: Long)
    private data class PreFlightResult(
        val contentLength: Long,
        val supportsRange: Boolean,
        val isArchive: Boolean
    )

    private class RangeNotSupportedException(message: String) : IOException(message)

    override suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity> = withContext(Dispatchers.IO) {
        Timber.d("Starting smart download for URI: $uri")
        progressFlow.emit(ProgressEntity.InstallPreparing(-1f))

        // 1. Security & Config Checks
        val httpProfileName = appSettingsRepo.getString(StringSetting.LabHttpProfile).first()
        validateSecurity(uri, HttpProfile.fromString(httpProfileName))

        val client = buildClientForScheme(uri, HttpProfile.fromString(httpProfileName))

        // 2. Pre-flight Check
        val preFlight = performPreFlightCheck(client, uri.toString())
        val contentLength = preFlight.contentLength
        val supportsRange = preFlight.supportsRange

        if (!preFlight.isArchive) {
            throw ResolveException(
                errorType = ResolveErrorType.LINK_NOT_VALID,
                message = "The target file is not a valid ZIP/APK archive."
            )
        }

        val tempFile = File(cacheDirectory, UUID.randomUUID().toString())
        tempFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        // 3. Determine Strategy
        val threadCount = if (supportsRange && contentLength > 0) {
            calculateOptimalThreadCount(contentLength)
        } else {
            1
        }

        try {
            if (threadCount > 1) {
                Timber.i("Strategy: Multi-threaded ($threadCount threads). Size: $contentLength")
                try {
                    downloadMultiThreaded(client, uri.toString(), tempFile, contentLength, threadCount, progressFlow)
                } catch (e: RangeNotSupportedException) {
                    Timber.w(e, "Range download failed. Falling back to single-threaded download.")
                    if (tempFile.exists()) tempFile.delete()
                    downloadSingleThreaded(client, uri.toString(), tempFile, progressFlow)
                }
            } else {
                Timber.i("Strategy: Single-threaded. Range Support: $supportsRange, Size: $contentLength")
                downloadSingleThreaded(client, uri.toString(), tempFile, progressFlow)
            }

            return@withContext listOf(DataEntity.FileEntity(tempFile.absolutePath))

        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    private fun calculateOptimalThreadCount(fileSize: Long): Int {
        val networkType = getNetworkType()

        val baseCount = when {
            fileSize < SIZE_THRESHOLD_SMALL -> 1
            fileSize < SIZE_THRESHOLD_MEDIUM -> 2
            fileSize < SIZE_THRESHOLD_LARGE -> 4
            fileSize < SIZE_THRESHOLD_XLARGE -> 5
            else -> 6
        }

        val adjustedCount = when (networkType) {
            NetworkType.WIFI -> when {
                fileSize < SIZE_THRESHOLD_SMALL -> 1
                fileSize < SIZE_THRESHOLD_MEDIUM -> 3
                fileSize < SIZE_THRESHOLD_LARGE -> 5
                else -> 8
            }

            NetworkType.ETHERNET -> when {
                fileSize < SIZE_THRESHOLD_SMALL -> 1
                fileSize < SIZE_THRESHOLD_MEDIUM -> 3
                else -> 10
            }

            NetworkType.MOBILE -> when {
                fileSize < SIZE_THRESHOLD_SMALL -> 1
                fileSize < SIZE_THRESHOLD_MEDIUM -> 2
                else -> 4
            }

            NetworkType.UNKNOWN -> baseCount
        }

        val maxThreadsByChunk = max(1, (fileSize / MIN_CHUNK_SIZE).toInt())
        return min(adjustedCount, maxThreadsByChunk)
    }

    private suspend fun downloadMultiThreaded(
        client: OkHttpClient,
        url: String,
        destFile: File,
        totalSize: Long,
        threadCount: Int,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ) = coroutineScope {
        RandomAccessFile(destFile, "rw").use { it.setLength(totalSize) }

        val ranges = calculateRanges(totalSize, threadCount)
        val downloadedTotal = AtomicLong(0)
        // Use AtomicInteger to track the last integer percentage (0-100) emitted
        // This prevents locking mutex on every single byte read
        val lastEmittedPercent = AtomicInteger(0)

        ranges.map { range ->
            async(Dispatchers.IO) {
                downloadChunk(client, url, destFile, range, totalSize, downloadedTotal, lastEmittedPercent, progressFlow)
            }
        }.awaitAll()

        progressFlow.emit(ProgressEntity.InstallPreparing(1f))
    }

    private suspend fun downloadChunk(
        client: OkHttpClient,
        url: String,
        destFile: File,
        range: ChunkRange,
        totalSize: Long,
        downloadedTotal: AtomicLong,
        lastEmittedPercent: AtomicInteger,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ) {
        val request = Request.Builder()
            .url(url)
            .addDefaultHeaders()
            .header("Range", "bytes=${range.start}-${range.end}")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code != 206) {
                // If we get a 200 OK here, it means server ignored Range.
                // We shouldn't write to the file as it would be the full file content
                // multiplied by thread count, corrupting the download.
                throw RangeNotSupportedException("Chunk download failed or Range ignored: ${response.code}")
            }

            val body = response.body

            RandomAccessFile(destFile, "rw").use { raf ->
                raf.seek(range.start)

                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                var bytesRead: Int
                val input = body.byteStream()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)

                    val currentTotal = downloadedTotal.addAndGet(bytesRead.toLong())

                    // Calculate current integer percentage (0-100)
                    val currentPercent = ((currentTotal.toDouble() / totalSize) * 100).toInt()
                    val lastPercent = lastEmittedPercent.get()

                    // Only acquire lock and emit if the percentage has increased by at least 1%
                    if (currentPercent > lastPercent) {
                        // CompareAndSet ensures only ONE thread emits for this percentage step
                        if (lastEmittedPercent.compareAndSet(lastPercent, currentPercent)) {
                            progressMutex.withLock {
                                progressFlow.emit(ProgressEntity.InstallPreparing(currentPercent / 100f))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun calculateRanges(totalSize: Long, threadCount: Int): List<ChunkRange> {
        val ranges = mutableListOf<ChunkRange>()
        val chunkSize = totalSize / threadCount

        for (i in 0 until threadCount) {
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalSize - 1 else (start + chunkSize - 1)
            ranges.add(ChunkRange(start, end))
        }
        return ranges
    }

    private suspend fun downloadSingleThreaded(
        client: OkHttpClient,
        url: String,
        destFile: File,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ) {
        val request = Request.Builder()
            .url(url)
            .addDefaultHeaders()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyToWithProgress(output, body.contentLength(), progressFlow)
                }
            }
        }
    }

    // --- Helper Methods ---

    private fun Request.Builder.addDefaultHeaders(): Request.Builder {
        return this.header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        ).header(
            "Accept",
            "application/vnd.android.package-archive, application/octet-stream, */*"
        ).header(
            "Accept-Encoding",
            "identity"
        )
    }

    private fun performPreFlightCheck(client: OkHttpClient, url: String): PreFlightResult {
        val rangeProbe = Request.Builder()
            .url(url)
            .addDefaultHeaders()
            .header("Range", "bytes=0-3")
            .build()

        try {
            client.newCall(rangeProbe).execute().use { response ->
                if (!response.isSuccessful) return performHeadFallback(client, url)

                val buffer = ByteArray(4)
                val bytesRead = response.body.byteStream().readUpTo(buffer)
                val isArchive = bytesRead >= 4 && buffer.isZipMagicNumber()

                val contentRange = response.header("Content-Range")
                val rangeTotal = parseContentRangeTotal(contentRange)
                val responseLength = response.header("Content-Length")?.toLongOrNull() ?: response.body.contentLength()
                val supportsRange = response.code == 206 && rangeTotal > 0
                val length = when {
                    rangeTotal > 0 -> rangeTotal
                    response.code == 200 -> responseLength
                    else -> -1L
                }

                return PreFlightResult(
                    contentLength = length,
                    supportsRange = supportsRange,
                    isArchive = isArchive
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Range pre-flight request failed. Falling back to HEAD.")
            return performHeadFallback(client, url)
        }
    }

    private fun performHeadFallback(client: OkHttpClient, url: String): PreFlightResult {
        val request = Request.Builder()
            .url(url)
            .head()
            .addDefaultHeaders()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val supports = response.header("Accept-Ranges").equals("bytes", ignoreCase = true)

                // Preserve the previous behavior: if the lightweight archive probe cannot
                // complete, do not reject otherwise valid download sources.
                PreFlightResult(length, supports, isArchive = true)
            }
        } catch (e: Exception) {
            Timber.w(e, "Pre-flight HEAD request failed. Assuming single thread.")
            PreFlightResult(-1L, supportsRange = false, isArchive = true)
        }
    }

    private fun parseContentRangeTotal(contentRange: String?): Long {
        if (contentRange.isNullOrBlank()) return -1L
        val total = contentRange.substringAfter('/', missingDelimiterValue = "")
        return total.toLongOrNull() ?: -1L
    }

    private fun InputStream.readUpTo(buffer: ByteArray): Int {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val read = read(buffer, bytesRead, buffer.size - bytesRead)
            if (read == -1) break
            bytesRead += read
        }
        return bytesRead
    }

    private fun getNetworkType(): NetworkType {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NetworkType.UNKNOWN
            val network = cm.activeNetwork ?: return NetworkType.UNKNOWN
            val caps = cm.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN

            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                else -> NetworkType.UNKNOWN
            }
        } catch (_: Exception) {
            return NetworkType.UNKNOWN
        }
    }

    private fun buildClientForScheme(uri: Uri, profile: HttpProfile): OkHttpClient {
        val scheme = uri.scheme?.lowercase()
        return if (scheme == "http" && profile != HttpProfile.ALLOW_SECURE) {
            okHttpClient.newBuilder()
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                .build()
        } else {
            okHttpClient
        }
    }

    private fun validateSecurity(uri: Uri, profile: HttpProfile) {
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase() ?: ""

        if (scheme == "http") {
            when (profile) {
                HttpProfile.ALLOW_SECURE -> throw ResolveException(
                    errorType = ResolveErrorType.HTTP_NOT_ALLOWED,
                    message = "Cleartext HTTP not allowed."
                )

                HttpProfile.ALLOW_LOCAL -> {
                    if (host != "localhost" && host != "127.0.0.1" && host != "::1") {
                        throw ResolveException(
                            errorType = ResolveErrorType.HTTP_RESTRICTED_FOR_LOCALHOST,
                            message = "Cleartext HTTP allowed only for localhost."
                        )
                    }
                }

                HttpProfile.ALLOW_ALL -> {
                    /* Allowed */
                }
            }
        }
    }
}
