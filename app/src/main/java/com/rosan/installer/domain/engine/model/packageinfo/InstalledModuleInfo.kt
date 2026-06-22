// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

data class InstalledModuleInfo(
    val id: String,
    val name: String? = null,
    val version: String? = null,
    val versionCode: Long? = null,
    val author: String? = null,
    val description: String? = null,
    val enabled: Boolean? = null,
    val remove: Boolean? = null,
    val update: Boolean? = null,
    val web: Boolean? = null,
    val action: Boolean? = null,
    val mount: Boolean? = null,
    val updateJson: String? = null
)
