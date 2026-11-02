// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.installer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.rosan.installer.ui.theme.miuixSheetCardColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixVirusTotalSection(
    state: VirusTotalScanState,
    mode: VirusTotalMode,
    onLookup: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = miuixSheetCardColors(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.virustotal_title),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
            )
            when (state) {
                is VirusTotalScanState.Idle -> {
                    if (mode == VirusTotalMode.STRICT) {
                        Text(text = stringResource(R.string.virustotal_strict_waiting), style = MiuixTheme.textStyles.body2)
                    } else {
                        Text(text = stringResource(R.string.virustotal_not_scanned), style = MiuixTheme.textStyles.body2)
                        TextButton(
                            onClick = onLookup,
                            text = stringResource(R.string.virustotal_check_hash),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is VirusTotalScanState.Hashing,
                is VirusTotalScanState.CheckingHash,
                is VirusTotalScanState.Uploading,
                is VirusTotalScanState.WaitingForAnalysis,
                -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfiniteProgressIndicator()
                        Text(text = virusTotalBusyText(state), style = MiuixTheme.textStyles.body2)
                    }
                    if (mode == VirusTotalMode.STRICT) {
                        Text(text = stringResource(R.string.virustotal_strict_waiting), style = MiuixTheme.textStyles.body2)
                    }
                }

                is VirusTotalScanState.NotFound -> {
                    Text(text = stringResource(R.string.virustotal_not_found), style = MiuixTheme.textStyles.body2)
                    Text(text = stringResource(R.string.virustotal_upload_note), style = MiuixTheme.textStyles.body2)
                    TextButton(
                        onClick = onUpload,
                        text = stringResource(R.string.virustotal_upload),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is VirusTotalScanState.Completed -> {
                    val stats = state.report.stats
                    val resultColor = if (stats.malicious > 0) {
                        MiuixTheme.colorScheme.error
                    } else {
                        MiuixTheme.colorScheme.primary
                    }
                    Text(
                        text = virusTotalResultSummary(stats),
                        style = MiuixTheme.textStyles.body2,
                        color = resultColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = virusTotalResultDetail(stats), style = MiuixTheme.textStyles.body2)
                    TextButton(
                        onClick = { uriHandler.openUri(state.report.guiUrl) },
                        text = stringResource(R.string.virustotal_view_report),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is VirusTotalScanState.Failed -> {
                    Text(
                        text = virusTotalErrorText(state.kind),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                    if (mode == VirusTotalMode.STRICT) {
                        Text(text = stringResource(R.string.virustotal_strict_fail_open), style = MiuixTheme.textStyles.body2)
                    }
                    val canRetry = state.kind !in listOf(
                        VirusTotalErrorKind.FILE_TOO_LARGE,
                        VirusTotalErrorKind.UNSUPPORTED_SOURCE,
                        VirusTotalErrorKind.NO_API_KEY,
                        VirusTotalErrorKind.INTERNET_DISABLED,
                    )
                    if (canRetry) {
                        TextButton(
                            onClick = if (mode == VirusTotalMode.STRICT) onUpload else onLookup,
                            text = stringResource(R.string.retry),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            state.sha256?.let { sha256 ->
                Text(
                    text = stringResource(R.string.virustotal_sha256_format, sha256),
                    style = MiuixTheme.textStyles.body2,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
