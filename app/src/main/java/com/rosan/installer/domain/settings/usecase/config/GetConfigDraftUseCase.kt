// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.provider.SystemEnvProvider
import com.rosan.installer.domain.settings.repository.ConfigRepo

class GetConfigDraftUseCase(
    private val configRepo: ConfigRepo,
    private val systemEnvProvider: SystemEnvProvider
) {
    suspend operator fun invoke(id: Long?, globalAuthorizer: Authorizer): ConfigModel {
        var model = id?.let { configRepo.find(it) } ?: ConfigModel.default.copy(name = "")

        if (!model.installRequester.isNullOrEmpty()) {
            val uid = systemEnvProvider.getPackageUid(model.installRequester)
            model.callingFromUid = uid
        }

        val effectiveAuthorizer = if (model.authorizer == Authorizer.Global) globalAuthorizer else model.authorizer
        if (effectiveAuthorizer == Authorizer.Dhizuku) {
            model = model.copy(
                installer = null,
                enableCustomizeUser = false,
                enableManualDexopt = false
            )
        }

        return model
    }
}
