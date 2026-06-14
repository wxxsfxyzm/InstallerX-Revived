// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.PackageSignatureAnalysis
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.packageinfo.analyzePackageSignatureMatch
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
    ): SignatureMatchStatus = baseEntity.analyzePackageSignatureMatch(
        installedInfo = installedInfo,
        hasSigningCertificate = installedPackageSignatureReader::hasSigningCertificate
    )

    fun analyzeSelection(
        selectableEntities: List<SelectInstallEntity>,
        installedInfo: InstalledAppInfo?
    ): PackageSignatureAnalysis = selectableEntities.analyzePackageSignatureSelection(installedInfo)

}
