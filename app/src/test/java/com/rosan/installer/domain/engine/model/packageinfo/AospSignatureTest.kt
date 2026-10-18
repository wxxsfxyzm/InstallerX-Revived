// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

import kotlin.test.Test
import kotlin.test.assertEquals

class AospSignatureTest {
    @Test
    fun `recognizes the public AOSP APK development certificates`() {
        val fingerprints = listOf(
            "a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc" to "testkey",
            "c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8" to "platform",
            "28bbfe4a7b97e74681dc55c2fbb6ccb8d6c74963733f6af6ae74d8c3a6e879fd" to "shared",
            "465983f7791f2abeb43ea2cbdc7f21a8260b72bc08a55c839fc1a43bc741a81e" to "media",
            "e1dbadce60dc080d15b58a014b0dcf9400e24de23fa00b287a5a982bfebda2ee" to "networkstack",
            "a6ccc500ff0e7421200eb66a7fe174ef1b00e52ca91727070cbedf061ff76c35" to "bluetooth",
            "abf21f9e2af1d881cc673fddcefa6ed9c269a437bd64b279cf45844cfd589126" to "sdk_sandbox",
            "fae9122a8721d6e2a196d2224dffcf773c9127e2bb956cbddb40b009192ffdfd" to "nfc",
        )
        for ((fingerprint, keyName) in fingerprints) {
            assertEquals(setOf(keyName), signature(fingerprint).aospSigningKeys(), fingerprint)
            assertEquals(setOf(keyName), signature(fingerprint.uppercase()).aospSigningKeys(), fingerprint)
        }
    }

    @Test
    fun `a matching certificate declaration requires verified APK contents`() {
        for (status in listOf(SignatureVerificationStatus.FAILED, SignatureVerificationStatus.SIGNING_BLOCK_ONLY)) {
            assertEquals(emptySet(), signature(TESTKEY).copy(verified = false, verificationStatus = status).aospSigningKeys())
        }
    }

    @Test
    fun `certificate names rotation history and source stamps cannot identify an AOSP signer`() {
        val info = signature(OTHER).copy(
            certificates = listOf(certificate(OTHER)),
            signingCertificateHistory = listOf(certificate(TESTKEY), certificate(OTHER)),
            sourceStampInfo = SourceStampInfo(
                verificationStatus = SourceStampVerificationStatus.VERIFIED,
                declaredCertificateSha256 = TESTKEY,
                certificate = certificate(TESTKEY),
            ),
        )
        assertEquals(emptySet(), info.aospSigningKeys())
        assertEquals(emptySet(), info.copy(signerSha256Set = emptySet()).aospSigningKeys())
        assertEquals(emptySet(), info.copy(fallbackIdentity = SigningIdentity(setOf(OTHER), listOf(TESTKEY, OTHER))).aospSigningKeys())
    }

    @Test
    fun `any current signer can identify AOSP including the classical hybrid member`() {
        assertEquals(setOf("testkey"), signature(OTHER).copy(signerSha256Set = setOf(OTHER, TESTKEY), hasMultipleSigners = true).aospSigningKeys())
        assertEquals(setOf("testkey"), signature(OTHER).copy(isHybrid = true, hybridClassicalCertificate = certificate(TESTKEY)).aospSigningKeys())
        assertEquals(emptySet(), signature(OTHER).copy(hybridClassicalCertificate = certificate(TESTKEY)).aospSigningKeys())
    }

    @Test
    fun `verified older scheme signers remain part of the APK signature`() {
        val info = signature(OTHER).copy(fallbackIdentity = SigningIdentity(setOf(TESTKEY)))
        assertEquals(setOf("testkey"), info.aospSigningKeys())
        assertEquals(emptySet(), info.copy(verified = false).aospSigningKeys())
        val combined = info.copy(
            isHybrid = true,
            hybridClassicalCertificate = certificate(TESTKEY),
            fallbackIdentity = SigningIdentity(setOf(TESTKEY, PLATFORM)),
        )
        assertEquals(setOf("testkey", "platform"), combined.aospSigningKeys())
    }

    private fun signature(sha256: String) = AppSignatureInfo(verified = true, signerSha256Set = setOf(sha256), certificates = emptyList())

    private fun certificate(sha256: String) = SignatureCertificateInfo(
        sha256, "", "CN=Android, O=Android", "CN=Android, O=Android", "1", null, null, "RSA", "SHA256withRSA",
    )

    private companion object {
        const val TESTKEY = "a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc"
        const val PLATFORM = "c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8"
        val OTHER = "12".repeat(32)
    }
}
