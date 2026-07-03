// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel

internal const val DEFAULT_PRIVILEGED_START_TIMEOUT_MS = 2500L

internal fun ConfigModel.shouldAttemptPrivilegedStart(isSystemApp: Boolean): Boolean =
    when (authorizer) {
        Authorizer.Root,
        Authorizer.Shizuku,
        Authorizer.Customize -> true

        Authorizer.None -> isSystemApp

        else -> false
    }
