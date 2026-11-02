// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.virustotal.model

import com.rosan.installer.domain.settings.model.preferences.VirusTotalMode

sealed interface VirusTotalScanState {
    val sha256: String?
        get() = null

    val isBusy: Boolean
        get() = this is Hashing || this is CheckingHash || this is Uploading || this is WaitingForAnalysis

    data object Idle : VirusTotalScanState

    data object Hashing : VirusTotalScanState

    data class CheckingHash(override val sha256: String) : VirusTotalScanState

    data class NotFound(override val sha256: String, val size: Long) : VirusTotalScanState

    data class Uploading(override val sha256: String?) : VirusTotalScanState

    data class WaitingForAnalysis(override val sha256: String?, val status: VirusTotalAnalysisStatus) : VirusTotalScanState

    data class Completed(val report: VirusTotalFileReport, val fromUpload: Boolean) : VirusTotalScanState {
        override val sha256: String
            get() = report.sha256
    }

    data class Failed(val kind: VirusTotalErrorKind, override val sha256: String? = null, val detail: String? = null) : VirusTotalScanState
}

object VirusTotalPolicy {
    fun isEnabled(mode: VirusTotalMode, apiKey: String): Boolean = mode != VirusTotalMode.OFF && apiKey.isNotBlank()

    /**
     * In Strict mode installation stays blocked while a scan has not produced a final outcome.
     * A failed scan never blocks: the error is surfaced in the UI and installation is allowed,
     * as required for oversized files and network errors.
     */
    fun blocksInstall(mode: VirusTotalMode, state: VirusTotalScanState): Boolean {
        if (mode != VirusTotalMode.STRICT) return false
        return when (state) {
            is VirusTotalScanState.Completed,
            is VirusTotalScanState.Failed,
            -> false

            else -> true
        }
    }
}
