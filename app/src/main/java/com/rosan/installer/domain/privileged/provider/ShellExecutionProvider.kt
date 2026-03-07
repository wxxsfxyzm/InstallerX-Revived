// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.provider

import com.rosan.installer.domain.settings.model.ConfigModel

interface ShellExecutionProvider {
    suspend fun executeCommandArray(config: ConfigModel, command: Array<String>): String
}
