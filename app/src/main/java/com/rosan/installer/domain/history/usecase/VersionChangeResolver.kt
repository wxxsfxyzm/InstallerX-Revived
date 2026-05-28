// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.history.usecase

import com.rosan.installer.domain.history.model.VersionChange

object VersionChangeResolver {
    fun resolve(oldVersionCode: Long?, newVersionCode: Long?): VersionChange = when {
        oldVersionCode == null && newVersionCode != null -> VersionChange.FRESH_INSTALL
        oldVersionCode == null || newVersionCode == null -> VersionChange.UNKNOWN
        newVersionCode > oldVersionCode -> VersionChange.UPDATE
        newVersionCode < oldVersionCode -> VersionChange.DOWNGRADE
        else -> VersionChange.SAME_VERSION
    }
}
