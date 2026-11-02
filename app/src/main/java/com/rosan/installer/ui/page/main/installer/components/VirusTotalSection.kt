// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.VirusTotalMode
import com.rosan.installer.domain.virustotal.model.VirusTotalErrorKind
import com.rosan.installer.domain.virustotal.model.VirusTotalScanState
import com.rosan.installer.ui.common.virusTotalBusyText
import com.rosan.installer.ui.common.virusTotalErrorText
import com.rosan.installer.ui.common.virusTotalResultDetail
import com.rosan.installer.ui.common.virusTotalResultSummary
import com.rosan.installer.ui.icons.AppIcons

@Composable
fun VirusTotalSection(
    state: VirusTotalScanState,
    mode: VirusTotalMode,
    onLookup: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = AppIcons.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.virustotal_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            when (state) {
                is VirusTotalScanState.Idle -> {
                    if (mode == VirusTotalMode.STRICT) {
                        Text(text = stringResource(R.string.virustotal_strict_waiting), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(text = stringResource(R.string.virustotal_not_scanned), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onLookup) {
                            Text(stringResource(R.string.virustotal_check_hash))
                        }
                    }
                }

                is VirusTotalScanState.Hashing,
                is VirusTotalScanState.CheckingHash,
                is VirusTotalScanState.Uploading,
                is VirusTotalScanState.WaitingForAnalysis,
                -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(text = virusTotalBusyText(state), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (mode == VirusTotalMode.STRICT) {
                        Text(text = stringResource(R.string.virustotal_strict_waiting), style = MaterialTheme.typography.bodySmall)
                    }
                }

                is VirusTotalScanState.NotFound -> {
                    Text(text = stringResource(R.string.virustotal_not_found), style = MaterialTheme.typography.bodyMedium)
                    Text(text = stringResource(R.string.virustotal_upload_note), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onUpload) {
                        Text(stringResource(R.string.virustotal_upload))
                    }
                }

                is VirusTotalScanState.Completed -> {
                    val stats = state.report.stats
                    val resultColor = when {
                        stats.malicious > 0 -> MaterialTheme.colorScheme.error
                        stats.suspicious > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Text(
                        text = virusTotalResultSummary(stats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = resultColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = virusTotalResultDetail(stats), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { uriHandler.openUri(state.report.guiUrl) }) {
                        Text(stringResource(R.string.virustotal_view_report))
                    }
                }

                is VirusTotalScanState.Failed -> {
                    Text(
                        text = virusTotalErrorText(state.kind),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (mode == VirusTotalMode.STRICT) {
                        Text(text = stringResource(R.string.virustotal_strict_fail_open), style = MaterialTheme.typography.bodySmall)
                    }
                    val canRetry = state.kind !in listOf(
                        VirusTotalErrorKind.FILE_TOO_LARGE,
                        VirusTotalErrorKind.UNSUPPORTED_SOURCE,
                        VirusTotalErrorKind.NO_API_KEY,
                        VirusTotalErrorKind.INTERNET_DISABLED,
                    )
                    if (canRetry) {
                        TextButton(onClick = if (mode == VirusTotalMode.STRICT) onUpload else onLookup) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            state.sha256?.let { sha256 ->
                Text(
                    text = stringResource(R.string.virustotal_sha256_format, sha256),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
