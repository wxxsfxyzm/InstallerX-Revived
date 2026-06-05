// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.domain.settings.repository.ConfigRepository

class SaveConfigUseCase(private val configRepo: ConfigRepository) {

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
        if (model.installRequesterMode == InstallRequesterMode.Custom && !hasRequesterUid) {
            return Result.failure(SaveConfigException(Error.REQUESTER_NOT_FOUND))
        }
        if (model.installerMode == InstallerMode.Custom && model.installer.isNullOrBlank()) {
            return Result.failure(SaveConfigException(Error.INSTALLER_EMPTY))
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
