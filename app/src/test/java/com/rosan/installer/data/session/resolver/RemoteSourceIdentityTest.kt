// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class RemoteSourceIdentityTest {
    @Test
    fun `strong etag pins the final URL and adds If-Match`() {
        val preflight = response(
            url = "https://cdn.example/final.apk",
            headers = mapOf("ETag" to "\"release-1\"")
        )
        val identity = requireNotNull(RemoteSourceIdentity.fromResponse(preflight, 100L))

        assertEquals("https://cdn.example/final.apk", identity.url.toString())
        assertIs<RemoteSourceValidator.StrongEtag>(identity.validator)
        assertEquals("\"release-1\"", identity.newRequestBuilder().build().header("If-Match"))
    }

    @Test
    fun `last modified is not sufficient for streaming identity`() {
        val preflight = response(
            headers = mapOf(
                "ETag" to "W/\"release-1\"",
                "Last-Modified" to "Fri, 31 Jul 2026 12:00:00 GMT"
            )
        )
        assertNull(RemoteSourceIdentity.fromResponse(preflight, 100L))
    }

    @Test
    fun `streaming identity requires a stable validator`() {
        assertNull(RemoteSourceIdentity.fromResponse(response(), 100L))
    }

    @Test
    fun `response validation rejects URL and validator changes`() {
        val identity = requireNotNull(
            RemoteSourceIdentity.fromResponse(
                response(headers = mapOf("ETag" to "\"release-1\"")),
                100L
            )
        )

        assertFailsWith<IOException> {
            identity.validateResponse(
                response(
                    url = "https://cdn.example/other.apk",
                    headers = mapOf("ETag" to "\"release-1\"")
                )
            )
        }
        assertFailsWith<IOException> {
            identity.validateResponse(response(headers = mapOf("ETag" to "\"release-2\"")))
        }
    }

    @Test
    fun `expected length stream accepts an exact chunked response`() {
        val input = ExpectedLengthInputStream(
            ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            expectedLength = 3L
        )
        val output = ByteArray(3)

        assertEquals(3, input.read(output))
        assertEquals(-1, input.read())
        assertContentEquals(byteArrayOf(1, 2, 3), output)
    }

    @Test
    fun `expected length stream rejects truncated response`() {
        val input = ExpectedLengthInputStream(
            ByteArrayInputStream(byteArrayOf(1, 2)),
            expectedLength = 3L
        )

        assertEquals(2, input.read(ByteArray(3)))
        assertFailsWith<EOFException> { input.read() }
    }

    @Test
    fun `expected length stream rejects response with trailing bytes`() {
        val input = ExpectedLengthInputStream(
            ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)),
            expectedLength = 3L
        )

        assertEquals(3, input.read(ByteArray(4)))
        assertFailsWith<IOException> { input.read() }
    }

    private fun response(
        url: String = "https://cdn.example/app.apk",
        headers: Map<String, String> = emptyMap()
    ): Response {
        val request = Request.Builder().url(url).build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(206)
            .message("Partial Content")
            .body(ByteArray(0).toResponseBody())
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
    }
}
