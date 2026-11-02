// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalScanState
import com.rosan.installer.domain.virustotal.model.VirusTotalStats

@Composable
fun virusTotalErrorText(kind: VirusTotalErrorKind): String = stringResource(
    when (kind) {
        VirusTotalErrorKind.NO_API_KEY -> R.string.virustotal_error_no_api_key
        VirusTotalErrorKind.INTERNET_DISABLED -> R.string.virustotal_error_internet_disabled
        VirusTotalErrorKind.UNAUTHORIZED -> R.string.virustotal_error_unauthorized
        VirusTotalErrorKind.RATE_LIMITED -> R.string.virustotal_error_rate_limited
        VirusTotalErrorKind.FILE_TOO_LARGE -> R.string.virustotal_error_file_too_large
        VirusTotalErrorKind.NETWORK -> R.string.virustotal_error_network
        VirusTotalErrorKind.HTTP -> R.string.virustotal_error_http
        VirusTotalErrorKind.UNSUPPORTED_SOURCE -> R.string.virustotal_error_unsupported_source
        VirusTotalErrorKind.HASH_FAILED -> R.string.virustotal_error_hash_failed
        VirusTotalErrorKind.ANALYSIS_TIMEOUT -> R.string.virustotal_error_analysis_timeout
        VirusTotalErrorKind.UNKNOWN -> R.string.virustotal_error_unknown
    },
)

@Composable
fun virusTotalBusyText(state: VirusTotalScanState): String = stringResource(
    when (state) {
        is VirusTotalScanState.CheckingHash -> R.string.virustotal_checking_hash
        is VirusTotalScanState.Uploading -> R.string.virustotal_uploading
        is VirusTotalScanState.WaitingForAnalysis -> R.string.virustotal_waiting_analysis
        else -> R.string.virustotal_hashing
    },
)

@Composable
fun virusTotalResultSummary(stats: VirusTotalStats): String = if (stats.detections == 0) {
    stringResource(R.string.virustotal_result_clean_format, stats.total)
} else {
    stringResource(R.string.virustotal_result_flagged_format, stats.detections, stats.total)
}

@Composable
fun virusTotalResultDetail(stats: VirusTotalStats): String = stringResource(
    R.string.virustotal_result_detail_format,
    stats.malicious,
    stats.suspicious,
    stats.harmless,
    stats.undetected,
)
