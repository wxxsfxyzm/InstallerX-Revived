// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

/**
 * VirusTotal scanning behaviour configured in Network settings.
 *
 * - [OFF]: no VirusTotal UI or requests.
 * - [NORMAL]: optional SHA-256 lookup from the install dialog, with an optional manual upload.
 * - [STRICT]: the file is always uploaded automatically and installation stays blocked until
 *   the analysis finishes. Scan failures (file too large, network errors, ...) are shown to the
 *   user and then allow installation.
 */
enum class VirusTotalMode {
    OFF,
    NORMAL,
    STRICT,
    ;

    companion object {
        fun fromValueOrDefault(value: String?): VirusTotalMode = entries.find { it.name == value } ?: OFF
    }
}
