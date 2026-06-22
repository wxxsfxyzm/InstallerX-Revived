package com.rosan.installer.domain.engine.model.packageinfo

import android.graphics.Bitmap
import com.rosan.installer.domain.engine.model.install.SessionMode
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
    val displayIcon: Bitmap? = null,
    val seedColor: Int? = null,
    val installedAppInfo: InstalledAppInfo?,
    val installedModuleInfo: InstalledModuleInfo? = null,
    val signatureMatchStatus: SignatureMatchStatus,
    val signatureAnalysis: PackageSignatureAnalysis = PackageSignatureAnalysis(),
    val identityStatus: PackageIdentityStatus
)
