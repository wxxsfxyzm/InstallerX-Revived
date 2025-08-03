package com.rosan.installer.data.app.util

import android.content.Context
import android.content.pm.ApplicationInfo // Import ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.rosan.installer.data.common.util.compatVersionCode
import com.rosan.installer.data.common.util.isPackageArchivedCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class InstalledAppInfo(
    val packageName: String,
    val icon: Drawable?,
    val label: String,
    val versionCode: Long,
    val versionName: String,
    val applicationInfo: ApplicationInfo?, // Add this field
    val minSdk: Int?,
    val targetSdk: Int?,
    val isArchived: Boolean = false
) {
    companion object : KoinComponent {
        fun buildByPackageName(packageName: String): InstalledAppInfo? {
            val context: Context = get()
            val packageManager = context.packageManager
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


                InstalledAppInfo(
                    packageName = packageName,
                    icon = applicationInfo?.loadIcon(packageManager),
                    label = applicationInfo?.loadLabel(packageManager)?.toString()
                        ?: packageName, // Fallback to package name
                    versionCode = packageInfo.compatVersionCode,
                    // --- FIX 2: AVOID CRASH ---
                    // Safely handle nullable versionName.
                    versionName = packageInfo.versionName ?: "",
                    applicationInfo = applicationInfo,
                    minSdk = applicationInfo?.minSdkVersion,
                    targetSdk = applicationInfo?.targetSdkVersion,
                    isArchived = packageManager.isPackageArchivedCompat(packageName)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // This is an expected failure, no need to print stack trace in production.
                null
            }
        }
    }
}