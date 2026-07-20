// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

import com.rosan.installer.domain.engine.exception.AnalyseException
import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import java.util.zip.ZipEntry

/**
 * InstallerX only supports the compression methods required by regular ZIP/APK payloads.
 */
fun requireSupportedZipCompressionMethod(compressionMethod: Int, entryName: String) {
    if (compressionMethod == ZipEntry.STORED || compressionMethod == ZipEntry.DEFLATED) return

    throw AnalyseException(
        errorType = AnalyseErrorType.ALL_FILES_UNSUPPORTED,
        message = "Unsupported ZIP compression method $compressionMethod for entry '$entryName'; " +
                "only STORE (0) and DEFLATE (8) are supported"
    )
}
