package com.rosan.installer.data.app.util

import timber.log.Timber
import java.io.File
import java.io.FileInputStream
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
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(this).use { fis ->
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        // Convert the byte array to a hex string and return.
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        Timber.e(e, "Failed to calculate SHA-256 for file: ${this.path}")
        null
    }
}