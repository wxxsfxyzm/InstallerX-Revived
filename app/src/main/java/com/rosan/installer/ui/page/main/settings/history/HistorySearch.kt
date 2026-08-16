// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.settings.model.config.Authorizer

enum class HistorySearchField {
    ALL,
    APP_LABEL,
    PACKAGE_NAME,
    OPERATION_TYPE,
    STATUS,
    TIME,
    VERSION_CHANGE,
    VERSION_NAME,
    VERSION_CODE,
    INITIATOR,
    INSTALLER_PACKAGE,
    APK_PATH,
    METHOD,
    AUTHORIZER,
    ERROR
}

@StringRes
fun HistorySearchField.labelRes(): Int = when (this) {
    HistorySearchField.ALL -> R.string.history_search_field_all
    HistorySearchField.APP_LABEL -> R.string.history_app_name
    HistorySearchField.PACKAGE_NAME -> R.string.history_package_name
    HistorySearchField.OPERATION_TYPE -> R.string.history_operation_type
    HistorySearchField.STATUS -> R.string.history_status
    HistorySearchField.TIME -> R.string.history_time
    HistorySearchField.VERSION_CHANGE -> R.string.history_version_change
    HistorySearchField.VERSION_NAME -> R.string.history_version_name
    HistorySearchField.VERSION_CODE -> R.string.history_version_code
    HistorySearchField.INITIATOR -> R.string.history_initiator
    HistorySearchField.INSTALLER_PACKAGE -> R.string.history_installer_package
    HistorySearchField.APK_PATH -> R.string.history_apk_path
    HistorySearchField.METHOD -> R.string.history_method
    HistorySearchField.AUTHORIZER -> R.string.history_authorizer
    HistorySearchField.ERROR -> R.string.history_error
}

data class HistorySearchTexts(
    val operationTypes: Map<OperationType, String> = emptyMap(),
    val statuses: Map<OperationStatus, String> = emptyMap(),
    val versionChanges: Map<VersionChange, String> = emptyMap(),
    val installMethods: Map<InstallMethod, String> = emptyMap(),
    val authorizers: Map<Authorizer, String> = emptyMap(),
    val formatTime: (Long) -> String = Long::toString
)

@Composable
fun rememberHistorySearchTexts(isSystemApp: Boolean): HistorySearchTexts {
    val configuration = LocalConfiguration.current
    val operationTypes = OperationType.entries.associateWith { stringResource(it.labelRes()) }
    val statuses = OperationStatus.entries.associateWith { stringResource(it.labelRes()) }
    val versionChanges = VersionChange.entries.associateWith { stringResource(it.labelRes()) }
    val installMethods = InstallMethod.entries.associateWith { stringResource(it.labelRes()) }
    val authorizers = Authorizer.entries.associateWith { authorizer ->
        if (authorizer == Authorizer.None && isSystemApp) {
            stringResource(R.string.working_status_system_installer)
        } else {
            stringResource(authorizer.displayNameRes)
        }
    }

    return remember(
        configuration.locales.toLanguageTags(),
        operationTypes,
        statuses,
        versionChanges,
        installMethods,
        authorizers
    ) {
        HistorySearchTexts(
            operationTypes = operationTypes,
            statuses = statuses,
            versionChanges = versionChanges,
            installMethods = installMethods,
            authorizers = authorizers,
            formatTime = { it.formatHistoryTime() }
        )
    }
}

fun filterHistoryRecords(
    records: List<OperationHistoryModel>,
    field: HistorySearchField,
    query: String,
    texts: HistorySearchTexts
): List<OperationHistoryModel> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return records

    return records.filter { record ->
        searchableValues(record, field, texts).any { value ->
            value.contains(normalizedQuery, ignoreCase = true)
        }
    }
}

private fun searchableValues(
    record: OperationHistoryModel,
    field: HistorySearchField,
    texts: HistorySearchTexts
): Sequence<String> = when (field) {
    HistorySearchField.ALL -> HistorySearchField.entries
        .asSequence()
        .filter { it != HistorySearchField.ALL }
        .flatMap { searchableValues(record, it, texts) }

    HistorySearchField.APP_LABEL -> valuesOf(record.appLabel)
    HistorySearchField.PACKAGE_NAME -> sequenceOf(record.packageName)
    HistorySearchField.OPERATION_TYPE -> valuesOf(
        texts.operationTypes[record.operationType],
        record.operationType.name
    )

    HistorySearchField.STATUS -> valuesOf(
        texts.statuses[record.status],
        record.status.name
    )

    HistorySearchField.TIME -> sequenceOf(
        texts.formatTime(record.timestamp),
        record.timestamp.toString()
    )

    HistorySearchField.VERSION_CHANGE -> valuesOf(
        texts.versionChanges[record.versionChange],
        record.versionChange.name
    )

    HistorySearchField.VERSION_NAME -> valuesOf(
        record.oldVersionName,
        record.newVersionName
    )

    HistorySearchField.VERSION_CODE -> valuesOf(
        record.oldVersionCode?.toString(),
        record.newVersionCode?.toString()
    )

    HistorySearchField.INITIATOR -> valuesOf(record.initiatorPackageName)
    HistorySearchField.INSTALLER_PACKAGE -> valuesOf(record.installerPackageName)
    HistorySearchField.APK_PATH -> record.sourcePaths.asSequence()

    HistorySearchField.METHOD -> valuesOf(
        texts.installMethods[record.installMethod],
        record.installMethod.name
    )

    HistorySearchField.AUTHORIZER -> valuesOf(
        texts.authorizers[record.authorizer],
        record.authorizer.value
    )

    HistorySearchField.ERROR -> valuesOf(record.errorType, record.errorSummary)
}

private fun valuesOf(vararg values: String?): Sequence<String> =
    values.asSequence().filterNotNull()
