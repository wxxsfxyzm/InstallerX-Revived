// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import com.rosan.installer.domain.engine.model.packageinfo.AppEntity

data class SelectInstallEntity(
    val app: AppEntity,
    val selected: Boolean
)
