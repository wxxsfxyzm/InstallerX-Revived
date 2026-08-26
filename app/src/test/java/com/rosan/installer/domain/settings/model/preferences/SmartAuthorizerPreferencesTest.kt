// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.model.preferences

import com.rosan.installer.domain.settings.model.config.Authorizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartAuthorizerPreferencesTest {
    @Test
    fun `default candidates include no privilege when session install is supported`() {
        assertTrue(
            SmartAuthorizerPreferences.defaultCandidates(isSessionInstallSupported = true)
                .any { it.authorizer == Authorizer.None && it.enabled },
        )
    }

    @Test
    fun `default candidates exclude no privilege when session install is unsupported`() {
        assertTrue(
            SmartAuthorizerPreferences.defaultCandidates(isSessionInstallSupported = false)
                .none { it.authorizer == Authorizer.None },
        )
    }

    @Test
    fun `decode adds no privilege to legacy candidate list when supported`() {
        val candidates = SmartAuthorizerPreferences.decode(
            value = "root:1,shizuku:1,dhizuku:1",
            isSessionInstallSupported = true,
        )

        assertEquals(Authorizer.None, candidates.last().authorizer)
        assertTrue(candidates.last().enabled)
    }

    @Test
    fun `decode preserves configured no privilege candidate`() {
        val candidates = SmartAuthorizerPreferences.decode(
            value = "none:0,root:1",
            isSessionInstallSupported = true,
        )

        assertEquals(Authorizer.None, candidates.first().authorizer)
        assertEquals(false, candidates.first().enabled)
    }

    @Test
    fun `decode removes configured no privilege candidate when unsupported`() {
        val candidates = SmartAuthorizerPreferences.decode(
            value = "none:1,root:1",
            isSessionInstallSupported = false,
        )

        assertTrue(candidates.none { it.authorizer == Authorizer.None })
    }
}
