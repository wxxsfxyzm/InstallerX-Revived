// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.settings.mapper

import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallMode
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationHistoryMapperTest {
    @Test
    fun `room mapping preserves operation session key`() {
        val model = OperationHistoryModel(
            operationType = OperationType.INSTALL,
            status = OperationStatus.SUCCESS,
            packageName = "com.rosan.installer",
            installMethod = InstallMethod.PACKAGE_MANAGER,
            authorizer = Authorizer.Root,
            installMode = InstallMode.Dialog,
            operationSessionKey = "self-update-session",
        )

        assertEquals("self-update-session", model.toEntity().toDomainModel().operationSessionKey)
    }
}
