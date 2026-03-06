// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.provider

import android.content.Context
import android.content.pm.ApplicationInfo
import com.rosan.installer.domain.settings.provider.SystemAppProvider
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewApp
import com.rosan.installer.util.hasFlag
import com.rosan.installer.util.pm.compatVersionCode
import com.rosan.installer.util.pm.getCompatInstalledPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemAppProviderImpl(private val context: Context) : SystemAppProvider {
    override suspend fun getInstalledApps(): List<ApplyViewApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        packageManager.getCompatInstalledPackages(0).map {
            ApplyViewApp(
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
