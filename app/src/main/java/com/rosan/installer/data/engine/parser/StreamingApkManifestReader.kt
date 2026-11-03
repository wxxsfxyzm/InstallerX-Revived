// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
// Manifest-first streaming follows vvb2060/PackageInstaller's ZipParser.
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.exception.DescriptorAnalysisUnsupportedException
import com.rosan.installer.domain.engine.model.source.DataEntity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

internal const val MAX_STREAMING_MANIFEST_BYTES = 4 * 1024 * 1024
internal const val MAX_STREAMING_MANIFEST_PREFIX_BYTES = 32 * 1024 * 1024L

/** Stop after the manifest; do not drain the base APK or download later entries. */
internal fun readStreamingApkManifest(
    input: InputStream,
    maxManifestBytes: Int = MAX_STREAMING_MANIFEST_BYTES,
    maxPrefixBytes: Long = MAX_STREAMING_MANIFEST_PREFIX_BYTES,
): ByteArray = ZipInputStream(input).use { zip ->
    var skipped = 0L
    val buffer = ByteArray(16 * 1024)
    repeat(4096) {
        val entry = zip.nextEntry ?: throw IOException("Base APK has no AndroidManifest.xml")
        if (entry.name == "AndroidManifest.xml") return@use readBoundedManifest(zip, maxManifestBytes)
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            skipped += count
            if (skipped > maxPrefixBytes) throw IOException("Base APK manifest exceeds the streaming scan budget")
        }
    }
    throw IOException("Too many APK entries before AndroidManifest.xml")
}

internal fun readBoundedManifest(input: InputStream, maxBytes: Int = MAX_STREAMING_MANIFEST_BYTES): ByteArray = readBoundedArchiveEntry(input, maxBytes, "AndroidManifest.xml")

/** JSON sidecars must not turn a metadata probe into an unbounded network download. */
internal fun UnifiedZipFile.openContainerMetadata(entry: UnifiedZipEntry): InputStream {
    val source = file
    if (source !is DataEntity.FileDescriptorEntity || !source.isNetworkSource) return openEntry(entry)
    return try {
        openEntry(entry).use { readBoundedArchiveEntry(it, MAX_STREAMING_MANIFEST_BYTES, entry.name) }.inputStream()
    } catch (error: IOException) {
        // Type detection must not swallow an over-budget remote sidecar and reinterpret the archive.
        throw DescriptorAnalysisUnsupportedException(source, error)
    }
}

private fun readBoundedArchiveEntry(input: InputStream, maxBytes: Int, name: String): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, 16 * 1024))
    val buffer = ByteArray(16 * 1024)
    while (true) {
        val count = input.read(buffer, 0, minOf(buffer.size.toLong(), maxBytes.toLong() - output.size() + 1).toInt())
        if (count < 0) break
        if (output.size().toLong() + count > maxBytes) throw IOException("$name is too large for streaming analysis")
        output.write(buffer, 0, count)
    }
    if (output.size() == 0) throw IOException("Empty $name")
    return output.toByteArray()
}
