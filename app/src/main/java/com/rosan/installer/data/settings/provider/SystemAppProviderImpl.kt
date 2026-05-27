// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import com.rosan.installer.domain.settings.model.app.InstalledAppTarget
import com.rosan.installer.domain.settings.provider.SystemAppProvider
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.util.pm.compatVersionCode
import com.rosan.installer.util.pm.getCompatInstalledPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemAppProviderImpl(private val context: Context) : SystemAppProvider {
    override suspend fun getInstalledApps(): List<InstalledAppTarget> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        packageManager.getCompatInstalledPackages(0).map {
            InstalledAppTarget(
                packageName = it.packageName,
                versionName = it.versionName,
                versionCode = it.compatVersionCode,
                firstInstallTime = it.firstInstallTime,
                lastUpdateTime = it.lastUpdateTime,
                isSystemApp = it.applicationInfo!!.flags.hasFlag(ApplicationInfo.FLAG_SYSTEM),
                label = it.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
            )
        }
    }
}
