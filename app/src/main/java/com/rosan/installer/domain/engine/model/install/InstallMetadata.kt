// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.install

data class InstallMetadata(
    val sourceUris: List<String> = emptyList(),
    val referrerUri: String? = null
) {
    companion object {
        val Empty = InstallMetadata()
    }
}
