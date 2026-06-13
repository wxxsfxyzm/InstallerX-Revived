// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.PackageSignatureAnalysis
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.packageinfo.analyzePackageSignatureSelection
import com.rosan.installer.domain.session.model.SelectInstallEntity

/**
 * Signature matcher.
 */
class SignatureMatcher(
    private val installedPackageSignatureReader: InstalledPackageSignatureReader
) {
    fun match(
        baseEntity: AppEntity.BaseEntity?,
        installedInfo: InstalledAppInfo?
    ): SignatureMatchStatus {
        val pendingSignatureInfo = baseEntity?.signatureInfo?.takeIf { it.verified }
        val pendingSignerSet = pendingSignatureInfo?.signerSha256Set?.takeIf { it.isNotEmpty() }
        val installedSignatureInfo = installedInfo?.signatureInfo
        val installedSignerSet = installedSignatureInfo?.signerSha256Set?.takeIf { it.isNotEmpty() }
        return when {
            installedInfo == null -> SignatureMatchStatus.NOT_INSTALLED
            pendingSignatureInfo == null || pendingSignerSet == null || installedSignerSet == null ->
                SignatureMatchStatus.UNKNOWN_ERROR

            pendingSignerSet == installedSignerSet ->
                SignatureMatchStatus.MATCH

            isRotationCompatibleByInstalledPackage(installedInfo, pendingSignerSet) ->
                SignatureMatchStatus.ROTATION_COMPATIBLE

            isCandidateRotationUnconfirmed(pendingSignatureInfo, pendingSignerSet, installedSignerSet) ->
                SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED

            else ->
                SignatureMatchStatus.MISMATCH
        }
    }

    fun analyzeSelection(
        selectableEntities: List<SelectInstallEntity>,
        installedInfo: InstalledAppInfo?
    ): PackageSignatureAnalysis = selectableEntities.analyzePackageSignatureSelection(installedInfo)

    private fun isRotationCompatibleByInstalledPackage(
        installedInfo: InstalledAppInfo,
        pendingSignerSet: Set<String>
    ): Boolean {
        val installedSignatureInfo = installedInfo.signatureInfo ?: return false
        if (installedSignatureInfo.hasMultipleSigners || pendingSignerSet.size != 1) return false

        val hasHistoryMatch = pendingSignerSet.all { sha256 ->
            sha256 in installedSignatureInfo.signingCertificateHistorySha256Set
        }
        val packageManagerConfirms = pendingSignerSet.all { sha256 ->
            installedPackageSignatureReader.hasSigningCertificate(installedInfo.packageName, sha256)
        }
        return hasHistoryMatch || packageManagerConfirms
    }

    private fun isCandidateRotationUnconfirmed(
        pendingSignatureInfo: AppSignatureInfo,
        pendingSignerSet: Set<String>,
        installedSignerSet: Set<String>
    ): Boolean {
        if (pendingSignatureInfo.hasMultipleSigners || pendingSignerSet.size != 1 || installedSignerSet.size != 1) {
            return false
        }
        val lineage = pendingSignatureInfo.signingCertificateHistorySha256Set
        return lineage.size > 1 &&
                pendingSignerSet.all { it in lineage } &&
                installedSignerSet.all { it in lineage }
    }
}
