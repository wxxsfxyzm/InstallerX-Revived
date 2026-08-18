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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistorySearchTest {
    private val texts = HistorySearchTexts(
        operationTypes = mapOf(OperationType.INSTALL to "Install"),
        statuses = mapOf(OperationStatus.SUCCESS to "Success"),
        versionChanges = mapOf(VersionChange.UPDATE to "Update"),
        installMethods = mapOf(InstallMethod.PACKAGE_MANAGER to "Package manager"),
        authorizers = mapOf(Authorizer.Root to "Root"),
        formatTime = { "2026-08-16 12:30" }
    )

    @Test
    fun `all fields match every searchable record value`() {
        val record = record()

        assertEquals(listOf(record), filterHistoryRecords(listOf(record), HistorySearchField.ALL, "installer", texts))
        assertEquals(listOf(record), filterHistoryRecords(listOf(record), HistorySearchField.ALL, "Update", texts))
        assertEquals(listOf(record), filterHistoryRecords(listOf(record), HistorySearchField.ALL, "2026-08-16", texts))
    }

    @Test
    fun `package field does not match installer package`() {
        val record = record()

        assertTrue(
            filterHistoryRecords(
                listOf(record),
                HistorySearchField.PACKAGE_NAME,
                "installer",
                texts
            ).isEmpty()
        )
        assertEquals(
            listOf(record),
            filterHistoryRecords(listOf(record), HistorySearchField.INSTALLER_PACKAGE, "installer", texts)
        )
    }

    @Test
    fun `blank query preserves record order`() {
        val first = record(id = 1L, packageName = "first.package")
        val second = record(id = 2L, packageName = "second.package")

        assertEquals(
            listOf(first, second),
            filterHistoryRecords(listOf(first, second), HistorySearchField.PACKAGE_NAME, "  ", texts)
        )
    }

    @Test
    fun `localized enum and time display values are searchable`() {
        val record = record()

        assertEquals(
            listOf(record),
            filterHistoryRecords(listOf(record), HistorySearchField.OPERATION_TYPE, "Install", texts)
        )
        assertEquals(
            listOf(record),
            filterHistoryRecords(listOf(record), HistorySearchField.AUTHORIZER, "Root", texts)
        )
        assertEquals(
            listOf(record),
            filterHistoryRecords(listOf(record), HistorySearchField.TIME, "12:30", texts)
        )
    }

    @Test
    fun `search empty state only applies when a nonblank query has no matches`() {
        val records = listOf(record())

        assertTrue(hasNoHistorySearchResults(records, "missing", emptyList()))
        assertFalse(hasNoHistorySearchResults(emptyList(), "missing", emptyList()))
        assertFalse(hasNoHistorySearchResults(records, "  ", emptyList()))
        assertFalse(hasNoHistorySearchResults(records, "example", records))
    }

    private fun record(
        id: Long = 1L,
        packageName: String = "com.example.app",
        installerPackageName: String? = "com.example.installer"
    ) = OperationHistoryModel(
        id = id,
        operationType = OperationType.INSTALL,
        status = OperationStatus.SUCCESS,
        packageName = packageName,
        appLabel = "Example App",
        timestamp = 1_755_330_600_000L,
        versionChange = VersionChange.UPDATE,
        oldVersionName = "1.0",
        oldVersionCode = 1L,
        newVersionName = "2.0",
        newVersionCode = 2L,
        sourcePaths = listOf("/tmp/example.apk"),
        initiatorPackageName = "com.example.source",
        installerPackageName = installerPackageName,
        installMethod = InstallMethod.PACKAGE_MANAGER,
        authorizer = Authorizer.Root,
        installMode = InstallMode.Dialog
    )
}
