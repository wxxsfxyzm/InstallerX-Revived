// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.repository.ConfigRepo

class ValidateAndSaveConfigUseCase(private val configRepo: ConfigRepo) {

    // 定义领域层错误枚举，不要在 Domain 层写 R.string.xxx
    enum class ValidationError {
        NAME_EMPTY,
        CUSTOM_AUTHORIZER_EMPTY,
        INSTALLER_EMPTY,
        REQUESTER_NOT_FOUND
    }

    suspend operator fun invoke(model: ConfigModel, hasRequesterUid: Boolean): Result<Unit> {
        // 执行业务校验
        if (model.name.isEmpty()) return Result.failure(Exception(ValidationError.NAME_EMPTY.name))
        if (model.authorizer == Authorizer.Customize && model.customizeAuthorizer.isEmpty()) {
            return Result.failure(Exception(ValidationError.CUSTOM_AUTHORIZER_EMPTY.name))
        }
        if (model.installer != null && model.installer.isEmpty()) {
            return Result.failure(Exception(ValidationError.INSTALLER_EMPTY.name))
        }
        if (model.installRequester != null && !hasRequesterUid) {
            return Result.failure(Exception(ValidationError.REQUESTER_NOT_FOUND.name))
        }

        // 校验通过，执行保存
        if (model.id == 0L) { // 假设 0 是新建
            configRepo.insert(model)
        } else {
            configRepo.update(model)
        }
        return Result.success(Unit)
    }
}
