// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model

import com.rosan.installer.domain.engine.model.source.DataType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyseExtraEntityTest {
    @Test
    fun `split package signature checks are disabled by default`() {
        val extra = AnalyseExtraEntity(
            cacheDirectory = "cache",
            checkAppSignature = true,
        )

        assertTrue(extra.shouldCheckAppSignatures(DataType.APK))
        assertFalse(extra.shouldCheckAppSignatures(DataType.APKS))
        assertFalse(extra.shouldCheckAppSignatures(DataType.APKM))
        assertFalse(extra.shouldCheckAppSignatures(DataType.XAPK))
    }

    @Test
    fun `split package signature checks can be enabled`() {
        val extra = AnalyseExtraEntity(
            cacheDirectory = "cache",
            checkAppSignature = true,
            checkSplitPackageSignatures = true,
        )

        assertTrue(extra.shouldCheckAppSignatures(DataType.APKS))
        assertTrue(extra.shouldCheckAppSignatures(DataType.APKM))
        assertTrue(extra.shouldCheckAppSignatures(DataType.XAPK))
    }

    @Test
    fun `master signature switch disables every package type`() {
        val extra = AnalyseExtraEntity(
            cacheDirectory = "cache",
            checkAppSignature = false,
            checkSplitPackageSignatures = true,
        )

        assertFalse(extra.shouldCheckAppSignatures(DataType.APK))
        assertFalse(extra.shouldCheckAppSignatures(DataType.APKS))
    }
}
