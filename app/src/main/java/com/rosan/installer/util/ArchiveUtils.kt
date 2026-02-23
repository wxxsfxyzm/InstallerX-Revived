// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.util

import java.io.File
import java.io.FileInputStream

object ArchiveUtils {

    /**
     * Checks if the given byte array starts with a valid ZIP magic number.
     * Requires an array of at least 4 bytes.
     * * @param header The byte array to check.
     * @return true if it matches known ZIP signatures, false otherwise.
     */
    fun isZipMagicNumber(header: ByteArray): Boolean {
        if (header.size < 4) return false

        val b0 = header[0].toInt() and 0xFF
        val b1 = header[1].toInt() and 0xFF
        val b2 = header[2].toInt() and 0xFF
        val b3 = header[3].toInt() and 0xFF

        // All ZIP variants start with 'PK' (0x50 0x4B)
        if (b0 != 0x50 || b1 != 0x4B) return false

        // Standard ZIP / APK (Local File Header) -> PK\x03\x04
        val isStandardZip = (b2 == 0x03 && b3 == 0x04)

        // Empty ZIP (End of Central Directory) -> PK\x05\x06
        val isEmptyZip = (b2 == 0x05 && b3 == 0x06)

        // Spanned ZIP (Data Descriptor) -> PK\x07\x08
        val isSpannedZip = (b2 == 0x07 && b3 == 0x08)

        return isStandardZip || isEmptyZip || isSpannedZip
    }

    /**
     * Checks if the local file has a valid ZIP magic number.
     * * @param file The file to check.
     * @return true if the file starts with a valid ZIP signature, false otherwise.
     */
    fun isZipArchive(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 4) return false

        return try {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4)
                if (fis.read(buffer) == 4) {
                    isZipMagicNumber(buffer)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
