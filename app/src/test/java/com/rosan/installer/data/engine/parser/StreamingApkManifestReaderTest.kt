// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StreamingApkManifestReaderTest {
    @Test
    fun `stops reading after the manifest and closes the stream`() {
        val manifest = "binary-manifest".toByteArray()
        val bytes = apk("AndroidManifest.xml" to manifest, "classes.dex" to Random(1).nextBytes(1024 * 1024))
        val source = object : ByteArrayInputStream(bytes) {
            var closed = false
            override fun close() {
                closed = true
            }
        }
        assertContentEquals(manifest, readStreamingApkManifest(source))
        assertTrue(source.available() > bytes.size / 2, "Later APK payload must not be drained")
        assertTrue(source.closed)
    }

    @Test
    fun `handles preceding entries and falls back when the scan budget is exceeded`() {
        val bytes = apk("resources.arsc" to ByteArray(256), "AndroidManifest.xml" to byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), readStreamingApkManifest(bytes.inputStream(), maxPrefixBytes = 256))
        assertFailsWith<IOException> { readStreamingApkManifest(bytes.inputStream(), maxPrefixBytes = 128) }
    }

    @Test
    fun `rejects missing empty and oversized manifests`() {
        assertFailsWith<IOException> { readStreamingApkManifest(apk("classes.dex" to byteArrayOf(1)).inputStream()) }
        assertFailsWith<IOException> { readStreamingApkManifest(apk("AndroidManifest.xml" to byteArrayOf()).inputStream()) }
        assertFailsWith<IOException> {
            readStreamingApkManifest(apk("AndroidManifest.xml" to ByteArray(33)).inputStream(), maxManifestBytes = 32)
        }
    }

    @Test
    fun `bounded manifest reads accept the limit and reject one extra byte`() {
        assertContentEquals(ByteArray(32), readBoundedManifest(ByteArray(32).inputStream(), 32))
        assertFailsWith<IOException> { readBoundedManifest(ByteArray(33).inputStream(), 32) }
    }

    private fun apk(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
    }.toByteArray()
}
