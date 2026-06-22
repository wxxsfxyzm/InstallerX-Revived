// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.provider

import com.rosan.installer.domain.engine.model.packageinfo.InstalledModuleInfo
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.preferences.RootMode

interface InstalledModuleInfoProvider {
    suspend fun list(config: ConfigModel, rootMode: RootMode): List<InstalledModuleInfo>
}
