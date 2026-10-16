// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.packageinfo

import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.session.model.SelectInstallEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigningIdentityTest {
    @Test fun `same hybrid pair matches`() {
        assertEquals(SignatureMatchStatus.MATCH, match(hybrid("C", "P"), hybrid("C", "P")))
    }

    @Test fun `reusing either active hybrid key is incompatible`() {
        for ((one, two) in listOf(
            hybrid("C", "P") to hybrid("D", "P"),
            hybrid("C", "P") to hybrid("C", "Q"),
            single("P") to hybrid("C", "P"),
            single("C") to hybrid("C", "P"),
        )) {
            assertEquals(SignatureMatchStatus.MISMATCH, match(one, two))
            assertEquals(SignatureMatchStatus.MISMATCH, match(two, one))
        }
    }

    @Test fun `incomplete hybrid identity cannot match a PQC hash alone`() {
        val incomplete = hybrid("C", "P").copy(hybridClassicalCertificate = null)
        assertEquals(SignatureMatchStatus.UNKNOWN_ERROR, match(incomplete, hybrid("C", "P")))
    }

    @Test fun `verified forward rotation requires installed data capability`() {
        for (allowed in listOf(true, false)) {
            val next = single("B", listOf("A", "B"), mapOf("A" to SigningCertificateCapabilities(allowed, false)))
            assertEquals(if (allowed) SignatureMatchStatus.ROTATION_COMPATIBLE else SignatureMatchStatus.MISMATCH, match(next, single("A")))
        }
    }

    @Test fun `missing forward capabilities are unconfirmed`() {
        assertEquals(SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED, match(single("B", listOf("A", "B")), single("A")))
    }

    @Test fun `installed certificate history alone does not authorize rollback`() {
        val installed = single("B", listOf("A", "B"))
        assertEquals(SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED, match(single("A"), installed))
    }

    @Test fun `known rollback permission is checked in installed to pending direction`() {
        for (allowed in listOf(true, false)) {
            val installed = single("B", listOf("A", "B"), mapOf("A" to SigningCertificateCapabilities(true, allowed)))
            assertEquals(if (allowed) SignatureMatchStatus.ROTATION_COMPATIBLE else SignatureMatchStatus.MISMATCH, match(single("A"), installed))
        }
    }

    @Test fun `valid hybrid rotation must authorize both previous hybrid keys`() {
        val installed = hybrid("C", "P")
        val flags = mapOf("C" to SigningCertificateCapabilities(true, false), "P" to SigningCertificateCapabilities(true, false))
        val next = hybrid("D", "Q", listOf("C", "P", "D", "Q"), flags)
        assertEquals(SignatureMatchStatus.ROTATION_COMPATIBLE, match(next, installed))
        assertEquals(SignatureMatchStatus.MISMATCH, match(next.copy(signingCertificateHistory = listOf(cert("P"), cert("D"), cert("Q"))), installed))
        assertEquals(SignatureMatchStatus.MISMATCH, match(next.copy(lineageCapabilities = flags + ("C" to SigningCertificateCapabilities(false, false))), installed))
    }

    @Test fun `ordinary multisigners require the complete signer set`() {
        val multi = single("A").copy(signerSha256Set = setOf("A", "B"), certificates = listOf(cert("A"), cert("B")), hasMultipleSigners = true)
        assertEquals(SignatureMatchStatus.MATCH, match(multi, multi))
        assertEquals(SignatureMatchStatus.MISMATCH, match(single("A"), multi))
        assertEquals(SignatureMatchStatus.MISMATCH, match(multi.copy(signerSha256Set = setOf("A", "C")), multi))
    }

    @Test fun `ambiguous platform scheme does not guess a matching fallback`() {
        val pending = hybrid("C", "P").copy(fallbackIdentity = single("A").signingIdentity)
        assertEquals(SignatureMatchStatus.CANDIDATE_ROTATION_UNCONFIRMED, match(pending, single("A")))
        assertEquals(SignatureMatchStatus.MATCH, match(pending, hybrid("C", "P")))
    }

    @Test fun `agreeing platform alternatives can confirm rotation without a platform probe`() {
        val flags = mapOf("A" to SigningCertificateCapabilities(true, false))
        val pending = hybrid("C", "P", listOf("A", "C", "P"), flags)
            .copy(fallbackIdentity = single("B", listOf("A", "B"), flags).signingIdentity)
        assertEquals(SignatureMatchStatus.ROTATION_COMPATIBLE, match(pending, single("A")))
    }

    @Test fun `split comparison includes the classical member of a hybrid`() {
        val result = selection(hybrid("C", "P"), hybrid("D", "P"))
        assertEquals(listOf("config.test.apk"), result.splitSignatureMismatchFiles)
    }

    @Test fun `installed hybrid scheme confirms matching splits despite different fallbacks`() {
        val base = hybrid("C", "P").copy(fallbackIdentity = single("A").signingIdentity)
        val split = hybrid("C", "P").copy(fallbackIdentity = single("B").signingIdentity)
        val result = selection(base, split, hybrid("C", "P"))
        assertFalse(result.hasIssues)
    }

    @Test fun `installed hybrid scheme rejects different pairs despite matching fallbacks`() {
        val fallback = single("A").signingIdentity
        val base = hybrid("C", "P").copy(fallbackIdentity = fallback)
        val split = hybrid("D", "P").copy(fallbackIdentity = fallback)
        val result = selection(base, split, hybrid("C", "P"))
        assertEquals(listOf("config.test.apk"), result.splitSignatureMismatchFiles)
        assertTrue(result.verificationFailedFiles.isEmpty())
    }

    @Test fun `ambiguous split comparisons follow unknown policy`() {
        val fallback = single("A").signingIdentity
        val base = hybrid("C", "P").copy(fallbackIdentity = fallback)
        val split = hybrid("D", "P").copy(fallbackIdentity = fallback)
        val incompleteHybrid = hybrid("C", "P").copy(hybridClassicalCertificate = null)
        for (installedSignature in listOf(null, single("A"), incompleteHybrid, hybrid("C", "P").copy(verified = false))) {
            val result = selection(base, split, installedSignature)
            assertTrue(result.splitSignatureMismatchFiles.isEmpty())
            assertEquals(listOf("config.test.apk"), result.verificationFailedFiles)
        }
    }

    @Test fun `lightweight comparison uses the complete pair and remains unverified`() {
        for (classical in listOf("C", "D")) {
            val pending = hybrid(classical, "P").copy(verified = false, verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY)
            val result = packageResult(pending, hybrid("C", "P"))
            assertEquals(
                if (classical == "C") SigningBlockCertificateStatus.MATCH else SigningBlockCertificateStatus.UNKNOWN,
                result.selectedSigningBlockCertificateStatus(),
            )
            assertFalse(pending.verified)
        }
    }

    @Test fun `lightweight fallback ambiguity stays unknown`() {
        val pending = hybrid("C", "P").copy(
            verified = false,
            verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY,
            fallbackIdentity = single("A").signingIdentity,
        )
        assertEquals(SigningBlockCertificateStatus.UNKNOWN, packageResult(pending, single("A")).selectedSigningBlockCertificateStatus())
    }

    private fun match(pending: AppSignatureInfo, installed: AppSignatureInfo) = base(pending).analyzePackageSignatureMatch(installed(installed))

    private fun selection(baseSignature: AppSignatureInfo, splitSignature: AppSignatureInfo, installedSignature: AppSignatureInfo? = null): PackageSignatureAnalysis {
        val split = AppEntity.SplitEntity(
            "test.app",
            DataEntity.FileEntity("unused"),
            "config.test",
            "37",
            "28",
            null,
            size = 0,
            signatureInfo = splitSignature,
        )
        return listOf(SelectInstallEntity(base(baseSignature), true), SelectInstallEntity(split, true))
            .analyzePackageSignatureSelection(installedSignature?.let(::installed))
    }

    private fun packageResult(pending: AppSignatureInfo, installed: AppSignatureInfo) = PackageAnalysisResult(
        packageName = "test.app",
        sessionMode = SessionMode.Single,
        appEntities = listOf(SelectInstallEntity(base(pending), true)),
        installedAppInfo = installed(installed),
        signatureCheckPerformed = true,
        signatureMatchStatus = SignatureMatchStatus.UNKNOWN_ERROR,
        identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
    )

    private fun base(info: AppSignatureInfo) = AppEntity.BaseEntity(
        packageName = "test.app", sharedUserId = null, data = DataEntity.FileEntity("unused"), versionCode = 1,
        versionName = "1", label = null, icon = null, targetSdk = "37", minSdk = "28", size = 0, signatureInfo = info,
    )

    private fun installed(info: AppSignatureInfo) = InstalledAppInfo(
        packageName = "test.app", icon = null, label = "test", versionCode = 1, versionName = "1",
        applicationInfo = null, minSdk = 28, targetSdk = 37, signatureInfo = info,
    )

    private fun single(name: String, history: List<String> = listOf(name), flags: Map<String, SigningCertificateCapabilities> = emptyMap()) = AppSignatureInfo(
        verified = true,
        signerSha256Set = setOf(name),
        certificates = listOf(cert(name)),
        signingCertificateHistory = history.map(::cert),
        lineageCapabilities = flags,
    )

    private fun hybrid(classical: String, pqc: String, history: List<String> = listOf(classical, pqc), flags: Map<String, SigningCertificateCapabilities> = emptyMap()) = single(pqc, history, flags).copy(isHybrid = true, hybridClassicalCertificate = cert(classical))

    private fun cert(name: String) = SignatureCertificateInfo(name, name, name, name, name, null, null, "RSA", "SHA256withRSA")
}
