package com.rosan.installer.data.common.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

val PackageInfo.compatVersionCode: Long
    get() = PackageInfoCompat.getLongVersionCode(this)

fun PackageManager.getCompatInstalledPackages(flags: Int): List<PackageInfo> =
    getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
