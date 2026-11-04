// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.install.InstallOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NonAdbInstallUserTest {
    @Test
    fun `removes an explicit ADB flag while preserving other options`() {
        val otherOptions = InstallOption.AllowDowngrade.value or InstallOption.Internal.value
        assertEquals(otherOptions, nonAdbInstallFlags(otherOptions or InstallOption.FromAdb.value))
        assertEquals(otherOptions, nonAdbInstallFlags(otherOptions))
        assertEquals(0, nonAdbInstallFlags(InstallOption.FromAdb.value))
    }

    @Test
    fun `removes privileged permission grants from app owned sessions`() {
        val grantPermissions = InstallOption.GrantAllRequestedPermissions.value
        val otherOptions = InstallOption.AllowDowngrade.value or InstallOption.Internal.value
        assertEquals(0, nonAdbInstallFlags(grantPermissions))
        assertEquals(otherOptions, nonAdbInstallFlags(otherOptions or grantPermissions))
        assertEquals(otherOptions, nonAdbInstallFlags(otherOptions or grantPermissions or InstallOption.FromAdb.value))
    }

    @Test
    fun `accepts the current user including secondary profiles`() {
        for (user in listOf(0, 10, 999)) {
            validateNonAdbInstallUser(user, null, 0)
            validateNonAdbInstallUser(user, user, InstallOption.FromAdb.value)
        }
    }

    @Test
    fun `rejects other users and all user flags`() {
        for ((target, flags) in listOf(10 to 0, -1 to 0, 0 to InstallOption.AllUsers.value)) {
            val error = assertFailsWith<InstallException> { validateNonAdbInstallUser(0, target, flags) }
            assertEquals(InstallErrorType.NON_ADB_UNSUPPORTED_USER, error.errorType)
        }
        assertFailsWith<InstallException> {
            validateNonAdbInstallUser(10, null, InstallOption.AllUsers.value)
        }
    }
}
