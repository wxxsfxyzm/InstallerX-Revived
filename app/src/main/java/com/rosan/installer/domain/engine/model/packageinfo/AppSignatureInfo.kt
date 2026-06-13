// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

data class AppSignatureInfo(
    val verified: Boolean,
    val signerSha256Set: Set<String>,
    val certificates: List<SignatureCertificateInfo>,
    val signingCertificateHistory: List<SignatureCertificateInfo> = emptyList(),
    val hasMultipleSigners: Boolean = false,
    val verifiedSchemes: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {
    val primarySha256: String?
        get() = certificates.firstOrNull()?.sha256 ?: signerSha256Set.firstOrNull()

    val signingCertificateHistorySha256Set: Set<String>
        get() = signingCertificateHistory.mapTo(linkedSetOf()) { it.sha256 }

    val allKnownSha256Set: Set<String>
        get() = linkedSetOf<String>().apply {
            addAll(signerSha256Set)
            addAll(signingCertificateHistorySha256Set)
        }
}

data class SignatureCertificateInfo(
    val sha256: String,
    val sha1: String,
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String?,
    val validUntil: String?,
    val publicKeyAlgorithm: String?,
    val signatureAlgorithm: String?
)
