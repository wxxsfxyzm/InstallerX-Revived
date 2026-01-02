package com.rosan.installer.data.app.model.entity

import com.rosan.installer.data.app.model.enums.SessionMode
import com.rosan.installer.data.app.model.enums.SignatureMatchStatus
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity

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
    val signatureMatchStatus: SignatureMatchStatus
)