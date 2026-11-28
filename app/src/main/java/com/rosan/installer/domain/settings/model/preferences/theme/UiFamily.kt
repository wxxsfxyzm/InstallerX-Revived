// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences.theme

/** The UI design family used by settings and installer screens. */
enum class UiFamily {
    MATERIAL3,
    MIUIX,
    ;

    companion object {
        fun fromValueOrNull(value: String?) = entries.find { it.name == value }
    }
}
