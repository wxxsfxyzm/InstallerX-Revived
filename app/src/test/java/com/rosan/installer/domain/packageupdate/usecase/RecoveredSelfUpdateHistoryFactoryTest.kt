// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.usecase

import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdateHistory
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode
import kotlin.test.Test
import kotlin.test.assertEquals

class RecoveredSelfUpdateHistoryFactoryTest {
    @Test
    fun `recovered update uses verified package version and keeps session identity`() {
        val pending = PendingSelfUpdateHistory(
            packageName = "com.rosan.installer",
            appLabel = "InstallerX",
            oldVersionName = "1.0",
            oldVersionCode = 10L,
            newVersionName = "1.1 candidate",
            newVersionCode = 11L,
            sourcePaths = listOf("/storage/emulated/0/Download/installer.apk"),
            initiatorPackageName = "com.android.documentsui",
            authorizer = Authorizer.Shizuku,
            installMode = InstallMode.Dialog,
            operationSessionKey = "session-42",
        )

        val history = pending.toSuccessfulOperationHistory(
            actualNewVersionName = "1.1",
            actualNewVersionCode = 12L,
            installerPackageName = "com.rosan.installer",
            timestamp = 1234L,
        )

        assertEquals(OperationType.INSTALL, history.operationType)
        assertEquals(OperationStatus.SUCCESS, history.status)
        assertEquals(false, history.isFreshInstall)
        assertEquals(VersionChange.UPDATE, history.versionChange)
        assertEquals("1.1", history.newVersionName)
        assertEquals(12L, history.newVersionCode)
        assertEquals(InstallMethod.PACKAGE_MANAGER, history.installMethod)
        assertEquals("session-42", history.operationSessionKey)
        assertEquals(1234L, history.timestamp)
    }
}
