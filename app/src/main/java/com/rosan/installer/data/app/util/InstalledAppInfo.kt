package com.rosan.installer.data.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.rosan.installer.data.common.util.compatVersionCode
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class InstalledAppInfo(
    val packageName: String,
    val icon: Drawable?,
    val label: String,
    val versionCode: Long,
    val versionName: String
) {
    companion object : KoinComponent {
        fun buildByPackageName(packageName: String): InstalledAppInfo? {
            val context: Context = get()
            val packageManager = context.packageManager
            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager?.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                else
                    packageManager?.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null
            } ?: return null
            val applicationInfo = packageInfo.applicationInfo
            return InstalledAppInfo(
                packageName = packageName,
                icon = applicationInfo?.loadIcon(packageManager),
                label = applicationInfo?.loadLabel(packageManager)?.toString() ?: "",
                versionCode = packageInfo.compatVersionCode,
                versionName = packageInfo.versionName!!
            )
        }
    }
}