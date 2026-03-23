// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.repository.AppIconRepository

/**
 * UseCase for clearing icon caches.
 * It can clear icons by session ID or by package name across all sessions.
 */
class ClearAppIconCacheUseCase(
    private val appIconRepo: AppIconRepository
) {
    /**
     * Clears the icon cache based on the provided parameters.
     * @param sessionId If provided, clears all icons associated with this session.
     * @param packageName If provided, clears all icons for this package across all sessions.
     */
    suspend operator fun invoke(sessionId: String? = null, packageName: String? = null) {
        sessionId?.let { appIconRepo.clearCacheForSession(it) }
        packageName?.let { appIconRepo.clearCacheForPackage(it) }
    }
}
