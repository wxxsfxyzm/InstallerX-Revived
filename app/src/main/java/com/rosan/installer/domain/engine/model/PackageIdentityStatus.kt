// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model

enum class PackageIdentityStatus {
    // The packages are exactly identical.
    IDENTICAL,

    // The versions match, but the file contents are different.
    DIFFERENT,

    // Not applicable for comparison (e.g., version mismatch or not installed).
    NOT_APPLICABLE,

    // Error occurred during calculation.
    ERROR
}
