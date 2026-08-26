// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.model

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode

data class PendingSelfUpdateHistory(
    val packageName: String,
    val appLabel: String?,
    val oldVersionName: String?,
    val oldVersionCode: Long?,
    val newVersionName: String?,
    val newVersionCode: Long?,
    val sourcePaths: List<String>,
    val initiatorPackageName: String?,
    val authorizer: Authorizer,
    val installMode: InstallMode,
    val operationSessionKey: String,
)
