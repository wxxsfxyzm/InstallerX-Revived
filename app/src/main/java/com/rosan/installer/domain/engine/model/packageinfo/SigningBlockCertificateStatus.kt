// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

/**
 * Compares unverified signer certificate declarations read from APK Signing Blocks.
 * This is intentionally separate from full APK signature verification.
 */
enum class SigningBlockCertificateStatus {
    NOT_INSTALLED,
    MATCH,
    UNKNOWN,
}

fun PackageAnalysisResult.selectedSigningBlockCertificateStatus(): SigningBlockCertificateStatus? {
    val pendingSignatures = appEntities
        .asSequence()
        .filter { it.selected }
        .map { it.app }
        .mapNotNull { entity ->
            when (entity) {
                is AppEntity.BaseEntity -> entity.signatureInfo
                is AppEntity.SplitEntity -> entity.signatureInfo
                else -> null
            }
        }
        .filter { it.verificationStatus == SignatureVerificationStatus.SIGNING_BLOCK_ONLY }
        .toList()

    if (pendingSignatures.isEmpty()) return null
    if (pendingSignatures.any { it.signerSha256Set.isEmpty() }) return SigningBlockCertificateStatus.UNKNOWN
    val installedInfo = installedAppInfo ?: return SigningBlockCertificateStatus.NOT_INSTALLED
    val installedSignature = installedInfo.signatureInfo
    if (installedSignature == null || installedSignature.signerSha256Set.isEmpty()) {
        return SigningBlockCertificateStatus.UNKNOWN
    }
    return if (pendingSignatures.all { it.compareSigningIdentity(installedSignature) == SigningIdentityComparison.MATCH }) {
        SigningBlockCertificateStatus.MATCH
    } else {
        SigningBlockCertificateStatus.UNKNOWN
    }
}
