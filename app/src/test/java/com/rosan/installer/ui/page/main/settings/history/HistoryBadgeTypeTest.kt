// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryBadgeTypeTest {
    @Test
    fun `own package install is labeled self update before version change`() {
        val history = history(
            packageName = "com.rosan.installer",
            operationType = OperationType.INSTALL,
            versionChange = VersionChange.DOWNGRADE,
        )

        assertEquals(
            HistoryBadgeType.SELF_UPDATE,
            history.historyBadgeType(selfPackageName = "com.rosan.installer"),
        )
    }

    @Test
    fun `other package update and downgrade use version change badge`() {
        assertEquals(
            HistoryBadgeType.FRESH_INSTALL,
            history(versionChange = VersionChange.FRESH_INSTALL).historyBadgeType("com.rosan.installer"),
        )
        assertEquals(
            HistoryBadgeType.UPDATE,
            history(versionChange = VersionChange.UPDATE).historyBadgeType("com.rosan.installer"),
        )
        assertEquals(
            HistoryBadgeType.DOWNGRADE,
            history(versionChange = VersionChange.DOWNGRADE).historyBadgeType("com.rosan.installer"),
        )
    }

    @Test
    fun `non install operation has no version badge`() {
        assertNull(
            history(
                packageName = "com.rosan.installer",
                operationType = OperationType.UNINSTALL,
                versionChange = VersionChange.DOWNGRADE,
            ).historyBadgeType("com.rosan.installer"),
        )
    }

    private fun history(
        packageName: String = "com.example.app",
        operationType: OperationType = OperationType.INSTALL,
        versionChange: VersionChange,
    ) = OperationHistoryModel(
        operationType = operationType,
        status = OperationStatus.SUCCESS,
        packageName = packageName,
        versionChange = versionChange,
        installMethod = InstallMethod.PACKAGE_MANAGER,
        authorizer = Authorizer.Root,
        installMode = InstallMode.Dialog,
    )
}
