// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.model

import com.rosan.installer.domain.settings.model.config.Authorizer

data class PendingSourceDeletion(
    val paths: List<String>,
    val authorizer: Authorizer,
    val customizeAuthorizer: String
)
