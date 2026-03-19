// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.state

import com.rosan.installer.domain.device.model.Architecture

/**
 * Represents specific notices (warnings or informational messages)
 * generated during package analysis.
 */
sealed class InstallNotice {
    // --- Warnings (Usually displayed with error/tertiary colors) ---
    data object Downgrade : InstallNotice()
    data object SignatureMismatch : InstallNotice()
    data object SignatureUnknown : InstallNotice()
    data object SdkIncompatible : InstallNotice()
    data object Arch32On64 : InstallNotice()
    data class Emulated(val appArch: Architecture, val sysArch: Architecture) : InstallNotice()

    // --- Info (Usually displayed with primary/neutral colors) ---
    data object Identical : InstallNotice()
    data class Xposed(val minApi: String?, val targetApi: String?, val description: String?) : InstallNotice()
}
