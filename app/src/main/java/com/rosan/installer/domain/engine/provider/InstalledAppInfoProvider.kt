// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.provider

import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo

interface InstalledAppInfoProvider {
    fun getByPackageName(packageName: String, includeSignature: Boolean = true): InstalledAppInfo?
}
