// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.packageupdate.usecase

import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.usecase.VersionChangeResolver
import com.rosan.installer.domain.packageupdate.model.PendingSelfUpdateHistory

fun PendingSelfUpdateHistory.toSuccessfulOperationHistory(
    actualNewVersionName: String? = newVersionName,
    actualNewVersionCode: Long? = newVersionCode,
    installerPackageName: String? = null,
    timestamp: Long = System.currentTimeMillis(),
) = OperationHistoryModel(
    operationType = OperationType.INSTALL,
    status = OperationStatus.SUCCESS,
    packageName = packageName,
    appLabel = appLabel,
    timestamp = timestamp,
    isFreshInstall = oldVersionCode == null,
    versionChange = VersionChangeResolver.resolve(oldVersionCode, actualNewVersionCode),
    oldVersionName = oldVersionName,
    oldVersionCode = oldVersionCode,
    newVersionName = actualNewVersionName,
    newVersionCode = actualNewVersionCode,
    sourcePaths = sourcePaths,
    initiatorPackageName = initiatorPackageName,
    installerPackageName = installerPackageName,
    installMethod = InstallMethod.PACKAGE_MANAGER,
    authorizer = authorizer,
    installMode = installMode,
    operationSessionKey = operationSessionKey,
)
