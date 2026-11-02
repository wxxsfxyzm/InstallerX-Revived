// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

import com.rosan.installer.domain.settings.model.preferences.VirusTotalMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VirusTotalPolicyTest {
    private val report = VirusTotalFileReport(sha256 = "abc", stats = VirusTotalStats(harmless = 10))

    @Test
    fun `strict blocks until a final outcome and failed scans fail open`() {
        assertTrue(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.Idle))
        assertTrue(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.Hashing))
        assertTrue(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.CheckingHash("abc")))
        assertTrue(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.Uploading("abc")))
        assertTrue(
            VirusTotalPolicy.blocksInstall(
                VirusTotalMode.STRICT,
                VirusTotalScanState.WaitingForAnalysis("abc", VirusTotalAnalysisStatus.IN_PROGRESS),
            ),
        )
        assertTrue(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.NotFound("abc", 1L)))
        assertFalse(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.Completed(report, fromUpload = true)))
        // Oversized files and network errors must be shown but allow installing.
        assertFalse(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.Failed(VirusTotalErrorKind.FILE_TOO_LARGE)))
        assertFalse(VirusTotalPolicy.blocksInstall(VirusTotalMode.STRICT, VirusTotalScanState.Failed(VirusTotalErrorKind.NETWORK)))
    }

    @Test
    fun `normal and off never block installation`() {
        assertFalse(VirusTotalPolicy.blocksInstall(VirusTotalMode.NORMAL, VirusTotalScanState.Idle))
        assertFalse(VirusTotalPolicy.blocksInstall(VirusTotalMode.NORMAL, VirusTotalScanState.Uploading("abc")))
        assertFalse(VirusTotalPolicy.blocksInstall(VirusTotalMode.OFF, VirusTotalScanState.Uploading("abc")))
    }

    @Test
    fun `enabled requires a mode and an api key`() {
        assertFalse(VirusTotalPolicy.isEnabled(VirusTotalMode.OFF, "key"))
        assertFalse(VirusTotalPolicy.isEnabled(VirusTotalMode.NORMAL, ""))
        assertTrue(VirusTotalPolicy.isEnabled(VirusTotalMode.NORMAL, "key"))
        assertTrue(VirusTotalPolicy.isEnabled(VirusTotalMode.STRICT, "key"))
    }
}
