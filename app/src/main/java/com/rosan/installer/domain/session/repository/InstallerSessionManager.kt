package com.rosan.installer.domain.session.repository

import android.content.Intent

interface InstallerSessionManager {
    fun getOrCreate(id: String?): InstallerSessionRepository
    fun enqueueForegroundInstall(intent: Intent)
    fun takeNextForegroundInstall(): Intent?
    fun enqueueForegroundUninstall(intent: Intent)
    fun takeNextForegroundUninstall(): Intent?
}
