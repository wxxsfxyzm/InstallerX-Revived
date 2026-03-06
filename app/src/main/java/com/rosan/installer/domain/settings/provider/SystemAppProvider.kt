// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.provider

import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewApp // 建议把这个实体移到 domain 层

interface SystemAppProvider {
    suspend fun getInstalledApps(): List<ApplyViewApp>
}
