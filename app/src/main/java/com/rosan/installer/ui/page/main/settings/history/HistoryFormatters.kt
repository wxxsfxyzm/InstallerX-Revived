// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import androidx.annotation.StringRes
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import java.text.DateFormat
import java.util.Date

@StringRes
fun OperationType.labelRes(): Int = when (this) {
    OperationType.INSTALL -> R.string.history_operation_install
    OperationType.UNINSTALL -> R.string.history_operation_uninstall
    OperationType.SESSION_CONFIRM -> R.string.history_operation_session_confirm
}

@StringRes
fun OperationStatus.labelRes(): Int = when (this) {
    OperationStatus.SUCCESS -> R.string.history_status_success
    OperationStatus.FAILED -> R.string.history_status_failed
}

@StringRes
fun VersionChange.labelRes(): Int = when (this) {
    VersionChange.FRESH_INSTALL -> R.string.history_version_fresh_install
    VersionChange.UPDATE -> R.string.history_version_update
    VersionChange.DOWNGRADE -> R.string.history_version_downgrade
    VersionChange.SAME_VERSION -> R.string.history_version_same
    VersionChange.UNKNOWN -> R.string.history_unknown
}

@StringRes
fun InstallMethod.labelRes(): Int = when (this) {
    InstallMethod.PACKAGE_MANAGER -> R.string.history_method_package_manager
    InstallMethod.SESSION -> R.string.history_method_session
}

fun Long.formatHistoryTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
