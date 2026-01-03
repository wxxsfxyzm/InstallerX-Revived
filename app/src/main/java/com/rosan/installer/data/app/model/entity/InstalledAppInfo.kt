package com.rosan.installer.data.app.model.entity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.rosan.installer.data.app.util.SignatureUtils
import com.rosan.installer.data.common.util.compatVersionCode
import com.rosan.installer.data.common.util.isPackageArchivedCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

data class InstalledAppInfo(
    val packageName: String,
    val icon: Drawable?,
    val label: String,
    val versionCode: Long,
    val versionName: String,
    val applicationInfo: ApplicationInfo?, // Add this field
    val minSdk: Int?,
    val targetSdk: Int?,
    val signatureHash: String? = null,
    val isSystemApp: Boolean = false,
    val isUninstalled: Boolean = false,
    val isArchived: Boolean = false,
    val packageSize: Long = 0L
) {
    companion object : KoinComponent {
        fun buildByPackageName(packageName: String): InstalledAppInfo? {
            val context: Context = get()
            val packageManager = context.packageManager
            val signatureHash = SignatureUtils.getInstalledAppSignatureHash(context, packageName)
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

                // Check if the app is effectively "uninstalled" (but data kept).
                // If FLAG_INSTALLED is NOT set, it means the app is not installed for the current user.
                val isUninstalled = ((applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_INSTALLED) == 0

                val packageSize = if (applicationInfo != null && !isUninstalled && applicationInfo.sourceDir != null) {
                    // Only calculate size if the app is actually installed and sourceDir is valid.
                    val baseFile = File(applicationInfo.sourceDir)
                    var totalSize = if (baseFile.exists()) baseFile.length() else 0L

                    // Calculate Split APKs size (if any)
                    applicationInfo.splitSourceDirs?.forEach { path ->
                        val splitFile = File(path)
                        if (splitFile.exists()) {
                            totalSize += splitFile.length()
                        }
                    }
                    totalSize
                } else 0L

                InstalledAppInfo(
                    packageName = packageName,
                    icon = applicationInfo?.loadIcon(packageManager),
                    label = applicationInfo?.loadLabel(packageManager)?.toString()
                        ?: packageName, // Fallback to package name
                    versionCode = packageInfo.compatVersionCode,
                    // Safely handle nullable versionName.
                    versionName = packageInfo.versionName ?: "",
                    applicationInfo = applicationInfo,
                    minSdk = applicationInfo?.minSdkVersion,
                    targetSdk = applicationInfo?.targetSdkVersion,
                    signatureHash = signatureHash,
                    isSystemApp = ((applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isUninstalled = isUninstalled,
                    isArchived = packageManager.isPackageArchivedCompat(packageName),
                    packageSize = packageSize
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // This is an expected failure, no need to print stack trace in production.
                null
            }
        }
    }
}