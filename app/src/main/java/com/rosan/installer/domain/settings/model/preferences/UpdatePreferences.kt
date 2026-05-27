// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

/**
 * Define GitHub Update Channels used by InstallerX
 */
enum class GithubUpdateChannel {
    OFFICIAL,
    PROXY_7ED,
    CUSTOM;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: OFFICIAL
    }
}
