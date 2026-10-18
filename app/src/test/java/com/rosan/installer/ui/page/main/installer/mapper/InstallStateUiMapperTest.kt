// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.mapper

import androidx.compose.ui.graphics.Color
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.SignatureCertificateInfo
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampInfo
import com.rosan.installer.domain.engine.model.packageinfo.SourceStampVerificationStatus
import com.rosan.installer.domain.engine.model.state.DomainInstallState
import com.rosan.installer.domain.engine.model.state.InstallActionType
import com.rosan.installer.domain.engine.model.state.InstallNotice
import com.rosan.installer.domain.engine.model.state.SignatureNoticeDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstallStateUiMapperTest {
    @Test
    fun `AOSP signature dialog names every matching key in a stable order`() {
        for ((keys, expectedKeys) in listOf(setOf("platform") to "platform", setOf("testkey", "platform") to "platform, testkey")) {
            val result = InstallStateUiMapper(resources()).mapToUiState(
                DomainInstallState(InstallActionType.INSTALL, listOf(InstallNotice.AospSignature(keys))),
            )
            val notice = result.notices.single()
            assertEquals("AOSP signature", notice.shortLabel)
            assertEquals("This package uses an AOSP signature.\n\nKeys: $expectedKeys", notice.fullDescription)
            assertEquals(Color.Gray, notice.color)
            assertEquals(com.rosan.installer.R.string.install, result.buttonTextId)
        }
    }

    @Test
    fun `source stamp row is only shown when a digest is available`() {
        val withoutStamp = description(null)
        assertEquals("New install\n\nPending signature\nNo certificates", withoutStamp)
        assertTrue(
            description(SourceStampInfo(declaredCertificateSha256 = "ab".repeat(32)))
                .endsWith("Source stamp SHA-256: ${List(32) { "AB" }.joinToString(":")}"),
        )
    }

    @Test
    fun `verified stamp information messages do not become verification failures`() {
        val message = "No digests are available in the source stamp for signature scheme: 31"
        val text = description(
            SourceStampInfo(
                verificationStatus = SourceStampVerificationStatus.VERIFIED,
                declaredCertificateSha256 = "ab".repeat(32),
                timestampEpochSeconds = 1790571492L,
                infoMessages = listOf(message),
            ),
        )

        assertTrue(text.contains("Verification status: Verified"))
        assertTrue(text.contains("Stamp time (UTC): 2026-09-28 04:58:12"))
        assertTrue(text.contains("Information\n- $message"))
        assertFalse(text.contains("Verification failed"))
        assertFalse(text.contains("Verification errors"))
    }

    @Test
    fun `failed stamps retain their digest and error while missing fields stay hidden`() {
        val text = description(
            SourceStampInfo(
                verificationStatus = SourceStampVerificationStatus.FAILED,
                declaredCertificateSha256 = "ab".repeat(32),
                errors = listOf("Invalid stamp signature"),
            ),
        )

        assertTrue(text.startsWith("New install"))
        assertTrue(text.contains("Verification status: Verification failed"))
        assertTrue(text.contains("Source stamp SHA-256:"))
        assertTrue(text.contains("Verification errors\n- Invalid stamp signature"))
        assertFalse(text.contains("Stamp time (UTC):"))
        assertFalse(text.contains("Source stamp certificate\n"))
    }

    @Test
    fun `hybrid history labels the two active certificate roles`() {
        val classical = SignatureCertificateInfo("cc", "cc", "classical", "issuer", "1", null, null, "RSA", "RSA")
        val pqc = classical.copy(sha256 = "dd", subject = "pqc", publicKeyAlgorithm = "ML-DSA")
        val info = AppSignatureInfo(
            verified = true,
            signerSha256Set = setOf(pqc.sha256),
            certificates = listOf(pqc),
            signingCertificateHistory = listOf(classical, pqc),
            isHybrid = true,
            hybridClassicalCertificate = classical,
        )
        val text = description(null, info)
        assertTrue(text.contains("(V3.2 classical certificate)"))
        assertTrue(text.contains("(V3.2 PQC certificate)"))
    }

    private fun description(stamp: SourceStampInfo?, signature: AppSignatureInfo? = null): String {
        val state = DomainInstallState(
            actionType = InstallActionType.INSTALL,
            notices = listOf(
                InstallNotice.SignatureSummary(
                    status = SignatureMatchStatus.NOT_INSTALLED,
                    details = SignatureNoticeDetails(
                        pendingSignatureInfo = signature ?: AppSignatureInfo(
                            verified = true,
                            signerSha256Set = emptySet(),
                            certificates = emptyList(),
                            sourceStampInfo = stamp,
                        ),
                        installedSignatureInfo = null,
                    ),
                ),
            ),
        )
        return InstallStateUiMapper(resources()).mapToUiState(state).notices.single().fullDescription
    }

    private fun resources() = InstallNoticeResources(
        tagDowngrade = "",
        textDowngrade = "",
        tagSignature = "",
        tagSignatureMatch = "",
        tagSignatureRotation = "",
        tagSignatureRotationUnconfirmed = "",
        tagAospSignature = "AOSP signature",
        textAospSignature = "This package uses an AOSP signature.\n\nKeys: %1\$s",
        textSigNewInstall = "New install",
        textSigMatch = "",
        textSigRotationCompatible = "",
        textSigCandidateRotationUnconfirmed = "",
        textSigMismatch = "",
        textSigUnknown = "",
        textSigAnalysisIssue = "",
        textSigSigningBlockNotInstalled = "",
        textSigSigningBlockMatch = "",
        textSigSigningBlockUnknown = "",
        labelPendingSignature = "Pending signature",
        labelInstalledSignature = "",
        labelSignatureAnalysisIssues = "",
        labelSignatureVerificationFailedFiles = "",
        labelSignatureSplitMismatchFiles = "",
        labelSignatureDuplicateSplitNames = "",
        labelSignatureSchemes = "",
        labelSignatureDeclaredSchemes = "",
        labelSignatureCertificate = "",
        labelSignatureCurrentCertificate = "",
        labelSignatureCertificateLineage = "",
        labelSignatureLineageCertificate = "",
        labelSignatureCurrentMarker = "",
        labelSignatureHybridClassical = "V3.2 classical certificate",
        labelSignatureHybridPqc = "V3.2 PQC certificate",
        labelSignatureSha256 = "",
        labelSignatureSourceStampSha256 = "Source stamp SHA-256",
        labelSignatureSourceStamp = "Source stamp",
        labelSignatureSourceStampStatus = "Verification status",
        textSourceStampUnverified = "Not verified",
        textSourceStampVerified = "Verified",
        textSourceStampFailed = "Verification failed",
        textSourceStampCertificateMismatch = "Certificate digest mismatch",
        textSourceStampVerificationError = "Unable to complete verification",
        labelSignatureSourceStampTimestamp = "Stamp time (UTC)",
        labelSignatureSourceStampCertificate = "Source stamp certificate",
        labelSignatureSourceStampLineage = "Source stamp certificate lineage",
        labelSignatureInformation = "Information",
        labelSignatureSha1 = "",
        labelSignatureSubject = "",
        labelSignatureIssuer = "",
        labelSignatureValidFrom = "",
        labelSignatureValidUntil = "",
        labelSignaturePublicKeyAlgorithm = "",
        labelSignatureAlgorithm = "",
        labelSignatureWarnings = "Verification warnings",
        labelSignatureErrors = "Verification errors",
        labelSignatureNoCertificates = "No certificates",
        tagSdk = "",
        textSdkIncompatible = "",
        tagOplusOsdk = "",
        textOplusOsdkIncompatible = "",
        tagArch32 = "",
        textArch32 = "",
        tagEmulated = "",
        textArchMismatchFormat = "",
        tagIdentical = "",
        textIdentical = "",
        tagXposed = "",
        labelXposedMinApi = "",
        labelXposedTargetApi = "",
        errorColor = Color.Red,
        tertiaryColor = Color.Gray,
        primaryColor = Color.Blue,
    )
}
