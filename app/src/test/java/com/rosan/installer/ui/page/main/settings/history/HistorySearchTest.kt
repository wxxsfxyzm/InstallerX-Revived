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
        unknown = "Unknown",
        none = "None",
        formatTime = { "2026-08-16 12:30" }
    )

    @Test
    fun `searchable fields match their visible values`() {
        val record = record()
        val failedRecord = record(
            id = 2L,
            status = OperationStatus.FAILED,
            errorType = "PackageInstallerException",
            errorSummary = "Install blocked"
        )
        val expectations = listOf(
            HistorySearchField.APP_LABEL to "Example App",
            HistorySearchField.PACKAGE_NAME to "com.example.app",
            HistorySearchField.OPERATION_TYPE to "Install",
            HistorySearchField.STATUS to "Success",
            HistorySearchField.TIME to "12:30",
            HistorySearchField.VERSION_CHANGE to "Update",
            HistorySearchField.VERSION_NAME to "1.0",
            HistorySearchField.VERSION_CODE to "2",
            HistorySearchField.INITIATOR to "com.example.source",
            HistorySearchField.INSTALLER_PACKAGE to "com.example.installer",
            HistorySearchField.APK_PATH to "/tmp/example.apk",
            HistorySearchField.METHOD to "Package manager",
            HistorySearchField.AUTHORIZER to "Root"
        )

        expectations.forEach { (field, query) ->
            assertEquals(listOf(record), filter(listOf(record), field, query))
        }
        assertEquals(
            listOf(failedRecord),
            filter(listOf(failedRecord), HistorySearchField.ERROR, "Install blocked")
        )
    }

    @Test
    fun `all scope matches values across searchable fields`() {
        val record = record()

        assertEquals(listOf(record), filter(listOf(record), HistorySearchField.ALL, "installer"))
        assertEquals(listOf(record), filter(listOf(record), HistorySearchField.ALL, "Update"))
        assertEquals(listOf(record), filter(listOf(record), HistorySearchField.ALL, "2026-08-16"))
    }

    @Test
    fun `package field does not match installer package`() {
        val record = record()

        assertTrue(
            filter(listOf(record), HistorySearchField.PACKAGE_NAME, "installer").isEmpty()
        )
        assertEquals(
            listOf(record),
            filter(listOf(record), HistorySearchField.INSTALLER_PACKAGE, "installer")
        )
    }

    @Test
    fun `app name field matches package fallback shown for records without labels`() {
        val record = record(
            packageName = "com.example.unresolved",
            appLabel = null
        )

        assertEquals(
            listOf(record),
            filter(listOf(record), HistorySearchField.APP_LABEL, "com.example.unresolved")
        )
    }

    @Test
    fun `blank query preserves record order`() {
        val first = record(id = 1L, packageName = "first.package")
        val second = record(id = 2L, packageName = "second.package")

        assertEquals(
            listOf(first, second),
            filter(listOf(first, second), HistorySearchField.PACKAGE_NAME, "  ")
        )
    }

    @Test
    fun `raw enum values remain searchable`() {
        val record = record()

        assertEquals(
            listOf(record),
            filter(listOf(record), HistorySearchField.OPERATION_TYPE, "INSTALL")
        )
        assertEquals(
            listOf(record),
            filter(listOf(record), HistorySearchField.AUTHORIZER, "root")
        )
    }

    @Test
    fun `visible unknown and none placeholders are searchable`() {
        val incompleteRecord = record(
            oldVersionName = null,
            oldVersionCode = null,
            newVersionName = "",
            newVersionCode = null,
            sourcePaths = emptyList(),
            initiatorPackageName = null,
            installerPackageName = null
        )
        val failedRecord = record(
            id = 2L,
            status = OperationStatus.FAILED,
            errorType = null,
            errorSummary = ""
        )

        assertEquals(
            listOf(incompleteRecord),
            filter(listOf(incompleteRecord), HistorySearchField.VERSION_NAME, "None")
        )
        assertEquals(
            listOf(incompleteRecord),
            filter(listOf(incompleteRecord), HistorySearchField.VERSION_CODE, "None")
        )
        assertEquals(
            listOf(incompleteRecord),
            filter(listOf(incompleteRecord), HistorySearchField.APK_PATH, "None")
        )
        assertEquals(
            listOf(incompleteRecord),
            filter(listOf(incompleteRecord), HistorySearchField.INITIATOR, "Unknown")
        )
        assertEquals(
            listOf(incompleteRecord),
            filter(listOf(incompleteRecord), HistorySearchField.INSTALLER_PACKAGE, "Unknown")
        )
        assertEquals(
            listOf(failedRecord),
            filter(listOf(failedRecord), HistorySearchField.ERROR, "Unknown")
        )
    }

    @Test
    fun `session records do not match fields hidden in their details`() {
        val sessionRecord = record(
            operationType = OperationType.SESSION_CONFIRM,
            installMethod = InstallMethod.SESSION,
            versionChange = VersionChange.UNKNOWN,
            oldVersionName = null,
            oldVersionCode = null,
            newVersionName = null,
            newVersionCode = null,
            sourcePaths = emptyList()
        )

        assertTrue(filter(listOf(sessionRecord), HistorySearchField.VERSION_CHANGE, "Unknown").isEmpty())
        assertTrue(filter(listOf(sessionRecord), HistorySearchField.VERSION_NAME, "None").isEmpty())
        assertTrue(filter(listOf(sessionRecord), HistorySearchField.VERSION_CODE, "None").isEmpty())
        assertTrue(filter(listOf(sessionRecord), HistorySearchField.APK_PATH, "None").isEmpty())
        assertTrue(filter(listOf(sessionRecord), HistorySearchField.ALL, "Unknown").isEmpty())
        assertFalse(sessionRecord.hasPackageManagerDetails())
        assertTrue(record().hasPackageManagerDetails())
    }

    @Test
    fun `search result distinguishes no history from no matches`() {
        val records = listOf(record())

        assertEquals(
            HistorySearchResult.Empty(HistoryEmptyState.NO_HISTORY),
            resolveHistorySearchResult(emptyList(), criteria("missing"), texts)
        )
        assertEquals(
            HistorySearchResult.Empty(HistoryEmptyState.NO_MATCHES),
            resolveHistorySearchResult(records, criteria("missing"), texts)
        )
        assertEquals(
            HistorySearchResult.Records(records),
            resolveHistorySearchResult(records, criteria("example"), texts)
        )
    }

    @Test
    fun `criteria is active for query or targeted field`() {
        assertTrue(criteria("query").isActive)
        assertTrue(HistorySearchCriteria(field = HistorySearchField.PACKAGE_NAME).isActive)
        assertFalse(HistorySearchCriteria().isActive)
    }

    private fun filter(
        records: List<OperationHistoryModel>,
        field: HistorySearchField,
        query: String
    ) = filterHistoryRecords(records, HistorySearchCriteria(query, field), texts)

    private fun criteria(query: String) = HistorySearchCriteria(query = query)

    private fun record(
        id: Long = 1L,
        operationType: OperationType = OperationType.INSTALL,
        packageName: String = "com.example.app",
        appLabel: String? = "Example App",
        installerPackageName: String? = "com.example.installer",
        status: OperationStatus = OperationStatus.SUCCESS,
        versionChange: VersionChange = VersionChange.UPDATE,
        oldVersionName: String? = "1.0",
        oldVersionCode: Long? = 1L,
        newVersionName: String? = "2.0",
        newVersionCode: Long? = 2L,
        sourcePaths: List<String> = listOf("/tmp/example.apk"),
        initiatorPackageName: String? = "com.example.source",
        installMethod: InstallMethod = InstallMethod.PACKAGE_MANAGER,
        errorSummary: String? = null,
        errorType: String? = null
    ) = OperationHistoryModel(
        id = id,
        operationType = operationType,
        status = status,
        packageName = packageName,
        appLabel = appLabel,
        timestamp = 1_755_330_600_000L,
        versionChange = versionChange,
        oldVersionName = oldVersionName,
        oldVersionCode = oldVersionCode,
        newVersionName = newVersionName,
        newVersionCode = newVersionCode,
        sourcePaths = sourcePaths,
        initiatorPackageName = initiatorPackageName,
        installerPackageName = installerPackageName,
        installMethod = installMethod,
        authorizer = Authorizer.Root,
        installMode = InstallMode.Dialog,
        errorSummary = errorSummary,
        errorType = errorType
    )
}
