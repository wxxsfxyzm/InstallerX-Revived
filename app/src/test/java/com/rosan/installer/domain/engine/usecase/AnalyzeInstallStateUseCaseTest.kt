// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.core.device.model.Architecture
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.InstalledAppInfo
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.PackageIdentityStatus
import com.rosan.installer.domain.engine.model.packageinfo.SignatureMatchStatus
import com.rosan.installer.domain.engine.model.packageinfo.SignatureVerificationStatus
import com.rosan.installer.domain.engine.model.packageinfo.SigningBlockCertificateStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.model.state.InstallActionType
import com.rosan.installer.domain.engine.model.state.InstallNotice
import com.rosan.installer.domain.session.model.SelectInstallEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AnalyzeInstallStateUseCaseTest {
    @Test
    fun `signing block declaration is informational and cannot produce mismatch action`() {
        val signatureInfo = AppSignatureInfo(
            verified = false,
            signerSha256Set = setOf("declared-signer"),
            certificates = emptyList(),
            declaredSchemes = listOf("V3"),
            verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY,
        )
        val base = AppEntity.BaseEntity(
            packageName = "example.app",
            sharedUserId = null,
            data = DataEntity.FileEntity("remote.apk"),
            versionCode = 1,
            versionName = "1",
            label = "Example",
            icon = null,
            targetSdk = "36",
            minSdk = "28",
            sourceType = DataType.APK,
            signatureInfo = signatureInfo,
        )
        val currentPackage = PackageAnalysisResult(
            packageName = base.packageName,
            sessionMode = SessionMode.Single,
            appEntities = listOf(SelectInstallEntity(base, selected = true)),
            installedAppInfo = null,
            signatureCheckPerformed = false,
            signatureMatchStatus = SignatureMatchStatus.MISMATCH,
            identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
        )

        val result = AnalyzeInstallStateUseCase()(
            currentPackage = currentPackage,
            entityToInstall = base,
            primaryEntity = base,
            isSplitUpdateMode = false,
            containerType = DataType.APK,
            systemArch = Architecture.ARM64,
            systemSdkInt = 36,
            checkAppSignature = true,
            showSignatureDetails = false,
        )

        assertEquals(InstallActionType.INSTALL, result.actionType)
        assertEquals(1, result.notices.size)
        val notice = assertIs<InstallNotice.SigningBlockOnly>(result.notices.single())
        assertEquals(SigningBlockCertificateStatus.NOT_INSTALLED, notice.certificateStatus)
        assertEquals(null, notice.details)
    }

    @Test
    fun `fresh install without a signer declaration is unknown`() {
        val signatureInfo = AppSignatureInfo(
            verified = false,
            signerSha256Set = emptySet(),
            certificates = emptyList(),
            verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY,
        )
        val base = AppEntity.BaseEntity(
            packageName = "example.app",
            sharedUserId = null,
            data = DataEntity.FileEntity("remote.apk"),
            versionCode = 1,
            versionName = "1",
            label = "Example",
            icon = null,
            targetSdk = "36",
            minSdk = "28",
            sourceType = DataType.APK,
            signatureInfo = signatureInfo,
        )
        val currentPackage = PackageAnalysisResult(
            packageName = base.packageName,
            sessionMode = SessionMode.Single,
            appEntities = listOf(SelectInstallEntity(base, selected = true)),
            installedAppInfo = null,
            signatureCheckPerformed = false,
            signatureMatchStatus = SignatureMatchStatus.NOT_INSTALLED,
            identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
        )

        val result = AnalyzeInstallStateUseCase()(
            currentPackage = currentPackage,
            entityToInstall = base,
            primaryEntity = base,
            isSplitUpdateMode = false,
            containerType = DataType.APK,
            systemArch = Architecture.ARM64,
            systemSdkInt = 36,
            checkAppSignature = true,
            showSignatureDetails = false,
        )

        val notice = assertIs<InstallNotice.SigningBlockOnly>(result.notices.single())
        assertEquals(SigningBlockCertificateStatus.UNKNOWN, notice.certificateStatus)
    }

    @Test
    fun `signing block summary compares certificates without exposing details`() {
        val cases = listOf(
            setOf("declared-signer") to SigningBlockCertificateStatus.MATCH,
            setOf("installed-signer") to SigningBlockCertificateStatus.UNKNOWN,
            emptySet<String>() to SigningBlockCertificateStatus.UNKNOWN,
        )

        cases.forEach { (installedSigners, expectedStatus) ->
            val pendingSignatureInfo = AppSignatureInfo(
                verified = false,
                signerSha256Set = setOf("declared-signer"),
                certificates = emptyList(),
                declaredSchemes = listOf("V3"),
                verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY,
            )
            val base = AppEntity.BaseEntity(
                packageName = "example.app",
                sharedUserId = null,
                data = DataEntity.FileEntity("remote.apk"),
                versionCode = 2,
                versionName = "2",
                label = "Example",
                icon = null,
                targetSdk = "36",
                minSdk = "28",
                sourceType = DataType.APK,
                signatureInfo = pendingSignatureInfo,
            )
            val installedSignatureInfo = AppSignatureInfo(
                verified = true,
                signerSha256Set = installedSigners,
                certificates = emptyList(),
            )
            val currentPackage = PackageAnalysisResult(
                packageName = base.packageName,
                sessionMode = SessionMode.Single,
                appEntities = listOf(SelectInstallEntity(base, selected = true)),
                installedAppInfo = InstalledAppInfo(
                    packageName = base.packageName,
                    icon = null,
                    label = "Example",
                    versionCode = 1,
                    versionName = "1",
                    applicationInfo = null,
                    minSdk = 28,
                    targetSdk = 35,
                    signatureInfo = installedSignatureInfo,
                ),
                signatureCheckPerformed = false,
                signatureMatchStatus = SignatureMatchStatus.MISMATCH,
                identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
            )

            val result = AnalyzeInstallStateUseCase()(
                currentPackage = currentPackage,
                entityToInstall = base,
                primaryEntity = base,
                isSplitUpdateMode = false,
                containerType = DataType.APK,
                systemArch = Architecture.ARM64,
                systemSdkInt = 36,
                checkAppSignature = true,
                showSignatureDetails = false,
            )

            assertEquals(InstallActionType.UPGRADE, result.actionType)
            val notice = assertIs<InstallNotice.SigningBlockOnly>(result.notices.single())
            assertEquals(expectedStatus, notice.certificateStatus)
            assertEquals(null, notice.details)
        }
    }
}
