// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.provider

import android.content.ComponentName
import android.content.Context
import com.rosan.installer.domain.privileged.provider.AppOpsProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.provider.PrivilegedProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivilegedProviderImpl(
    private val context: Context,
    private val appOpsProvider: AppOpsProvider
) : PrivilegedProvider {
    override suspend fun setAdbVerify(authorizer: Authorizer, customizeAuthorizer: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            appOpsProvider.setAdbVerifyEnabled(authorizer, customizeAuthorizer, enabled)
        }
    }

    override suspend fun setDefaultInstaller(authorizer: Authorizer, lock: Boolean) {
        withContext(Dispatchers.IO) {
            val component = ComponentName(context, "com.rosan.installer.ui.activity.InstallerActivity")
            appOpsProvider.setDefaultInstaller(authorizer, component, lock)
        }
    }
}
