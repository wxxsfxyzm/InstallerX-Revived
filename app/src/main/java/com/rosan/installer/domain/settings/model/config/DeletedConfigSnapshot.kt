// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.config

import com.rosan.installer.domain.settings.model.app.AppModel

data class DeletedConfigSnapshot(
    val configModel: ConfigModel,
    val scopes: List<AppModel>
)
