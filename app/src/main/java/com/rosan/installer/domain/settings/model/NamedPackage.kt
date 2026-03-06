// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model

import kotlinx.serialization.Serializable

@Serializable
data class NamedPackage(
    val name: String,
    val packageName: String
)