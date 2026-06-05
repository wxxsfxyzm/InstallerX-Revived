// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.ConfigRepository

class GetConfigDraftUseCase(
    private val configRepo: ConfigRepository,
    private val systemEnvProvider: SystemEnvProvider
) {
    suspend operator fun invoke(id: Long?, globalAuthorizer: Authorizer): ConfigModel {
        // Ensure device-specific optimal presets (e.g., Xiaomi) are applied for new configs.
        var model = id?.let { configRepo.find(it) } ?: ConfigModel.generateOptimalDefault().copy(name = "")

        if (model.installRequesterMode == InstallRequesterMode.Custom && !model.installRequester.isNullOrEmpty()) {
            val uid = systemEnvProvider.getPackageUid(model.installRequester)
            model = model.copy(callingFromUid = uid)
        }

        val effectiveAuthorizer = if (model.authorizer == Authorizer.Global) globalAuthorizer else model.authorizer
        if (effectiveAuthorizer == Authorizer.Dhizuku) {
            model = model.copy(
                installerMode = InstallerMode.Self,
                enableCustomizeUser = false,
                enableManualDexopt = false
            )
        }

        return model
    }
}
