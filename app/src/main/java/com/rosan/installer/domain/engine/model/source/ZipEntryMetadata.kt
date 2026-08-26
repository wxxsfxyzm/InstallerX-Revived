// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

data class ZipEntryMetadata(val uncompressedSize: Long, val compressedSize: Long, val crc: Long, val compressionMethod: Int)

interface ZipEntryMetadataSource {
    val zipEntryMetadata: ZipEntryMetadata?
}
