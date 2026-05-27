// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences.theme

enum class ThemeColorSpec(val displayName: String) {
    SPEC_2021("Material 3 (2021)"),
    SPEC_2025("Expressive (2025)");

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SPEC_2025
    }
}
