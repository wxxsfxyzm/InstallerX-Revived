// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.repository.ConfigRepo

class SaveConfigUseCase(private val configRepo: ConfigRepo) {

    enum class Error {
        NAME_EMPTY,
        CUSTOM_AUTHORIZER_EMPTY,
        INSTALLER_EMPTY,
        REQUESTER_NOT_FOUND
    }

    class SaveConfigException(val error: Error) : Exception("Save config failed: ${error.name}")

    suspend operator fun invoke(model: ConfigModel, hasRequesterUid: Boolean): Result<Unit> {
        // Business rule validations
        if (model.name.isEmpty()) {
            return Result.failure(SaveConfigException(Error.NAME_EMPTY))
        }
        if (model.authorizer == Authorizer.Customize && model.customizeAuthorizer.isEmpty()) {
            return Result.failure(SaveConfigException(Error.CUSTOM_AUTHORIZER_EMPTY))
        }
        if (model.installer != null && model.installer.isEmpty()) {
            return Result.failure(SaveConfigException(Error.INSTALLER_EMPTY))
        }
        if (model.installRequester != null && !hasRequesterUid) {
            return Result.failure(SaveConfigException(Error.REQUESTER_NOT_FOUND))
        }

        // Execution
        if (model.id == 0L) {
            configRepo.insert(model)
        } else {
            configRepo.update(model)
        }
        return Result.success(Unit)
    }
}
