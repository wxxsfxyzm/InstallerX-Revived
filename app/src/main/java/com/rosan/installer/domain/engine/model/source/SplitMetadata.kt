// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.source

/**
 * UI categorization for APK splits.
 */
enum class SplitType {
    ARCHITECTURE,
    LANGUAGE,
    DENSITY,
    FEATURE
}

/**
 * Selection logic for APK splits.
 */
enum class FilterType {
    NONE,
    ABI,
    DENSITY,
    LANGUAGE
}

/**
 * Parsed split metadata shared by parser and domain selection logic.
 */
data class SplitMetadata(
    val type: SplitType,
    val filterType: FilterType,
    val configValue: String?
)
