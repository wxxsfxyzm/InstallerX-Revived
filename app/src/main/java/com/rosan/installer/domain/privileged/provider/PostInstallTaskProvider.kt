// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import com.rosan.installer.domain.privileged.model.PostInstallTaskInfo
import com.rosan.installer.domain.settings.model.Authorizer

interface PostInstallTaskProvider {
    suspend fun executeTasks(authorizer: Authorizer, customizeAuthorizer: String, info: PostInstallTaskInfo)
}
