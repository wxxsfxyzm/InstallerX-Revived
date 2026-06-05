// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.provider.InstalledAppInfoProvider
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.domain.history.usecase.RecordOperationHistoryUseCase
import com.rosan.installer.domain.history.usecase.historyErrorSummary
import com.rosan.installer.domain.history.usecase.historyErrorType
import com.rosan.installer.domain.settings.model.config.ConfigModel
import timber.log.Timber

/**
 * UseCase for executing the uninstallation of a package.
 */
class ProcessUninstallUseCase(
    private val appInstaller: AppInstallerRepository,
    private val installedAppInfoProvider: InstalledAppInfoProvider,
    private val recordOperationHistory: RecordOperationHistoryUseCase
) {
    /**
     * Executes the uninstallation work.
     */
    suspend operator fun invoke(
        config: ConfigModel,
        packageName: String
    ) {
        val installed = installedAppInfoProvider.getByPackageName(packageName)
        val result = runCatching {
            appInstaller.doUninstallWork(config, packageName)
        }

        runCatching {
            recordOperationHistory(
                OperationHistoryModel(
                    operationType = OperationType.UNINSTALL,
                    status = if (result.isSuccess) OperationStatus.SUCCESS else OperationStatus.FAILED,
                    packageName = packageName,
                    appLabel = installed?.label,
                    isFreshInstall = null,
                    versionChange = VersionChange.UNKNOWN,
                    oldVersionName = installed?.versionName,
                    oldVersionCode = installed?.versionCode,
                    newVersionName = null,
                    newVersionCode = null,
                    initiatorPackageName = config.initiatorPackageName,
                    installMethod = InstallMethod.PACKAGE_MANAGER,
                    authorizer = config.authorizer,
                    installMode = config.installMode,
                    errorSummary = result.exceptionOrNull()?.historyErrorSummary(),
                    errorType = result.exceptionOrNull()?.historyErrorType()
                )
            )
        }.onFailure { e ->
            Timber.e(e, "Failed to record uninstall history for $packageName")
        }

        result.onFailure { throw it }
    }
}
