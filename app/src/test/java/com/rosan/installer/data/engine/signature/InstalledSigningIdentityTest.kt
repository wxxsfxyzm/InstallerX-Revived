// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.signature

import com.rosan.installer.domain.engine.model.packageinfo.SignatureCertificateInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstalledSigningIdentityTest {
    @Test fun `platform hybrid history supplies both roles but no capability grants`() {
        for (algorithm in listOf("ML-DSA", "2.16.840.1.101.3.4.3.18", "2.16.840.1.101.3.4.3.19")) {
            val classical = cert("C", "RSA")
            val pqc = cert("P", algorithm)
            val result = mapInstalledSignatureInfo(listOf(pqc), listOf(cert("A", "RSA"), classical, pqc), false, 37)
            assertTrue(result.isHybrid)
            assertTrue(result.signingIdentity.isComplete)
            assertEquals(classical, result.hybridClassicalCertificate)
            assertEquals(setOf("P"), result.signerSha256Set)
            assertTrue(result.lineageCapabilities.isEmpty())
            assertNull(result.fallbackIdentity)
        }
    }

    @Test fun `missing hybrid classical key remains incomplete`() {
        val pqc = cert("P", "ML-DSA")
        val result = mapInstalledSignatureInfo(listOf(pqc), listOf(pqc), false, 37)
        assertTrue(result.isHybrid)
        assertFalse(result.signingIdentity.isComplete)
        assertNull(result.hybridClassicalCertificate)
    }

    @Test fun `ordinary signer sets retain their multiple signer semantics`() {
        val certificates = listOf(cert("A", "RSA"), cert("B", "EC"))
        val result = mapInstalledSignatureInfo(certificates, emptyList(), true, 37)
        assertFalse(result.isHybrid)
        assertTrue(result.hasMultipleSigners)
        assertEquals(setOf("A", "B"), result.signerSha256Set)
    }

    private fun cert(name: String, algorithm: String) = SignatureCertificateInfo(name, name, name, name, name, null, null, algorithm, algorithm)
}
