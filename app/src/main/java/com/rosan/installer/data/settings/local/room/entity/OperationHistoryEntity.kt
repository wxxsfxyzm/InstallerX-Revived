// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.local.room.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "operation_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["package_name"])
    ]
)
data class OperationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,
    @ColumnInfo(name = "operation_type") var operationType: String,
    @ColumnInfo(name = "status") var status: String,
    @ColumnInfo(name = "package_name") var packageName: String,
    @ColumnInfo(name = "app_label") var appLabel: String? = null,
    @ColumnInfo(name = "timestamp") var timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_fresh_install") var isFreshInstall: Boolean? = null,
    @ColumnInfo(name = "version_change") var versionChange: String,
    @ColumnInfo(name = "old_version_name") var oldVersionName: String? = null,
    @ColumnInfo(name = "old_version_code") var oldVersionCode: Long? = null,
    @ColumnInfo(name = "new_version_name") var newVersionName: String? = null,
    @ColumnInfo(name = "new_version_code") var newVersionCode: Long? = null,
    @ColumnInfo(name = "source_paths") var sourcePaths: List<String> = emptyList(),
    @ColumnInfo(name = "initiator_package_name") var initiatorPackageName: String? = null,
    @ColumnInfo(name = "installer_package_name") var installerPackageName: String? = null,
    @ColumnInfo(name = "install_method") var installMethod: String,
    @ColumnInfo(name = "authorizer") var authorizer: String,
    @ColumnInfo(name = "install_mode") var installMode: String,
    @ColumnInfo(name = "error_summary") var errorSummary: String? = null,
    @ColumnInfo(name = "error_type") var errorType: String? = null
)
