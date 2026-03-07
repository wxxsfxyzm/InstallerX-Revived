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

    suspend operator fun invoke(model: ConfigModel, hasRequesterUid: Boolean): Result<Unit> {
        // Business rule validations
        if (model.name.isEmpty()) return Result.failure(Exception(Error.NAME_EMPTY.name))
        if (model.authorizer == Authorizer.Customize && model.customizeAuthorizer.isEmpty()) {
            return Result.failure(Exception(Error.CUSTOM_AUTHORIZER_EMPTY.name))
        }
        if (model.installer != null && model.installer.isEmpty()) {
            return Result.failure(Exception(Error.INSTALLER_EMPTY.name))
        }
        if (model.installRequester != null && !hasRequesterUid) {
            return Result.failure(Exception(Error.REQUESTER_NOT_FOUND.name))
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
