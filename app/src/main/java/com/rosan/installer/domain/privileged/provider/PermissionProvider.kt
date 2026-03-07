// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import com.rosan.installer.domain.settings.model.Authorizer

/** 处理应用权限的特权提供者 */
interface PermissionProvider {
    suspend fun grantRuntimePermission(authorizer: Authorizer, packageName: String, permission: String)
    suspend fun isPermissionGranted(authorizer: Authorizer, packageName: String, permission: String): Boolean
}
