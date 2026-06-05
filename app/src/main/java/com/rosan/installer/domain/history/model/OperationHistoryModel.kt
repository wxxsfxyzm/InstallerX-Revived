// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.history.model

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode

data class OperationHistoryModel(
    val id: Long = 0L,
    val operationType: OperationType,
    val status: OperationStatus,
    val packageName: String,
    val appLabel: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isFreshInstall: Boolean? = null,
    val versionChange: VersionChange = VersionChange.UNKNOWN,
    val oldVersionName: String? = null,
    val oldVersionCode: Long? = null,
    val newVersionName: String? = null,
    val newVersionCode: Long? = null,
    val sourcePaths: List<String> = emptyList(),
    val initiatorPackageName: String? = null,
    val installerPackageName: String? = null,
    val installMethod: InstallMethod,
    val authorizer: Authorizer,
    val installMode: InstallMode,
    val errorSummary: String? = null,
    val errorType: String? = null
)

enum class OperationType {
    INSTALL,
    UNINSTALL,
    SESSION_CONFIRM
}

enum class OperationStatus {
    SUCCESS,
    FAILED
}

enum class VersionChange {
    FRESH_INSTALL,
    UPDATE,
    DOWNGRADE,
    SAME_VERSION,
    UNKNOWN
}

enum class InstallMethod {
    PACKAGE_MANAGER,
    SESSION
}
