package com.rosan.installer.data.installer.model.impl.installer.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.app.model.enums.HttpProfile
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.exception.HttpNotAllowedException
import com.rosan.installer.data.installer.model.exception.HttpRestrictedForLocalhostException
import com.rosan.installer.data.installer.model.exception.ResolveFailedLinkNotValidException
import com.rosan.installer.data.installer.repo.NetworkResolver
import com.rosan.installer.data.installer.util.copyToWithProgress
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.util.ArchiveUtils
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

class OkHttpNetworkResolver : NetworkResolver, KoinComponent {

    private val context by inject<Context>()
    private val okHttpClient by inject<OkHttpClient>()
    private val appDataStore by inject<AppDataStore>()

    // Mutex to ensure thread-safe progress emission
    private val progressMutex = Mutex()

    companion object {
        private const val SIZE_THRESHOLD_SMALL = 5 * 1024 * 1024L        // 5MB
        private const val SIZE_THRESHOLD_MEDIUM = 20 * 1024 * 1024L      // 20MB
        private const val SIZE_THRESHOLD_LARGE = 50 * 1024 * 1024L       // 50MB
        private const val SIZE_THRESHOLD_XLARGE = 100 * 1024 * 1024L     // 100MB

        private const val MIN_CHUNK_SIZE = 2 * 1024 * 1024L              // 2MB
    }

    private enum class NetworkType { WIFI, MOBILE, ETHERNET, UNKNOWN }

    private data class ChunkRange(val start: Long, val end: Long)

    override suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity> = withContext(Dispatchers.IO) {
        Timber.d("Starting smart download for URI: $uri")
        progressFlow.emit(ProgressEntity.InstallPreparing(-1f))

        // 1. Security & Config Checks
        val httpProfileName = appDataStore.getString(AppDataStore.LAB_HTTP_PROFILE).first()
        validateSecurity(uri, HttpProfile.fromString(httpProfileName))

        val client = buildClientForScheme(uri, HttpProfile.fromString(httpProfileName))

        // 2. Pre-flight Check (HEAD Request)
        val preFlight = performPreFlightCheck(client, uri.toString())
        val contentLength = preFlight.first
        val supportsRange = preFlight.second

        if (!verifyArchiveMagicNumber(client, uri.toString())) {
            // Throw a custom exception or handle the error
            throw ResolveFailedLinkNotValidException("The target file is not a valid ZIP/APK archive.")
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
                downloadMultiThreaded(client, uri.toString(), tempFile, contentLength, threadCount, progressFlow)
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
            if (!response.isSuccessful && response.code != 206) {
                // If we get a 200 OK here, it means server ignored Range.
                // We shouldn't write to the file as it would be the full file content
                // multiplied by thread count, corrupting the download.
                throw IOException("Chunk download failed or Range ignored: ${response.code}")
            }

            val body = response.body

            RandomAccessFile(destFile, "rw").use { raf ->
                raf.seek(range.start)

                val buffer = ByteArray(8192)
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
        )
    }

    private fun performPreFlightCheck(client: OkHttpClient, url: String): Pair<Long, Boolean> {
        val request = Request.Builder()
            .url(url)
            .head()
            .addDefaultHeaders()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val acceptRanges = response.header("Accept-Ranges")
                val contentRange = response.header("Content-Range")
                val supports = (acceptRanges == "bytes" || contentRange != null)
                return Pair(length, supports)
            }
        } catch (e: Exception) {
            Timber.w(e, "Pre-flight HEAD request failed. Assuming single thread.")
            return Pair(-1L, false)
        }
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
                HttpProfile.ALLOW_SECURE -> throw HttpNotAllowedException("Cleartext HTTP not allowed.")
                HttpProfile.ALLOW_LOCAL -> {
                    if (host != "localhost" && host != "127.0.0.1" && host != "::1") {
                        throw HttpRestrictedForLocalhostException("Cleartext HTTP allowed only for localhost.")
                    }
                }

                HttpProfile.ALLOW_ALL -> {
                    /* Allowed */
                }
            }
        }
    }

    /**
     * Verify if the remote file is a valid archive by checking common ZIP signatures.
     * Requesting only the first 4 bytes is highly efficient for rejecting HTML/JSON error pages.
     *
     * @param client The OkHttpClient instance.
     * @param url The target download URL.
     * @return true if the header matches known ZIP signatures, false otherwise.
     */
    private fun verifyArchiveMagicNumber(client: OkHttpClient, url: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .addDefaultHeaders()
            // Request the first 4 bytes
            .header("Range", "bytes=0-3")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false

                val body = response.body ?: return false
                val stream = body.byteStream()
                val buffer = ByteArray(4)
                var bytesRead = 0

                while (bytesRead < 4) {
                    val read = stream.read(buffer, bytesRead, 4 - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }

                if (bytesRead >= 4) {
                    ArchiveUtils.isZipMagicNumber(buffer)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to verify archive magic number.")
            // Assume true on network failure to avoid blocking valid downloads
            // if the server simply mishandled the Range request.
            true
        }
    }
}