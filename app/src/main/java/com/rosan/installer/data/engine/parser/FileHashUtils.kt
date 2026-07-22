// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.parser

import com.rosan.installer.domain.engine.model.source.DataEntity
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Calculates the SHA-256 hash of this file.
 *
 * @return The hex string of the SHA-256 hash, or null if the file doesn't exist or an error occurs.
 */
fun File.calculateSHA256(): String? {
    // Check if the file is valid before proceeding.
    if (!this.exists() || !this.isFile || !this.canRead()) {
        Timber.w("Cannot calculate hash for non-existent or unreadable file: ${this.path}")
        return null
    }

    // Use a try-catch block for robustness against security or IO exceptions.
    return try {
        FileInputStream(this).use(InputStream::calculateSHA256)
    } catch (e: Exception) {
        Timber.e(e, "Failed to calculate SHA-256 for file: ${this.path}")
        null
    }
}

fun DataEntity.FileEntity.calculateSHA256(): String? = try {
    getInputStream().use(InputStream::calculateSHA256)
} catch (e: Exception) {
    Timber.e(e, "Failed to calculate SHA-256 for source: $path")
    null
}

private fun InputStream.calculateSHA256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var bytesRead: Int
    while (read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
