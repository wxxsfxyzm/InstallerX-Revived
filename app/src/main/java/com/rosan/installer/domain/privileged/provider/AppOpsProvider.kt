// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import android.content.ComponentName
import com.rosan.installer.domain.settings.model.Authorizer

/** 处理系统级应用操作（如默认安装器、ADB 验证、网络控制）的特权提供者 */
interface AppOpsProvider {
    suspend fun setDefaultInstaller(authorizer: Authorizer, component: ComponentName, lock: Boolean)
    suspend fun setAdbVerifyEnabled(authorizer: Authorizer, customizeAuthorizer: String, enabled: Boolean)
    suspend fun setPackageNetworkingEnabled(authorizer: Authorizer, uid: Int, enabled: Boolean)
}