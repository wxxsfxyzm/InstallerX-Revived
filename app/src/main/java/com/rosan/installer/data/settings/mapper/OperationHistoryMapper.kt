// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.mapper

import com.rosan.installer.data.settings.local.room.entity.OperationHistoryEntity
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode

fun OperationHistoryEntity.toDomainModel() = OperationHistoryModel(
    id = id,
    operationType = enumValueOrDefault(operationType, OperationType.INSTALL),
    status = enumValueOrDefault(status, OperationStatus.FAILED),
    packageName = packageName,
    appLabel = appLabel,
    timestamp = timestamp,
    isFreshInstall = isFreshInstall,
    versionChange = enumValueOrDefault(versionChange, VersionChange.UNKNOWN),
    oldVersionName = oldVersionName,
    oldVersionCode = oldVersionCode,
    newVersionName = newVersionName,
    newVersionCode = newVersionCode,
    sourcePaths = sourcePaths,
    initiatorPackageName = initiatorPackageName,
    installerPackageName = installerPackageName,
    installMethod = enumValueOrDefault(installMethod, InstallMethod.PACKAGE_MANAGER),
    authorizer = Authorizer.fromValueOrDefault(authorizer),
    installMode = InstallMode.entries.find { it.value == installMode } ?: InstallMode.Dialog,
    errorSummary = errorSummary,
    errorType = errorType
)

fun OperationHistoryModel.toEntity() = OperationHistoryEntity(
    id = id,
    operationType = operationType.name,
    status = status.name,
    packageName = packageName,
    appLabel = appLabel,
    timestamp = timestamp,
    isFreshInstall = isFreshInstall,
    versionChange = versionChange.name,
    oldVersionName = oldVersionName,
    oldVersionCode = oldVersionCode,
    newVersionName = newVersionName,
    newVersionCode = newVersionCode,
    sourcePaths = sourcePaths,
    initiatorPackageName = initiatorPackageName,
    installerPackageName = installerPackageName,
    installMethod = installMethod.name,
    authorizer = authorizer.value,
    installMode = installMode.value,
    errorSummary = errorSummary,
    errorType = errorType
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
