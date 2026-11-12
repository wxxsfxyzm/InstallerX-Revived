// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import com.rosan.installer.data.engine.executor.appinstaller.validateUninstallUser
import com.rosan.installer.domain.engine.exception.UninstallException
import com.rosan.installer.domain.engine.model.error.UninstallErrorType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UninstallUserTest {
    @Test
    fun `launcher clone user overrides the host user`() {
        assertEquals(11, resolveUninstallUserId(0, true, 11))
        assertEquals(0, resolveUninstallUserId(11, true, 0))
    }

    @Test
    fun `requests without a user target the current process user`() {
        for (userId in listOf(0, 10, 11, 999)) {
            assertEquals(userId, resolveUninstallUserId(userId, false, null))
        }
    }

    @Test
    fun `malformed and special user extras cannot fall back to the host user`() {
        for (userId in listOf(null, -1, -2, -3)) {
            assertFailsWith<IllegalArgumentException> { resolveUninstallUserId(0, true, userId) }
        }
    }

    @Test
    fun `unprivileged backend rejects cross user requests instead of uninstalling host app`() {
        val userId = resolveUninstallUserId(0, true, 11)
        val error = assertFailsWith<UninstallException> { validateUninstallUser(0, userId) }
        assertEquals(UninstallErrorType.USER_RESTRICTED, error.errorType)
        validateUninstallUser(11, userId)
        validateUninstallUser(0, 0)
    }
}
