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
    // 使用你定义好的数据结构，或者在 Domain 层新建一个专门的 DTO
    suspend operator fun invoke(id: Long?, globalAuthorizer: Authorizer): ConfigModel {
        var model = id?.let { configRepo.find(it) } ?: ConfigModel.default.copy(name = "")

        // 业务规则 1：加载 UID
        if (!model.installRequester.isNullOrEmpty()) {
            val uid = systemEnvProvider.getPackageUid(model.installRequester)
            model.callingFromUid = uid
        }

        // 业务规则 2：Dhizuku 降级限制
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
