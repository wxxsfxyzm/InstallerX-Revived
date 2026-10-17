// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

/** Source stamp metadata and verification, independent of the APK's app signer identity. */
data class SourceStampInfo(
    val verificationStatus: SourceStampVerificationStatus = SourceStampVerificationStatus.UNVERIFIED,
    val declaredCertificateSha256: String? = null,
    val certificate: SignatureCertificateInfo? = null,
    val certificateLineage: List<SignatureCertificateInfo> = emptyList(),
    val timestampEpochSeconds: Long? = null,
    val infoMessages: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val sha256: String?
        get() = declaredCertificateSha256 ?: certificate?.sha256
}

enum class SourceStampVerificationStatus {
    UNVERIFIED,
    VERIFIED,
    FAILED,
    CERTIFICATE_MISMATCH,
    VERIFICATION_ERROR,
}
