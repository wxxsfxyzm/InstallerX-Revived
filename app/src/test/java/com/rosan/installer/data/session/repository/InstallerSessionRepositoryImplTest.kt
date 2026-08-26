// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.repository

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
