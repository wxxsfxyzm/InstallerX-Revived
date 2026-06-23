// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import android.content.ComponentName
import com.rosan.installer.domain.settings.model.config.Authorizer

/** Privileged provider that handles system-level application operations (e.g., default installer, ADB verification, network control) */
interface AppOpsProvider {
    suspend fun setDefaultInstaller(authorizer: Authorizer, component: ComponentName, lock: Boolean)
    suspend fun setAdbVerifyEnabled(authorizer: Authorizer, customizeAuthorizer: String, enabled: Boolean)
    suspend fun setPackageNetworkingEnabled(authorizer: Authorizer, uid: Int, enabled: Boolean)
    suspend fun prepareUnknownSourceAppOp(authorizer: Authorizer, customizeAuthorizer: String, uid: Int, packageName: String): Int?
}
