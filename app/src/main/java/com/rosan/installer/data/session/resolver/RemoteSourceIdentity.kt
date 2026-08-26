// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import java.io.EOFException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response

internal data class RemoteSourceIdentity(val url: HttpUrl, val contentLength: Long, val validator: RemoteSourceValidator) {
    fun newRequestBuilder(): Request.Builder = Request.Builder()
        .url(url)
        .header(validator.requestHeaderName, validator.value)

    fun validateResponse(response: Response) {
        if (response.request.url != url) {
            throw IOException("Remote source redirected after preflight: ${response.request.url}")
        }

        val actualValidator = response.header(validator.responseHeaderName)
            ?: throw IOException("Remote source response omitted ${validator.responseHeaderName}")
        if (actualValidator != validator.value) {
            throw IOException(
                "Remote source changed after preflight: " +
                    "expected ${validator.responseHeaderName}=${validator.value}, " +
                    "actual=$actualValidator",
            )
        }
    }

    companion object {
        fun fromResponse(response: Response, contentLength: Long): RemoteSourceIdentity? {
            if (contentLength <= 0L) return null
            val validator = RemoteSourceValidator.fromResponse(response) ?: return null
            return RemoteSourceIdentity(response.request.url, contentLength, validator)
        }
    }
}

internal sealed class RemoteSourceValidator(val requestHeaderName: String, val responseHeaderName: String, val value: String) {
    class StrongEtag(value: String) :
        RemoteSourceValidator(
            requestHeaderName = "If-Match",
            responseHeaderName = "ETag",
            value = value,
        )

    companion object {
        fun fromResponse(response: Response): RemoteSourceValidator? = response.header("ETag")
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.startsWith("W/", ignoreCase = true) }
            ?.let(::StrongEtag)
    }
}

internal class ExpectedLengthInputStream(input: InputStream, private val expectedLength: Long) : FilterInputStream(input) {
    private var bytesRead = 0L
    private var endVerified = false

    init {
        require(expectedLength >= 0L) { "expectedLength must not be negative" }
    }

    override fun read(): Int {
        if (bytesRead == expectedLength) {
            verifyEnd()
            return -1
        }

        val value = `in`.read()
        if (value < 0) throw incompleteSource()
        bytesRead++
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRead == expectedLength) {
            verifyEnd()
            return -1
        }

        val boundedLength = minOf(length.toLong(), expectedLength - bytesRead).toInt()
        val count = `in`.read(buffer, offset, boundedLength)
        if (count < 0) throw incompleteSource()
        if (count == 0) return 0
        bytesRead += count
        return count
    }

    private fun verifyEnd() {
        if (endVerified) return
        if (`in`.read() >= 0) {
            throw IOException(
                "Remote source exceeded expected length: expected=$expectedLength",
            )
        }
        endVerified = true
    }

    private fun incompleteSource() = EOFException(
        "Remote source ended before expected length: " +
            "expected=$expectedLength, actual=$bytesRead",
    )
}
