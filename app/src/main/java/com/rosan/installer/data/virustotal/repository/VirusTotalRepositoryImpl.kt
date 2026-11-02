// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.virustotal.repository

import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysis
import com.rosan.installer.domain.virustotal.model.VirusTotalAnalysisStatus
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalException
import com.rosan.installer.domain.virustotal.model.VirusTotalFileReport
import com.rosan.installer.domain.virustotal.model.VirusTotalHash
import com.rosan.installer.domain.virustotal.repository.VirusTotalRepository
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import timber.log.Timber

class VirusTotalRepositoryImpl(
    baseClient: OkHttpClient,
    private val json: Json,
    private val appSettingsRepository: AppSettingsRepository,
) : VirusTotalRepository {

    // The shared client times out after 15s, which is too short for APK uploads.
    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    override suspend fun computeSha256(source: DataEntity): VirusTotalHash = withContext(Dispatchers.IO) {
        if (source is DataEntity.StreamDataEntity || source.getSourceTop() is DataEntity.StreamDataEntity) {
            throw VirusTotalException(VirusTotalErrorKind.UNSUPPORTED_SOURCE, "Streamed sources cannot be re-read for scanning")
        }
        try {
            val stream = source.getInputStream()
                ?: throw VirusTotalException(VirusTotalErrorKind.UNSUPPORTED_SOURCE, "Source cannot be opened for scanning")
            stream.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var size = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read > 0) {
                        digest.update(buffer, 0, read)
                        size += read
                    }
                }
                VirusTotalHash(
                    sha256 = digest.digest().joinToString("") { "%02x".format(it) },
                    size = size,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: VirusTotalException) {
            throw error
        } catch (error: Exception) {
            Timber.e(error, "VirusTotal: failed to hash source")
            throw VirusTotalException(VirusTotalErrorKind.HASH_FAILED, error.message, error)
        }
    }

    override suspend fun lookupBySha256(apiKey: String, sha256: String): VirusTotalFileReport? {
        val body = executeForBody(
            request = requestBuilder(apiKey, "$API_BASE/files/$sha256").get().build(),
            notFoundReturnsNull = true,
        ) ?: return null
        return parseOrThrow { VirusTotalResponseParser.parseFileReport(json, body, sha256) }
    }

    override suspend fun upload(apiKey: String, source: DataEntity, fileName: String, size: Long): VirusTotalAnalysis {
        if (size > VirusTotalRepository.MAX_UPLOAD_SIZE_BYTES) {
            throw VirusTotalException(
                VirusTotalErrorKind.FILE_TOO_LARGE,
                "File size $size exceeds the VirusTotal limit of ${VirusTotalRepository.MAX_UPLOAD_SIZE_BYTES} bytes",
            )
        }
        if (source is DataEntity.StreamDataEntity || source.getSourceTop() is DataEntity.StreamDataEntity) {
            throw VirusTotalException(VirusTotalErrorKind.UNSUPPORTED_SOURCE, "Streamed sources cannot be uploaded for scanning")
        }
        val uploadUrl = if (size > VirusTotalRepository.DIRECT_UPLOAD_LIMIT_BYTES) {
            val body = executeForBody(
                request = requestBuilder(apiKey, "$API_BASE/files/upload_url").get().build(),
                notFoundReturnsNull = false,
            ).orEmpty()
            parseOrThrow { VirusTotalResponseParser.parseUploadUrl(json, body) }
                ?: throw VirusTotalException(VirusTotalErrorKind.HTTP, "VirusTotal did not return an upload URL")
        } else {
            "$API_BASE/files"
        }
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, StreamingRequestBody(source, size))
            .build()
        val body = executeForBody(
            request = requestBuilder(apiKey, uploadUrl).post(multipart).build(),
            notFoundReturnsNull = false,
        ).orEmpty()
        val analysisId = parseOrThrow { VirusTotalResponseParser.parseUploadAnalysisId(json, body) }
            ?: throw VirusTotalException(VirusTotalErrorKind.HTTP, "VirusTotal did not return an analysis id")
        return VirusTotalAnalysis(id = analysisId, status = VirusTotalAnalysisStatus.QUEUED)
    }

    override suspend fun getAnalysis(apiKey: String, analysisId: String): VirusTotalAnalysis {
        val body = executeForBody(
            request = requestBuilder(apiKey, "$API_BASE/analyses/$analysisId").get().build(),
            notFoundReturnsNull = false,
        ).orEmpty()
        return parseOrThrow { VirusTotalResponseParser.parseAnalysis(json, body, analysisId) }
    }

    private suspend fun ensureNetworkAllowed(apiKey: String) {
        if (apiKey.isBlank()) {
            throw VirusTotalException(VirusTotalErrorKind.NO_API_KEY, "No VirusTotal API key configured")
        }
        if (!appSettingsRepository.preferencesFlow.first().allowInternetAccess) {
            throw VirusTotalException(VirusTotalErrorKind.INTERNET_DISABLED, "Internet access is disabled in Network settings")
        }
    }

    private fun requestBuilder(apiKey: String, url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("x-apikey", apiKey.trim())
        .header("Accept", "application/json")

    private suspend fun executeForBody(request: Request, notFoundReturnsNull: Boolean): String? {
        // The API key travels in the header built by requestBuilder; recover it for the network gate.
        ensureNetworkAllowed(request.header("x-apikey").orEmpty())
        return withContext(Dispatchers.IO) {
            val response = try {
                client.newCall(request).execute()
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                Timber.e(error, "VirusTotal: network request failed")
                throw VirusTotalException(VirusTotalErrorKind.NETWORK, error.message, error)
            }
            response.use { mapResponse(it, notFoundReturnsNull) }
        }
    }

    private fun mapResponse(response: Response, notFoundReturnsNull: Boolean): String? = when {
        response.isSuccessful -> response.body.string()
        response.code == 404 && notFoundReturnsNull -> null
        response.code == 401 || response.code == 403 ->
            throw VirusTotalException(VirusTotalErrorKind.UNAUTHORIZED, "VirusTotal rejected the API key (HTTP ${response.code})")

        response.code == 429 ->
            throw VirusTotalException(VirusTotalErrorKind.RATE_LIMITED, "VirusTotal rate limit or quota exceeded")

        else ->
            throw VirusTotalException(VirusTotalErrorKind.HTTP, "VirusTotal request failed (HTTP ${response.code})")
    }

    private fun <T> parseOrThrow(block: () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: VirusTotalException) {
        throw error
    } catch (error: Exception) {
        Timber.e(error, "VirusTotal: failed to parse response")
        throw VirusTotalException(VirusTotalErrorKind.HTTP, error.message, error)
    }

    /** Streams the source on every write attempt so OkHttp retries/redirects can re-read the file. */
    private class StreamingRequestBody(private val source: DataEntity, private val size: Long) : RequestBody() {
        override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

        override fun contentLength(): Long = if (size >= 0L) size else -1L

        override fun writeTo(sink: BufferedSink) {
            val stream = source.getInputStream() ?: throw IOException("Source cannot be opened for upload")
            stream.use { input -> sink.writeAll(input.source()) }
        }
    }

    private companion object {
        const val API_BASE = "https://www.virustotal.com/api/v3"
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
