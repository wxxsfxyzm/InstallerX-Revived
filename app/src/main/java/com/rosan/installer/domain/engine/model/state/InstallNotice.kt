// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.state

import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.PackageSignatureAnalysis
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus

data class SignatureNoticeDetails(
    val pendingSignatureInfo: AppSignatureInfo?,
    val installedSignatureInfo: AppSignatureInfo?,
    val packageSignatureAnalysis: PackageSignatureAnalysis = PackageSignatureAnalysis()
)

/**
 * Represents specific notices (warnings or informational messages)
 * generated during package analysis.
 */
sealed class InstallNotice {
    // --- Warnings (Usually displayed with error/tertiary colors) ---
    data object Downgrade : InstallNotice()
    data class SignatureSummary(
        val status: SignatureMatchStatus,
        val details: SignatureNoticeDetails? = null,
        val hasPackageSignatureIssues: Boolean = false
    ) : InstallNotice()

    data class SignatureMismatch(
        val details: SignatureNoticeDetails? = null,
        val hasPackageSignatureIssues: Boolean = false
    ) : InstallNotice()

    data class SignatureUnknown(
        val details: SignatureNoticeDetails? = null,
        val hasPackageSignatureIssues: Boolean = false
    ) : InstallNotice()
    data object SdkIncompatible : InstallNotice()
    data object Arch32On64 : InstallNotice()
    data class Emulated(val appArch: Architecture, val sysArch: Architecture) : InstallNotice()

    // --- Info (Usually displayed with primary/neutral colors) ---
    data object Identical : InstallNotice()
    data class Xposed(val minApi: String?, val targetApi: String?, val description: String?) : InstallNotice()
}
