// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

internal data class SeekableZipArchive(
    val entries: List<SeekableZipEntry>,
    val hasCentralDirectory: Boolean
)

internal data class SeekableZipEntry(
    val name: String,
    val localHeaderOffset: Long,
    val dataOffset: Long,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val crc: Long,
    val compressionMethod: Int,
    val flags: Int
) {
    val isDirectory: Boolean
        get() = name.endsWith('/')
}
