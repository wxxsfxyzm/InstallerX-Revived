// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.config

import androidx.annotation.StringRes
import com.rosan.installer.R

/**
 * Define Authorizers used by InstallerX
 */
enum class Authorizer(
    val value: String,
    @field:StringRes val displayNameRes: Int,
) {
    Global("global", R.string.config_authorizer_global),
    None("none", R.string.config_authorizer_none),
    Root("root", R.string.config_authorizer_root),
    Shizuku("shizuku", R.string.config_authorizer_shizuku),
    Dhizuku("dhizuku", R.string.config_authorizer_dhizuku),
    Customize("customize", R.string.config_authorizer_customize);

    companion object {
        fun fromValueOrDefault(value: String) =
            entries.find { it.value == value } ?: Global
    }
}
