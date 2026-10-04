// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import java.util.zip.ZipEntry

internal const val ZIP_COMPRESSION_XZ = 95

/** Compression methods supported by the bundled ZIP decoders. */
fun requireSupportedZipCompressionMethod(compressionMethod: Int, entryName: String) {
    if (compressionMethod == ZipEntry.STORED ||
        compressionMethod == ZipEntry.DEFLATED ||
        compressionMethod == ZIP_COMPRESSION_XZ
    ) {
        return
    }

    throw AnalyseException(
        errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
        message = "Unsupported ZIP compression method $compressionMethod for entry '$entryName'; " +
            "only STORE (0), DEFLATE (8), and XZ (95) are supported",
    )
}
