package com.rosan.installer.domain.engine.model

import com.rosan.installer.domain.session.model.SelectInstallEntity

/**
 * Holds the complete result of analysing a single package.
 * It contains both the information parsed from the installation file(s)
 * and the information about the currently installed version of the app on the system.
 */
data class PackageAnalysisResult(
    val packageName: String,
    val sessionMode: SessionMode,
    val appEntities: List<SelectInstallEntity>,
    val seedColor: Int? = null,
    val installedAppInfo: InstalledAppInfo?,
    val signatureMatchStatus: SignatureMatchStatus,
    val identityStatus: PackageIdentityStatus
)