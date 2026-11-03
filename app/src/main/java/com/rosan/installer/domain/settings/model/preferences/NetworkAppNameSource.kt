// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

enum class NetworkAppNameSource(val value: String) {
    ServerFilename("server_filename"),
    ApkResources("apk_resources"),
    ;

    companion object {
        fun fromValue(value: String?): NetworkAppNameSource = entries.firstOrNull { it.value == value } ?: ServerFilename
    }
}
