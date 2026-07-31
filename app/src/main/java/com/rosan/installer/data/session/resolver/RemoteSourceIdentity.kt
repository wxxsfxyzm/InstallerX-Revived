// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal data class RemoteSourceIdentity(
    val url: HttpUrl,
    val contentLength: Long,
    val validator: RemoteSourceValidator
) {
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
                        "actual=$actualValidator"
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

internal sealed class RemoteSourceValidator(
    val requestHeaderName: String,
    val responseHeaderName: String,
    val value: String
) {
    class StrongEtag(value: String) : RemoteSourceValidator(
        requestHeaderName = "If-Match",
        responseHeaderName = "ETag",
        value = value
    )

    class LastModified(value: String) : RemoteSourceValidator(
        requestHeaderName = "If-Unmodified-Since",
        responseHeaderName = "Last-Modified",
        value = value
    )

    companion object {
        fun fromResponse(response: Response): RemoteSourceValidator? {
            response.header("ETag")
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.startsWith("W/", ignoreCase = true) }
                ?.let { return StrongEtag(it) }

            return response.header("Last-Modified")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::LastModified)
        }
    }
}
