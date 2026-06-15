// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.session.model.SelectInstallEntity

/**
 * Signature analyzer for packages.
 */
class PackageSignatureAnalyzer(
    private val signatureMatcher: SignatureMatcher
) {
    fun match(
        baseEntity: AppEntity.BaseEntity?,
        installedInfo: InstalledAppInfo?
    ) = signatureMatcher.match(baseEntity, installedInfo)

    fun analyzeSelection(
        selectableEntities: List<SelectInstallEntity>,
        installedInfo: InstalledAppInfo?
    ) = signatureMatcher.analyzeSelection(selectableEntities, installedInfo)
}
