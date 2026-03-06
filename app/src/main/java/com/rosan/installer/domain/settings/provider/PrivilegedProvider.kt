// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.provider

import com.rosan.installer.domain.settings.model.Authorizer

interface PrivilegedProvider {
    suspend fun setAdbVerify(authorizer: Authorizer, customizeAuthorizer: String, enabled: Boolean)
    suspend fun setDefaultInstaller(authorizer: Authorizer, lock: Boolean)
}
