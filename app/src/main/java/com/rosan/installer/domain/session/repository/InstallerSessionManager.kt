// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.repository

import android.content.Intent

interface InstallerSessionManager {
    fun getOrCreate(id: String?): InstallerSessionRepository
    fun enqueueForegroundInstall(intent: Intent)
    fun takeNextForegroundInstall(): Intent?
    fun clearForegroundInstallQueue()
    fun enqueueForegroundConfirmation(intent: Intent, callerUid: Int)
    fun takeNextForegroundConfirmation(): Pair<Intent, Int>?
    fun enqueueForegroundUninstall(intent: Intent)
    fun takeNextForegroundUninstall(): Intent?
}
