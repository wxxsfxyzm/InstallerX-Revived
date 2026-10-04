// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import java.util.zip.ZipEntry

internal const val ZIP_COMPRESSION_ZSTD_DEPRECATED = 20
internal const val ZIP_COMPRESSION_ZSTD = 93
internal const val ZIP_COMPRESSION_XZ = 95

internal fun isSupportedZipCompressionMethod(compressionMethod: Int): Boolean = compressionMethod == ZipEntry.STORED ||
    compressionMethod == ZipEntry.DEFLATED ||
    compressionMethod == ZIP_COMPRESSION_ZSTD_DEPRECATED ||
    compressionMethod == ZIP_COMPRESSION_ZSTD ||
    compressionMethod == ZIP_COMPRESSION_XZ

/** Compression methods supported by the bundled ZIP decoders. */
fun requireSupportedZipCompressionMethod(compressionMethod: Int, entryName: String) {
    if (isSupportedZipCompressionMethod(compressionMethod)) return

    throw AnalyseException(
        errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
        message = "Unsupported ZIP compression method $compressionMethod for entry '$entryName'; " +
            "Please submit a issue with the install package you trying install",
    )
}
