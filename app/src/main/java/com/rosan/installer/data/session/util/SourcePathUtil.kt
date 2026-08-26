// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.util

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import timber.log.Timber

fun String.pathUnify(): String = // Handle paths starting with /mnt/user/0
// This is necessary for compatibility with Android 14+ where /storage is used instead of
// /mnt/user/0 for the primary shared storage directory. Particularly, Samsung devices
    // shell id 2000 only has access to /storage/emulated/0
    if (this.startsWith("/mnt/user/0")) {
        this.replace("/mnt/user/0", "/storage")
    } else {
        this
    }

fun String.getRealPathFromUri(uri: Uri): String {
    if (!this.startsWith("/mnt/appfuse")) {
        return this
    }

    val providerPath = uri.path

    if (providerPath == null) {
        Timber.e("Real path is null for URI: $uri")
        return this
    } else {
        val displayPath = providerPath.decodeSmbProviderPath()
        Timber.d("Provider display path resolved: $displayPath")
        return displayPath
    }
}

internal fun String.decodeSmbProviderPath(): String {
    val decoded = runCatching {
        // URLDecoder follows form semantics for '+'. Protect literal plus signs so this remains
        // equivalent to a single URI percent-decoding pass.
        URLDecoder.decode(
            replace("+", "%2B"),
            StandardCharsets.UTF_8.name(),
        )
    }.getOrDefault(this)

    return if (decoded.startsWith("/smb://", ignoreCase = true)) {
        decoded.drop(1)
    } else {
        this
    }
}
