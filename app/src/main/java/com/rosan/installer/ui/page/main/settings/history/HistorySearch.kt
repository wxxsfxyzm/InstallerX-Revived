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

/** User-visible record fields that can be targeted by history search. */
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

data class HistorySearchCriteria(
    val query: String = "",
    val field: HistorySearchField = HistorySearchField.ALL
) {
    val isActive: Boolean
        get() = query.isNotBlank() || this.field != HistorySearchField.ALL
}

enum class HistoryEmptyState {
    NO_HISTORY,
    NO_MATCHES
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

@StringRes
fun HistoryEmptyState.titleRes(): Int = when (this) {
    HistoryEmptyState.NO_HISTORY -> R.string.history_empty_title
    HistoryEmptyState.NO_MATCHES -> R.string.history_search_empty_title
}

@StringRes
fun HistoryEmptyState.descriptionRes(): Int = when (this) {
    HistoryEmptyState.NO_HISTORY -> R.string.history_empty_desc
    HistoryEmptyState.NO_MATCHES -> R.string.history_search_empty_desc
}

data class HistorySearchTexts(
    val operationTypes: Map<OperationType, String> = emptyMap(),
    val statuses: Map<OperationStatus, String> = emptyMap(),
    val versionChanges: Map<VersionChange, String> = emptyMap(),
    val installMethods: Map<InstallMethod, String> = emptyMap(),
    val authorizers: Map<Authorizer, String> = emptyMap(),
    val unknown: String = "",
    val none: String = "",
    val formatTime: (Long) -> String = Long::toString
)

data class HistorySearchResult(
    val records: List<OperationHistoryModel>,
    val emptyState: HistoryEmptyState?
)

@Composable
fun rememberHistorySearchTexts(isSystemApp: Boolean): HistorySearchTexts {
    val configuration = LocalConfiguration.current
    val operationTypes = OperationType.entries.associateWith { stringResource(it.labelRes()) }
    val statuses = OperationStatus.entries.associateWith { stringResource(it.labelRes()) }
    val versionChanges = VersionChange.entries.associateWith { stringResource(it.labelRes()) }
    val installMethods = InstallMethod.entries.associateWith { stringResource(it.labelRes()) }
    val unknown = stringResource(R.string.history_unknown)
    val none = stringResource(R.string.history_none)
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
        authorizers,
        unknown,
        none
    ) {
        HistorySearchTexts(
            operationTypes = operationTypes,
            statuses = statuses,
            versionChanges = versionChanges,
            installMethods = installMethods,
            authorizers = authorizers,
            unknown = unknown,
            none = none,
            formatTime = { it.formatHistoryTime() }
        )
    }
}

fun filterHistoryRecords(
    records: List<OperationHistoryModel>,
    criteria: HistorySearchCriteria,
    texts: HistorySearchTexts
): List<OperationHistoryModel> {
    val normalizedQuery = criteria.query.trim()
    if (normalizedQuery.isEmpty()) return records

    return records.filter { record ->
        searchableValues(record, criteria.field, texts).any { value ->
            value.contains(normalizedQuery, ignoreCase = true)
        }
    }
}

fun resolveHistorySearchResult(
    records: List<OperationHistoryModel>,
    criteria: HistorySearchCriteria,
    texts: HistorySearchTexts
): HistorySearchResult {
    val visibleRecords = filterHistoryRecords(records, criteria, texts)
    val emptyState = when {
        visibleRecords.isNotEmpty() -> null
        records.isEmpty() -> HistoryEmptyState.NO_HISTORY
        else -> HistoryEmptyState.NO_MATCHES
    }

    return HistorySearchResult(records = visibleRecords, emptyState = emptyState)
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

    HistorySearchField.VERSION_CHANGE -> record.nonSessionValues {
        valuesOf(
            texts.versionChanges[record.versionChange],
            record.versionChange.name
        )
    }

    HistorySearchField.VERSION_NAME -> record.nonSessionValues {
        valuesWithFallback(texts.none, record.oldVersionName, record.newVersionName)
    }

    HistorySearchField.VERSION_CODE -> record.nonSessionValues {
        valuesWithFallback(texts.none, record.oldVersionCode?.toString(), record.newVersionCode?.toString())
    }

    HistorySearchField.INITIATOR -> valueOrFallback(record.initiatorPackageName, texts.unknown)
    HistorySearchField.INSTALLER_PACKAGE -> valueOrFallback(record.installerPackageName, texts.unknown)
    HistorySearchField.APK_PATH -> record.nonSessionValues {
        if (record.sourcePaths.isEmpty()) {
            sequenceOf(texts.none)
        } else {
            record.sourcePaths.asSequence()
        }
    }

    HistorySearchField.METHOD -> valuesOf(
        texts.installMethods[record.installMethod],
        record.installMethod.name
    )

    HistorySearchField.AUTHORIZER -> valuesOf(
        texts.authorizers[record.authorizer],
        record.authorizer.value
    )

    HistorySearchField.ERROR -> if (record.status == OperationStatus.FAILED) {
        valuesOrFallback(texts.unknown, record.errorType, record.errorSummary)
    } else {
        emptySequence()
    }
}

private fun valuesOf(vararg values: String?): Sequence<String> =
    values.asSequence().filterNotNull()

private fun valuesWithFallback(fallback: String, vararg values: String?): Sequence<String> =
    values.asSequence()
        .map { it?.takeIf(String::isNotBlank) ?: fallback }
        .distinct()

private fun valueOrFallback(value: String?, fallback: String): Sequence<String> =
    sequenceOf(value ?: fallback)

private fun valuesOrFallback(fallback: String, vararg values: String?): Sequence<String> {
    val nonBlankValues = values.filterNotNull().filter { it.isNotBlank() }
    return if (nonBlankValues.isEmpty()) sequenceOf(fallback) else nonBlankValues.asSequence()
}

private inline fun OperationHistoryModel.nonSessionValues(
    values: () -> Sequence<String>
): Sequence<String> = if (installMethod == InstallMethod.SESSION) emptySequence() else values()
