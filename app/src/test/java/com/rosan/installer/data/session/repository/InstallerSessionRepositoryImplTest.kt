// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.repository

import com.rosan.installer.domain.session.model.UninstallInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull

class InstallerSessionRepositoryImplTest {
    @Test
    fun `action channel buffers once without replaying consumed commands`() = runTest {
        val repository = InstallerSessionRepositoryImpl("test") {}

        repository.analyse()

        assertEquals(InstallerSessionRepositoryImpl.Action.Analyse, repository.action.first())
        assertNull(withTimeoutOrNull(1) { repository.action.first() })
    }

    @Test
    fun `platform session tracker retains only active ids`() {
        val repository = InstallerSessionRepositoryImpl("test") {}

        repository.setPlatformSessionActive(41, true)
        repository.setPlatformSessionActive(42, true)
        repository.setPlatformSessionActive(41, false)

        assertEquals(setOf(42), repository.activePlatformSessionIds.value)
    }

    @Test
    fun `confirming uninstall preserves clone user and resolved metadata`() = runTest {
        val repository = InstallerSessionRepositoryImpl("test") {}
        val info = UninstallInfo("example.app", userId = 11, appLabel = "Clone app", versionCode = 123)
        repository.uninstallInfo.value = info
        repository.config = repository.config.copy(enableCustomizeUser = true, targetUserId = 0)

        repository.uninstall("example.app")

        assertEquals(info, repository.uninstallInfo.value)
        assertEquals(InstallerSessionRepositoryImpl.Action.Uninstall("example.app"), repository.action.first())
    }

    @Test
    fun `another package cannot reuse the resolved uninstall user`() {
        val repository = InstallerSessionRepositoryImpl("test") {}
        val info = UninstallInfo("example.app", userId = 11)
        repository.uninstallInfo.value = info

        assertFailsWith<IllegalStateException> { repository.uninstall("another.app") }
        assertEquals(info, repository.uninstallInfo.value)
    }
}
