// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.engine.repository

import com.rosan.installer.domain.engine.model.AnalyseExtraEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.AppSignatureInfo
import com.rosan.installer.domain.engine.model.packageinfo.SignatureVerificationStatus
import com.rosan.installer.domain.engine.model.source.DataEntity
import com.rosan.installer.domain.engine.model.source.DataType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyserRepositoryImplTest {
    @Test
    fun `signing block metadata requests installed signatures`() {
        val entity = AppEntity.BaseEntity(
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
            signatureInfo = AppSignatureInfo(
                verified = false,
                signerSha256Set = setOf("declared-signer"),
                certificates = emptyList(),
                verificationStatus = SignatureVerificationStatus.SIGNING_BLOCK_ONLY
            )
        )
        val enabled = AnalyseExtraEntity(
            cacheDirectory = "cache",
            checkAppSignature = true
        )
        val disabled = enabled.copy(checkAppSignature = false)

        assertTrue(shouldLoadInstalledSignatures(listOf(entity), enabled))
        assertFalse(shouldLoadInstalledSignatures(listOf(entity), disabled))
        assertFalse(shouldLoadInstalledSignatures(listOf(entity.copy(signatureInfo = null)), enabled))
    }
}
