// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

import com.rosan.installer.domain.session.model.SelectInstallEntity

data class PackageSignatureAnalysis(
    val verificationFailedFiles: List<String> = emptyList(),
    val splitSignatureMismatchFiles: List<String> = emptyList(),
    val duplicateSplitNames: List<String> = emptyList()
) {
    val hasIssues: Boolean
        get() = verificationFailedFiles.isNotEmpty() ||
                splitSignatureMismatchFiles.isNotEmpty() ||
                duplicateSplitNames.isNotEmpty()
}

fun List<SelectInstallEntity>.analyzePackageSignatureSelection(
    installedInfo: InstalledAppInfo?
): PackageSignatureAnalysis {
    val selectedApps = filter { it.selected }.map { it.app }
    val selectedApks = selectedApps.filter { it is AppEntity.BaseEntity || it is AppEntity.SplitEntity }
    if (selectedApks.isEmpty()) return PackageSignatureAnalysis()

    val selectedBase = selectedApks.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val selectedSplits = selectedApks.filterIsInstance<AppEntity.SplitEntity>()

    val verificationFailedFiles = selectedApks
        .filter { entity -> entity.signatureInfo()?.verified != true }
        .map { it.signatureDisplayName() }
        .distinct()

    val referenceSignerSet = selectedBase?.signatureInfo
        ?.takeIf { it.verified }
        ?.signerSha256Set
        ?.takeIf { it.isNotEmpty() }
        ?: installedInfo?.signatureInfo
            ?.signerSha256Set
            ?.takeIf { it.isNotEmpty() }

    val splitSignatureMismatchFiles = if (referenceSignerSet == null) {
        emptyList()
    } else {
        selectedSplits
            .filter { split ->
                val splitInfo = split.signatureInfo
                splitInfo?.verified == true &&
                        splitInfo.signerSha256Set.isNotEmpty() &&
                        splitInfo.signerSha256Set != referenceSignerSet
            }
            .map { it.signatureDisplayName() }
            .distinct()
    }

    val duplicateSplitNames = selectedSplits
        .groupBy { it.splitName }
        .filterValues { it.size > 1 }
        .keys
        .sorted()

    return PackageSignatureAnalysis(
        verificationFailedFiles = verificationFailedFiles,
        splitSignatureMismatchFiles = splitSignatureMismatchFiles,
        duplicateSplitNames = duplicateSplitNames
    )
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
