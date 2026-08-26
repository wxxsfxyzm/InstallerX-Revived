// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.install

/**
 * Byte progress while selected APK payloads are written into PackageInstaller sessions.
 */
data class InstallWriteProgress(val bytesWritten: Long, val totalBytes: Long) {
    init {
        require(totalBytes > 0L) { "totalBytes must be positive" }
        require(bytesWritten in 0L..totalBytes) {
            "bytesWritten must be between zero and totalBytes"
        }
    }

    val fraction: Float
        get() = (bytesWritten.toDouble() / totalBytes.toDouble()).toFloat()
}
