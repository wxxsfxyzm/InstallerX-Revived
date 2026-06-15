// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.data.engine.signature.InstalledPackageSignatureReader
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.provider.InstalledAppInfoProvider
import com.rosan.installer.util.pm.compatVersionCode
import com.rosan.installer.util.pm.isPackageArchivedCompat
import java.io.File

class InstalledAppInfoProviderImpl(
    private val context: Context,
    private val installedPackageSignatureReader: InstalledPackageSignatureReader
) : InstalledAppInfoProvider {
    override fun getByPackageName(packageName: String, includeSignature: Boolean): InstalledAppInfo? {
        val packageManager = context.packageManager
        val signatureInfo = if (includeSignature) installedPackageSignatureReader.read(packageName) else null
        val signatureHash = signatureInfo?.primarySha256
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            }

            val applicationInfo = packageInfo.applicationInfo
            val flags = applicationInfo?.flags ?: 0
            val isUninstalled = !flags.hasFlag(ApplicationInfo.FLAG_INSTALLED)
            val packageSize = if (applicationInfo != null && !isUninstalled && applicationInfo.sourceDir != null) {
                val baseFile = File(applicationInfo.sourceDir)
                var totalSize = if (baseFile.exists()) baseFile.length() else 0L
                applicationInfo.splitSourceDirs?.forEach { path ->
                    val splitFile = File(path)
                    if (splitFile.exists()) {
                        totalSize += splitFile.length()
                    }
                }
                totalSize
            } else {
                0L
            }

            InstalledAppInfo(
                packageName = packageName,
                icon = applicationInfo?.loadIcon(packageManager),
                label = applicationInfo?.loadLabel(packageManager)?.toString() ?: packageName,
                versionCode = packageInfo.compatVersionCode,
                versionName = packageInfo.versionName ?: "",
                applicationInfo = applicationInfo,
                minSdk = applicationInfo?.minSdkVersion,
                targetSdk = applicationInfo?.targetSdkVersion,
                signatureHash = signatureHash,
                signatureInfo = signatureInfo,
                isSystemApp = flags.hasFlag(ApplicationInfo.FLAG_SYSTEM),
                isUninstalled = isUninstalled,
                isArchived = packageManager.isPackageArchivedCompat(packageName),
                packageSize = packageSize,
                sourceDir = applicationInfo?.sourceDir
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
