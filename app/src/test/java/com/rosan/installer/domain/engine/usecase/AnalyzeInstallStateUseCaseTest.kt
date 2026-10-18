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
import com.rosan.installer.domain.engine.model.state.DomainInstallState
import com.rosan.installer.domain.engine.model.state.InstallActionType
import com.rosan.installer.domain.engine.model.state.InstallNotice
import com.rosan.installer.domain.session.model.SelectInstallEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AnalyzeInstallStateUseCaseTest {
    @Test
    fun `AOSP badge is shown even when ordinary matching signature details are hidden`() {
        val signature = aospSignature()
        val entities = listOf(SelectInstallEntity(aospBase(signature), true))
        val result = analyzeAosp(entities)
        assertEquals(listOf(InstallNotice.AospSignature(setOf("testkey"))), result.notices)
        assertEquals(InstallActionType.INSTALL, result.actionType)
        val matching = analyzeAosp(entities, installedSignature = signature, signatureMatchStatus = SignatureMatchStatus.MATCH)
        assertEquals(listOf(InstallNotice.AospSignature(setOf("testkey"))), matching.notices)
        assertEquals(InstallActionType.REINSTALL, matching.actionType)
    }

    @Test
    fun `AOSP badge follows selected APKs and includes split only installs without duplication`() {
        val base = aospBase(aospSignature())
        val split = AppEntity.SplitEntity(
            base.packageName,
            DataEntity.FileEntity("config.apk"),
            "config.test",
            "36",
            "28",
            null,
            size = 0,
            signatureInfo = aospSignature(),
        )
        for ((selectBase, selectSplit, expectedCount) in listOf(Triple(true, true, 1), Triple(false, true, 1), Triple(false, false, 0))) {
            val result = analyzeAosp(listOf(SelectInstallEntity(base, selectBase), SelectInstallEntity(split, selectSplit)))
            assertEquals(expectedCount, result.notices.count { it is InstallNotice.AospSignature })
            if (expectedCount > 0) assertEquals(setOf("testkey"), assertIs<InstallNotice.AospSignature>(result.notices.single()).keyNames)
        }
        val customSplit = split.copy(signatureInfo = aospSignature().copy(signerSha256Set = setOf("private-signer")))
        val customResult = analyzeAosp(listOf(SelectInstallEntity(base, false), SelectInstallEntity(customSplit, true)))
        assertFalse(customResult.notices.any { it is InstallNotice.AospSignature })
        val platformSplit = split.copy(
            signatureInfo = aospSignature().copy(signerSha256Set = setOf("c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8")),
        )
        val combined = analyzeAosp(listOf(SelectInstallEntity(base, true), SelectInstallEntity(platformSplit, true)))
        assertEquals(setOf("testkey", "platform"), assertIs<InstallNotice.AospSignature>(combined.notices.single()).keyNames)
    }

    @Test
    fun `disabled analysis and installed app certificates cannot produce an AOSP badge`() {
        val aosp = aospSignature()
        val pending = listOf(SelectInstallEntity(aospBase(aosp), true))
        assertFalse(analyzeAosp(pending, checkSignature = false).notices.any { it is InstallNotice.AospSignature })
        val custom = listOf(SelectInstallEntity(aospBase(aosp.copy(signerSha256Set = setOf("private-signer"))), true))
        assertFalse(analyzeAosp(custom, installedSignature = aosp).notices.any { it is InstallNotice.AospSignature })
    }

    @Test
    fun `AOSP badge does not relax a signature mismatch`() {
        val pending = listOf(SelectInstallEntity(aospBase(aospSignature()), true))
        val custom = aospSignature().copy(signerSha256Set = setOf("private-signer"))
        val result = analyzeAosp(pending, installedSignature = custom)
        assertTrue(result.notices.any { it is InstallNotice.AospSignature })
        assertTrue(result.notices.any { it is InstallNotice.SignatureMismatch })
        assertEquals(InstallActionType.SIGNATURE_MISMATCH_INSTALL_ANYWAY, result.actionType)
    }

    @Test
    fun `Oplus OS SDK mismatch only blocks checked standalone APK`() {
        val base = AppEntity.BaseEntity(
            packageName = "example.app",
            sharedUserId = null,
            data = DataEntity.FileEntity("example.apk"),
            versionCode = 1,
            versionName = "1",
            label = "Example",
            icon = null,
            targetSdk = "36",
            minSdk = "28",
            minOsdkVersion = "15.1",
            sourceType = DataType.APK,
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

        fun analyze(check: Boolean, device: String?) = AnalyzeInstallStateUseCase()(
            currentPackage = currentPackage,
            entityToInstall = base,
            primaryEntity = base,
            isSplitUpdateMode = false,
            containerType = DataType.APK,
            systemArch = Architecture.ARM64,
            systemSdkInt = 36,
            checkAppSignature = false,
            checkOplusOsdk = check,
            deviceOplusOsdkVersion = device,
        )

        val incompatible = analyze(true, "15.0")
        assertTrue(incompatible.isOplusOsdkIncompatible)
        assertEquals(InstallNotice.OplusOsdkIncompatible("15.1", "15.0"), incompatible.notices.single())
        assertFalse(analyze(false, "15.0").isOplusOsdkIncompatible)
        assertFalse(analyze(true, "15.1").isOplusOsdkIncompatible)
        assertFalse(analyze(true, null).isOplusOsdkIncompatible)
    }

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
    private fun aospSignature() = AppSignatureInfo(
        verified = true,
        signerSha256Set = setOf("a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc"),
        certificates = emptyList(),
    )

    private fun aospBase(signature: AppSignatureInfo) = AppEntity.BaseEntity(
        packageName = "example.app", sharedUserId = null, data = DataEntity.FileEntity("example.apk"),
        versionCode = 1, versionName = "1", label = "Example", icon = null, targetSdk = "36", minSdk = "28",
        sourceType = DataType.APK, signatureInfo = signature,
    )

    private fun analyzeAosp(
        entities: List<SelectInstallEntity>,
        checkSignature: Boolean = true,
        installedSignature: AppSignatureInfo? = null,
        signatureMatchStatus: SignatureMatchStatus = if (installedSignature == null) SignatureMatchStatus.NOT_INSTALLED else SignatureMatchStatus.MISMATCH,
    ): DomainInstallState {
        val base = entities.filter { it.selected }.map { it.app }.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        val currentPackage = PackageAnalysisResult(
            packageName = "example.app",
            sessionMode = SessionMode.Single,
            appEntities = entities,
            installedAppInfo = installedSignature?.let {
                InstalledAppInfo("example.app", null, "Example", 1, "1", null, 28, 36, signatureInfo = it)
            },
            signatureCheckPerformed = true,
            signatureMatchStatus = signatureMatchStatus,
            identityStatus = PackageIdentityStatus.NOT_APPLICABLE,
        )
        return AnalyzeInstallStateUseCase()(
            currentPackage = currentPackage,
            entityToInstall = base,
            primaryEntity = base ?: entities.firstOrNull { it.selected }?.app,
            isSplitUpdateMode = base == null,
            containerType = DataType.APKS,
            systemArch = Architecture.ARM64,
            systemSdkInt = 36,
            checkAppSignature = checkSignature,
        )
    }
}
