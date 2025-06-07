package com.rosan.installer.data.common.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

val PackageInfo.compatVersionCode: Long
    get() = PackageInfoCompat.getLongVersionCode(this)

// Compat function to run below SDK 33
fun PackageManager.getCompatInstalledPackages(flags: Int): List<PackageInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // For Android 13 (API 33) and above
        getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        // For older versions
        getInstalledPackages(flags)
    }
}
