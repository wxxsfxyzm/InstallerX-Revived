// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.session.util

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import timber.log.Timber

private const val APPFUSE_PREFIX = "/mnt/appfuse"
private const val CANONICAL_EMULATED_STORAGE = "/storage/emulated/0"
private const val PRIMARY_USER_MOUNT = "/mnt/user/0"

// Readlink of /proc/<pid>/fd and some provider URIs expose these aliases of primary shared
// storage. Shell uid 2000 on Samsung Android 14+ can read /storage/emulated/0, not the alias.
private val primaryEmulatedAliases = listOf(
    "$PRIMARY_USER_MOUNT/emulated/0",
    "/mnt/pass_through/0/emulated/0",
    "/mnt/androidwritable/0/emulated/0",
    "/mnt/runtime/default/emulated/0",
    "/mnt/runtime/read/emulated/0",
    "/mnt/runtime/write/emulated/0",
    "/mnt/runtime/full/emulated/0",
    "/storage/self/primary",
)

// App-private external directories are not safe to reopen with java.io.File on Android 11+.
private val scopedExternalAppData = Regex(
    "^/(?:storage/(?:emulated/\\d+|[^/]+)|sdcard)/Android/(?:data|obb)(?:/|$)",
)

internal data class ResolvedSourcePath(
    val displayPath: String,
    val filePath: String?,
)

fun String.pathUnify(): String {
    primaryEmulatedAliases.firstOrNull { this == it || startsWith("$it/") }?.let { alias ->
        return CANONICAL_EMULATED_STORAGE + removePrefix(alias)
    }
    if (this == PRIMARY_USER_MOUNT || startsWith("$PRIMARY_USER_MOUNT/")) {
        return "/storage" + removePrefix(PRIMARY_USER_MOUNT)
    }
    return this
}

fun String.getRealPathFromUri(uri: Uri): String = resolveProviderBackedPath(uri.path).displayPath

/**
 * Classifies a `/proc/<pid>/fd` target together with the provider URI path.
 *
 * Scoped external app data, virtual provider paths, and SMB display paths keep [filePath] null
 * so callers continue reading the content descriptor instead of reopening them as a [java.io.File].
 */
internal fun String.resolveProviderBackedPath(providerPath: String?): ResolvedSourcePath {
    if (isNotEmpty() && !startsWith(APPFUSE_PREFIX)) {
        return pathUnify().toFilesystemSource()
    }
    if (providerPath == null) {
        Timber.e("Real path is null for provider-backed fd path: $this")
        return ResolvedSourcePath(displayPath = this, filePath = null)
    }
    return providerPath.toProviderDisplaySource(log = true)
}

/** Direct `file://` paths. Aliases are normalized; canonical and non-external paths stay as-is. */
internal fun String.resolveDirectFilePath(): ResolvedSourcePath {
    val unified = pathUnify()
    return ResolvedSourcePath(displayPath = unified, filePath = unified)
}

/**
 * File path to use when a provider refuses the URI.
 *
 * Returns null for a missing, virtual, SMB, or scoped external path so the original resolver
 * error is preserved. [isReadableFile] is only consulted for a non-scoped shared-storage path.
 */
internal fun providerFileFallbackPath(providerPath: String?, isReadableFile: (String) -> Boolean): String? {
    if (providerPath.isNullOrBlank()) return null
    val candidate = providerPath.toProviderDisplaySource(log = false).filePath ?: return null
    return candidate.takeIf(isReadableFile)
}

/** Path stored on a descriptor entity. Scoped app-data paths contribute only their leaf name. */
internal fun ResolvedSourcePath.descriptorEntityPath(contentUri: String): String = when {
    filePath != null -> filePath
    displayPath.isScopedExternalAppDataPath() -> displayPath.substringAfterLast('/').ifBlank { contentUri }
    displayPath.hasPathTraversal() -> contentUri
    displayPath.isNotBlank() -> displayPath
    else -> contentUri
}

/**
 * Source identity. A scoped external path is represented by the content URI so later `File`
 * consumers do not reopen `/Android/data` or `/Android/obb`.
 */
internal fun ResolvedSourcePath.sourceIdentityPath(contentUri: String): String = when {
    filePath != null -> filePath
    displayPath.isScopedExternalAppDataPath() || displayPath.hasPathTraversal() -> contentUri
    displayPath.isNotBlank() -> displayPath
    else -> contentUri
}

internal fun String.isScopedExternalAppDataPath(): Boolean = scopedExternalAppData.containsMatchIn(this)

private fun String.toFilesystemSource(): ResolvedSourcePath = if (isScopedExternalAppDataPath()) {
    ResolvedSourcePath(displayPath = this, filePath = null)
} else {
    ResolvedSourcePath(displayPath = this, filePath = this)
}

private fun String.toProviderDisplaySource(log: Boolean): ResolvedSourcePath {
    val displayPath = decodeSmbProviderPath()
    if (log) Timber.d("Provider display path resolved: $displayPath")
    if (displayPath.startsWith("smb://", ignoreCase = true)) {
        return ResolvedSourcePath(displayPath = displayPath, filePath = null)
    }
    val unified = displayPath.pathUnify()
    if (unified.hasPathTraversal() ||
        !unified.isCanonicalSharedStoragePath() ||
        unified.isScopedExternalAppDataPath()
    ) {
        val identity = if (unified.isScopedExternalAppDataPath() && !unified.hasPathTraversal()) {
            unified
        } else {
            displayPath
        }
        return ResolvedSourcePath(displayPath = identity, filePath = null)
    }
    return ResolvedSourcePath(displayPath = unified, filePath = unified)
}

private fun String.isCanonicalSharedStoragePath(): Boolean = startsWith("/storage/") || startsWith("/sdcard/")

private fun String.hasPathTraversal(): Boolean = split('/').any { it == ".." }

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
