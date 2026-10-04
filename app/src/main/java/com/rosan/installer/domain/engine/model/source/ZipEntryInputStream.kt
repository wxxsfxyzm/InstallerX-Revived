// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

import java.io.InputStream
import java.util.zip.ZipException
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.tukaani.xz.SingleXZInputStream

// Limit total decoder memory, including the dictionary and any additional XZ filters.
internal const val XZ_MEMORY_LIMIT_KIB = 64 * 1024

internal fun openXzInputStream(input: InputStream): InputStream = SingleXZInputStream(input.buffered(), XZ_MEMORY_LIMIT_KIB)

/** Applies the same XZ limit to Commons-backed analysis and installation entry streams. */
internal fun openZipEntryInputStream(zipFile: ZipFile, entry: ZipArchiveEntry): InputStream {
    requireSupportedZipCompressionMethod(entry.method, entry.name)
    if (entry.method != ZIP_COMPRESSION_XZ) return zipFile.getInputStream(entry)

    // Raw streams bypass Commons' feature checks, so preserve its encryption rejection.
    if (entry.generalPurposeBit.usesEncryption()) {
        throw UnsupportedZipFeatureException(UnsupportedZipFeatureException.Feature.ENCRYPTION, entry)
    }
    val raw = zipFile.getRawInputStream(entry) ?: throw ZipException("Missing ZIP entry payload: ${entry.name}")
    return try {
        openXzInputStream(raw)
    } catch (error: Exception) {
        try {
            raw.close()
        } catch (closeError: Exception) {
            error.addSuppressed(closeError)
        }
        throw error
    }
}
