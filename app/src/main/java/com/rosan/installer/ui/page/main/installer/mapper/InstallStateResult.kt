// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.mapper

import androidx.compose.ui.graphics.Color
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel

// Encapsulate UI resources to keep the function signature clean
data class InstallNoticeResources(
    // String
    val tagDowngrade: String,
    val textDowngrade: String,
    val tagSignature: String,
    val tagSignatureMatch: String,
    val tagSignatureRotation: String,
    val tagSignatureRotationUnconfirmed: String,
    val textSigNewInstall: String,
    val textSigMatch: String,
    val textSigRotationCompatible: String,
    val textSigCandidateRotationUnconfirmed: String,
    val textSigMismatch: String,
    val textSigUnknown: String,
    val textSigAnalysisIssue: String,
    val labelPendingSignature: String,
    val labelInstalledSignature: String,
    val labelSignatureAnalysisIssues: String,
    val labelSignatureVerificationFailedFiles: String,
    val labelSignatureSplitMismatchFiles: String,
    val labelSignatureDuplicateSplitNames: String,
    val labelSignatureSchemes: String,
    val labelSignatureCertificate: String,
    val labelSignatureCurrentCertificate: String,
    val labelSignatureCertificateLineage: String,
    val labelSignatureLineageCertificate: String,
    val labelSignatureCurrentMarker: String,
    val labelSignatureSha256: String,
    val labelSignatureSha1: String,
    val labelSignatureSubject: String,
    val labelSignatureIssuer: String,
    val labelSignatureValidFrom: String,
    val labelSignatureValidUntil: String,
    val labelSignaturePublicKeyAlgorithm: String,
    val labelSignatureAlgorithm: String,
    val labelSignatureWarnings: String,
    val labelSignatureErrors: String,
    val labelSignatureNoCertificates: String,
    val tagSdk: String,
    val textSdkIncompatible: String,
    val tagArch32: String,
    val textArch32: String,
    val tagEmulated: String,
    val textArchMismatchFormat: String, // Expecting a string with 2 placeholders
    val tagIdentical: String,
    val textIdentical: String,
    // Xposed specific strings
    val tagXposed: String,
    val labelXposedMinApi: String,
    val labelXposedTargetApi: String,

    // Color
    val errorColor: Color,
    val tertiaryColor: Color,
    val primaryColor: Color
)

// Return type containing the list and the button ID
data class InstallStateResult(
    val notices: List<NoticeModel>,
    val buttonTextId: Int
)
