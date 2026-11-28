// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences.theme

enum class UiStyle {
    MATERIAL3,
    MIUIX,
    COUI,
    ;

    companion object {
        fun fromValueOrDefault(value: String?, fallbackMiuix: Boolean = false): UiStyle =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: if (fallbackMiuix) MIUIX else MATERIAL3
    }
}
