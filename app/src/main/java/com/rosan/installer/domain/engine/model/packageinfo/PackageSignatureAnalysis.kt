// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

import com.rosan.installer.domain.session.model.SelectInstallEntity

data class PackageSignatureAnalysis(
    val verificationFailedFiles: List<String> = emptyList(),
    val splitSignatureMismatchFiles: List<String> = emptyList(),
    val duplicateSplitNames: List<String> = emptyList(),
) {
    val hasIssues: Boolean
        get() = verificationFailedFiles.isNotEmpty() ||
            splitSignatureMismatchFiles.isNotEmpty() ||
            duplicateSplitNames.isNotEmpty()
}

fun List<SelectInstallEntity>.analyzePackageSignatureSelection(installedInfo: InstalledAppInfo?): PackageSignatureAnalysis {
    val selectedApps = filter { it.selected }.map { it.app }
    val selectedApks = selectedApps.filter { it is AppEntity.BaseEntity || it is AppEntity.SplitEntity }
    if (selectedApks.isEmpty()) return PackageSignatureAnalysis()

    val selectedBase = selectedApks.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val selectedSplits = selectedApks.filterIsInstance<AppEntity.SplitEntity>()

    val verificationFailedFiles = selectedApks
        .filter { entity -> entity.signatureInfo()?.verified != true }
        .map { it.signatureDisplayName() }
        .distinct()

    val referenceSignature = selectedBase?.signatureInfo?.takeIf { it.verified }
        ?: installedInfo?.signatureInfo
    val installedIdentity = installedInfo?.signatureInfo?.takeIf { it.verified }?.signingIdentity

    val splitComparisons = if (referenceSignature == null) {
        emptyList()
    } else {
        selectedSplits.mapNotNull { split ->
            split.signatureInfo?.takeIf { it.verified }?.let {
                split.signatureDisplayName() to it.compareSigningIdentity(referenceSignature, installedIdentity)
            }
        }
    }
    val splitSignatureMismatchFiles = splitComparisons
        .filter { it.second == SigningIdentityComparison.MISMATCH }.map { it.first }.distinct()
    val unconfirmedSplitFiles = splitComparisons
        .filter { it.second == SigningIdentityComparison.UNKNOWN }.map { it.first }

    val duplicateSplitNames = selectedSplits
        .groupBy { it.splitName }
        .filterValues { it.size > 1 }
        .keys
        .sorted()

    return PackageSignatureAnalysis(
        verificationFailedFiles = (verificationFailedFiles + unconfirmedSplitFiles).distinct(),
        splitSignatureMismatchFiles = splitSignatureMismatchFiles,
        duplicateSplitNames = duplicateSplitNames,
    )
}

fun List<SelectInstallEntity>.analyzePackageSignatureMatch(
    installedInfo: InstalledAppInfo?,
): SignatureMatchStatus {
    val selectedBase = filter { it.selected }
        .map { it.app }
        .filterIsInstance<AppEntity.BaseEntity>()
        .firstOrNull()

    return selectedBase.analyzePackageSignatureMatch(installedInfo)
}

fun AppEntity.BaseEntity?.analyzePackageSignatureMatch(
    installedInfo: InstalledAppInfo?,
): SignatureMatchStatus {
    if (installedInfo == null) return SignatureMatchStatus.NOT_INSTALLED
    val pending = this?.signatureInfo?.takeIf { it.verified } ?: return SignatureMatchStatus.UNKNOWN_ERROR
    val installed = installedInfo.signatureInfo ?: return SignatureMatchStatus.UNKNOWN_ERROR
    val identities = if (pending.fallbackIdentity != null && !installed.isHybrid) {
        listOf(pending.signingIdentity, pending.fallbackIdentity)
    } else {
        listOf(pending.signingIdentity)
    }
    val results = identities.map { it.matchUpdateTo(installed.signingIdentity) }.distinct()
    return results.singleOrNull() ?: SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED
}

private fun SigningIdentity.matchUpdateTo(installed: SigningIdentity): SignatureMatchStatus {
    if (!isComplete || !installed.isComplete) return SignatureMatchStatus.UNKNOWN_ERROR
    if (matches(installed)) return SignatureMatchStatus.MATCH
    if (signerSha256Set.size != 1 || installed.signerSha256Set.size != 1) return SignatureMatchStatus.MISMATCH

    val forward = grants(installed, rollback = false)
    val backward = installed.grants(this, rollback = true)
    return when {
        forward == CapabilityResult.ALLOWED || backward == CapabilityResult.ALLOWED -> SignatureMatchStatus.ROTATION_COMPATIBLE
        forward == CapabilityResult.UNKNOWN || backward == CapabilityResult.UNKNOWN -> SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED
        else -> SignatureMatchStatus.MISMATCH
    }
}

private enum class CapabilityResult { ALLOWED, DENIED, UNKNOWN }

/** Mirrors the direction and hybrid-key restrictions of Android SigningDetails.checkCapability. */
private fun SigningIdentity.grants(other: SigningIdentity, rollback: Boolean): CapabilityResult {
    val primary = signerSha256Set.single()
    val otherPrimary = other.signerSha256Set.single()
    if (isHybrid || other.isHybrid) {
        // Reusing only one member of a hybrid pair is forbidden even if a lineage contains it.
        if ((primary == otherPrimary && classicalSha256 != other.classicalSha256) ||
            (other.isHybrid && primary == other.classicalSha256) ||
            (isHybrid && classicalSha256 == otherPrimary) ||
            (isHybrid && other.isHybrid && classicalSha256 == other.classicalSha256 && primary != otherPrimary)
        ) {
            return CapabilityResult.DENIED
        }
    }
    val required = listOfNotNull(otherPrimary, other.classicalSha256.takeIf { other.isHybrid })
    var unknown = false
    for (sha256 in required) {
        if (sha256 == primary) continue
        if (sha256 !in historySha256) return CapabilityResult.DENIED
        val flags = capabilities[sha256]
        if (flags == null) {
            unknown = true
        } else if (!(if (rollback) flags.rollback else flags.installedData)) {
            return CapabilityResult.DENIED
        }
    }
    return if (unknown) CapabilityResult.UNKNOWN else CapabilityResult.ALLOWED
}

private fun AppEntity.signatureInfo() = when (this) {
    is AppEntity.BaseEntity -> signatureInfo
    is AppEntity.SplitEntity -> signatureInfo
    else -> null
}

private fun AppEntity.signatureDisplayName() = when (this) {
    is AppEntity.BaseEntity -> name
    is AppEntity.SplitEntity -> name
    else -> name
}
