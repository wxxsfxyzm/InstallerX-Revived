// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.core.env

import com.rosan.installer.BuildConfig
import com.rosan.installer.domain.device.model.Level

object AppConfig {
    val LEVEL: Level = when (BuildConfig.BUILD_LEVEL) {
        1 -> Level.PREVIEW
        2 -> Level.STABLE
        else -> Level.UNSTABLE
    }

    val isDebug: Boolean = BuildConfig.DEBUG
    const val isInternetAccessEnabled: Boolean = BuildConfig.INTERNET_ACCESS_ENABLED
    const val VERSION_NAME: String = BuildConfig.VERSION_NAME
    const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    val isLogEnabled = LEVEL == Level.PREVIEW || LEVEL == Level.UNSTABLE || isDebug
}
