// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.util

import com.rosan.installer.domain.settings.model.config.Authorizer

/**
 * Returns true if the Dhizuku authorizer is active, which disables certain features.
 */
fun isDhizukuActive(
    stateAuthorizer: Authorizer,
    globalAuthorizer: Authorizer
) = when (stateAuthorizer) {
    Authorizer.Dhizuku -> true
    Authorizer.Global -> globalAuthorizer == Authorizer.Dhizuku
    else -> false
}

/**
 * Returns true if the None authorizer is active.
 */
fun isNoneActive(
    stateAuthorizer: Authorizer,
    globalAuthorizer: Authorizer
) = when (stateAuthorizer) {
    Authorizer.None -> true
    Authorizer.Global -> globalAuthorizer == Authorizer.None
    else -> false
}

/**
 * Returns true if the effective None authorizer is backed by system package installer privileges.
 */
fun isSystemPackageInstallerActive(
    stateAuthorizer: Authorizer,
    globalAuthorizer: Authorizer,
    isSystemApp: Boolean
) = isSystemApp && isNoneActive(stateAuthorizer, globalAuthorizer)
