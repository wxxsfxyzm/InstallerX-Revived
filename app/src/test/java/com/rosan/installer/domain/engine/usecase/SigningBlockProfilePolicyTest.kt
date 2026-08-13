// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.model.packageinfo.SigningBlockCertificateStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SigningBlockProfilePolicyTest {
    @Test
    fun `matching lightweight certificates bypass unknown policy`() {
        assertNull(
            SigningBlockCertificateStatus.MATCH.profilePolicyViolation(
                allowSigMismatch = false,
                allowSigUnknown = false
            )
        )
    }

    @Test
    fun `mismatching lightweight certificates follow mismatch policy`() {
        assertEquals(
            ProfileSignaturePolicyViolation.MISMATCH,
            SigningBlockCertificateStatus.MISMATCH.profilePolicyViolation(
                allowSigMismatch = false,
                allowSigUnknown = true
            )
        )
        assertNull(
            SigningBlockCertificateStatus.MISMATCH.profilePolicyViolation(
                allowSigMismatch = true,
                allowSigUnknown = false
            )
        )
    }

    @Test
    fun `uncomparable lightweight certificates follow unknown policy`() {
        assertEquals(
            ProfileSignaturePolicyViolation.UNKNOWN,
            SigningBlockCertificateStatus.UNKNOWN.profilePolicyViolation(
                allowSigMismatch = true,
                allowSigUnknown = false
            )
        )
        assertNull(
            SigningBlockCertificateStatus.UNKNOWN.profilePolicyViolation(
                allowSigMismatch = false,
                allowSigUnknown = true
            )
        )
    }
}
