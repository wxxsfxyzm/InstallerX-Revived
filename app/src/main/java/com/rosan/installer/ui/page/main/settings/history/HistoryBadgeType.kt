// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import androidx.annotation.StringRes
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange

internal enum class HistoryBadgeType {
    FRESH_INSTALL,
    UPDATE,
    DOWNGRADE,
    SELF_UPDATE,
}

@StringRes
internal fun HistoryBadgeType.labelRes(): Int = when (this) {
    HistoryBadgeType.FRESH_INSTALL -> R.string.history_version_fresh_install
    HistoryBadgeType.UPDATE -> R.string.history_version_update
    HistoryBadgeType.DOWNGRADE -> R.string.history_version_downgrade
    HistoryBadgeType.SELF_UPDATE -> R.string.history_version_self_update
}

internal fun OperationHistoryModel.historyBadgeType(selfPackageName: String): HistoryBadgeType? = when {
    operationType != OperationType.INSTALL -> null
    packageName == selfPackageName -> HistoryBadgeType.SELF_UPDATE
    versionChange == VersionChange.FRESH_INSTALL -> HistoryBadgeType.FRESH_INSTALL
    versionChange == VersionChange.UPDATE -> HistoryBadgeType.UPDATE
    versionChange == VersionChange.DOWNGRADE -> HistoryBadgeType.DOWNGRADE
    else -> null
}
