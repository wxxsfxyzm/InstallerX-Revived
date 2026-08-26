// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.device.provider

import android.content.Context
import com.rosan.installer.domain.device.provider.AppInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppInfoProviderImpl(private val context: Context) : AppInfoProvider {

    override suspend fun getAppLabel(packageName: String): String? = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Timber.d(e, "Failed to resolve app label for package: $packageName")
            null
        }
    }
}
