// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.execution.authorization

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.framework.privileged.core.infrastructure.process.AppProcessTerminal

/**
 * Helper to generate the special auth command (e.g. "su 1000") for Root mode.
 * This ensures different methods reuse the same 'su 1000' service process.
 */
fun getSpecialAuth(
    authorizer: Authorizer,
    specialAuth: AppProcessTerminal = AppProcessTerminal.RootSystem
): (() -> AppProcessTerminal?)? =
    if (authorizer == Authorizer.Root) {
        { specialAuth }
    } else null
