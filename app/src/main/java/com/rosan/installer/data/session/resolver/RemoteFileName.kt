// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
// Filename handling follows vvb2060/PackageInstaller's DownloadEngine.
package com.rosan.installer.data.session.resolver

import android.webkit.URLUtil
import java.net.URLDecoder
import okhttp3.HttpUrl

/** Uses the preflight response and final redirect URL without another request. */
internal fun remoteFileName(contentDisposition: String?, url: HttpUrl): String? = decodeRemoteFileName(
    URLUtil.guessFileName(url.toString(), contentDisposition, null),
)

internal fun decodeRemoteFileName(fileName: String): String? {
    val decoded = try {
        URLDecoder.decode(fileName, "UTF-8")
    } catch (_: IllegalArgumentException) {
        fileName
    }
    return decoded
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .filterNot { it.isISOControl() }
        .trim()
        .takeIf { it.isNotEmpty() && it != "." && it != ".." }
}
