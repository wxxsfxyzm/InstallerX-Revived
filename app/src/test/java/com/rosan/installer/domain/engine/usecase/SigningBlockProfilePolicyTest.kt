// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.model.packageinfo.SigningBlockCertificateStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigningBlockProfilePolicyTest {
    @Test
    fun `matching lightweight certificates bypass unknown policy`() {
        assertFalse(
            SigningBlockCertificateStatus.MATCH.isBlockedByUnknownPolicy(
                allowSigUnknown = false,
            ),
        )
    }

    @Test
    fun `uncomparable lightweight certificates follow unknown policy`() {
        assertTrue(
            SigningBlockCertificateStatus.UNKNOWN.isBlockedByUnknownPolicy(
                allowSigUnknown = false,
            ),
        )
        assertFalse(
            SigningBlockCertificateStatus.UNKNOWN.isBlockedByUnknownPolicy(
                allowSigUnknown = true,
            ),
        )
    }
}
