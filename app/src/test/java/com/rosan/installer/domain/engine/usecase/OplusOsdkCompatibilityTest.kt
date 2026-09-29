// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OplusOsdkCompatibilityTest {
    @Test
    fun `compares numeric version components`() {
        assertTrue(isOplusOsdkIncompatible("15.10", "15.9"))
        assertTrue(isOplusOsdkIncompatible("15.1", "15"))
        assertFalse(isOplusOsdkIncompatible("15.0", "15"))
        assertFalse(isOplusOsdkIncompatible("15.9", "15.10"))
    }

    @Test
    fun `unknown versions do not block installation`() {
        assertFalse(isOplusOsdkIncompatible(null, "15.0"))
        assertFalse(isOplusOsdkIncompatible("15.0", null))
        assertFalse(isOplusOsdkIncompatible("15.beta", "14.0"))
        assertFalse(isOplusOsdkIncompatible("15.0", "14.beta"))
    }
}
