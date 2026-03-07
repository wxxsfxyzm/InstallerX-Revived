// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.InstallOption
import com.rosan.installer.domain.settings.model.Authorizer
import org.koin.compose.koinInject

@SuppressLint("LocalContextResourcesRead")
@Composable
fun rememberInstallOptions(authorizer: Authorizer): List<InstallOption> {
    val context = LocalContext.current
    val capabilityProvider: DeviceCapabilityProvider = koinInject()
    val isSystemApp = capabilityProvider.isSystemApp

    return remember(authorizer) {
        getInstallOptions(authorizer, isSystemApp).sortedBy { opt ->
            context.resources.getString(opt.labelResource)
        }
    }
}

private fun getInstallOptions(authorizer: Authorizer, isSystemApp: Boolean) = InstallOption.entries
    .filter {
        // First, check if the option is compatible with the current SDK version.
        val sdkVersionMatch = Build.VERSION.SDK_INT >= it.minSdk && Build.VERSION.SDK_INT <= it.maxSdk
        if (!sdkVersionMatch) {
            return@filter false
        }

        // Second, apply custom logic based on the authorizer.
        when (it) {
            // The AllowDowngrade option should only be available when the authorizer is Root, Shizuku(running as root).
            // Or running as SystemApp.
            InstallOption.AllowDowngrade -> authorizer == Authorizer.Root || authorizer == Authorizer.Shizuku || (authorizer == Authorizer.None && isSystemApp)
            // All other options are available by default.
            else -> true
        }
    }