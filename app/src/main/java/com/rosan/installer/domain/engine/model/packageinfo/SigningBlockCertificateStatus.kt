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
    UNKNOWN
}

fun PackageAnalysisResult.selectedSigningBlockCertificateStatus(): SigningBlockCertificateStatus? {
    val pendingSignerSets = appEntities
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
        .map { it.signerSha256Set }
        .toList()

    if (pendingSignerSets.isEmpty()) return null
    if (pendingSignerSets.any { it.isEmpty() }) return SigningBlockCertificateStatus.UNKNOWN
    val installedInfo = installedAppInfo ?: return SigningBlockCertificateStatus.NOT_INSTALLED
    val installedSignerSet = installedInfo.signatureInfo?.signerSha256Set
    if (installedSignerSet.isNullOrEmpty()) {
        return SigningBlockCertificateStatus.UNKNOWN
    }
    return if (pendingSignerSets.all { it == installedSignerSet }) {
        SigningBlockCertificateStatus.MATCH
    } else {
        SigningBlockCertificateStatus.UNKNOWN
    }
}
