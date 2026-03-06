// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.data.updater.repo.AppUpdater
import com.rosan.installer.data.updater.repo.UpdateChecker
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel

class PerformAppUpdateUseCase(
    private val appUpdater: AppUpdater
) {
    suspend operator fun invoke(updateInfo: UpdateChecker.CheckResult?, currentAuthorizer: Authorizer) {
        if (updateInfo == null || !updateInfo.hasUpdate || updateInfo.downloadUrl.isEmpty()) {
            throw IllegalStateException("No valid update info found.")
        }
        val config = ConfigModel.default.copy(authorizer = currentAuthorizer)
        // 注意这里的 appUpdater 还没被抽象，但作为过渡，在 UseCase 层调用它远好于在 ViewModel 中
        appUpdater.performInAppUpdate(updateInfo.downloadUrl, config)
    }
}
