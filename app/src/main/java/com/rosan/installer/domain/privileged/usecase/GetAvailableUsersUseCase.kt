// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
import com.rosan.installer.domain.settings.model.Authorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAvailableUsersUseCase(
    private val systemInfoProvider: SystemInfoProvider
) {
    /**
     * Retrieves the list of system users supported by the current environment.
     * @param authorizer The current authorization method.
     * @return Result<Map<UserId, UserName>>
     */
    suspend operator fun invoke(authorizer: Authorizer): Result<Map<Int, String>> {
        // Dhizuku does not support cross-user install/user querying.
        if (authorizer == Authorizer.Dhizuku) {
            return Result.success(emptyMap())
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                systemInfoProvider.getUsers(authorizer)
            }
        }
    }
}
