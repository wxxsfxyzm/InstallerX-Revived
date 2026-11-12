// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import com.rosan.installer.domain.engine.exception.UninstallException
import com.rosan.installer.domain.engine.model.error.UninstallErrorType

internal fun validateUninstallUser(currentUserId: Int, targetUserId: Int) {
    if (targetUserId < 0 || targetUserId != currentUserId) {
        throw UninstallException(
            UninstallErrorType.USER_RESTRICTED,
            "Uninstalling for another user requires cross-user privileges (target user: $targetUserId)",
        )
    }
}
