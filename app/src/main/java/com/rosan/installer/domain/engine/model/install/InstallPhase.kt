// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.install

/**
 * The active part of a PackageInstaller-backed installation.
 */
enum class InstallPhase {
    /**
     * Selected APK payloads are being written into the PackageInstaller session.
     */
    WRITING,

    /**
     * All payloads have been staged and PackageInstaller is processing the committed session.
     */
    INSTALLING,
}
