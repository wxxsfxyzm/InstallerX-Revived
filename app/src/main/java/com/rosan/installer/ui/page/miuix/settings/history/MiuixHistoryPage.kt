// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.history.HistoryViewAction
import com.rosan.installer.ui.page.main.settings.history.HistoryViewModel
import com.rosan.installer.ui.page.main.settings.history.formatHistoryTime
import com.rosan.installer.ui.page.main.settings.history.labelRes
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixHistoryPage(
    enableBlur: Boolean,
    viewModel: HistoryViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val topBarBackdrop = rememberMiuixBlurBackdrop(enableBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = title,
                actions = {
                    IconButton(
                        enabled = state.records.isNotEmpty(),
                        onClick = { viewModel.dispatch(HistoryViewAction.ClearHistory) }
                    ) {
                        Icon(
                            imageVector = AppIcons.Delete,
                            contentDescription = stringResource(R.string.history_clear)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        if (state.records.isEmpty()) {
            EmptyHistory(
                modifier = Modifier
                    .fillMaxSize()
                    .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                            layoutDirection
                        ) + 24.dp,
                        top = innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                            layoutDirection
                        ) + 24.dp,
                        bottom = outerPadding.calculateBottomPadding()
                    )
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                        layoutDirection
                    ) + 16.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    end = innerPadding.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                        layoutDirection
                    ) + 16.dp,
                    bottom = outerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.records, key = { it.id }) { record ->
                    HistoryRecordCard(record)
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.history_empty_desc),
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

@Composable
private fun HistoryRecordCard(record: OperationHistoryModel) {
    val statusColor = if (record.status == OperationStatus.SUCCESS) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryPill(text = stringResource(record.operationType.labelRes()))
                HistoryPill(
                    text = stringResource(record.status.labelRes()),
                    color = statusColor
                )
            }
            Text(
                text = record.appLabel ?: record.packageName,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.packageName,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HistoryInfoLine(
                title = stringResource(R.string.history_time),
                value = record.timestamp.formatHistoryTime()
            )
            HistoryInfoLine(
                title = stringResource(R.string.history_version_change),
                value = stringResource(record.versionChange.labelRes())
            )
            HistoryInfoLine(
                title = stringResource(R.string.history_versions),
                value = versionText(record)
            )
            HistoryInfoLine(
                title = stringResource(R.string.history_initiator),
                value = record.initiatorPackageName ?: stringResource(R.string.history_unknown)
            )
            HistoryInfoLine(
                title = stringResource(R.string.history_method),
                value = stringResource(record.installMethod.labelRes())
            )
            HistoryInfoLine(
                title = stringResource(R.string.history_authorizer),
                value = record.authorizer.value
            )
            if (record.status == OperationStatus.FAILED) {
                HistoryInfoLine(
                    title = stringResource(R.string.history_error),
                    value = listOfNotNull(record.errorType, record.errorSummary).joinToString(": ")
                        .ifBlank { stringResource(R.string.history_unknown) }
                )
            }
        }
    }
}

@Composable
private fun HistoryPill(text: String, color: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurface) {
    Card {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = text,
            color = color,
            style = MiuixTheme.textStyles.subtitle,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HistoryInfoLine(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.main,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun versionText(record: OperationHistoryModel): String {
    val oldVersion = record.oldVersionName?.takeIf { it.isNotBlank() }
        ?: record.oldVersionCode?.toString()
        ?: stringResource(R.string.history_none)
    val newVersion = record.newVersionName?.takeIf { it.isNotBlank() }
        ?: record.newVersionCode?.toString()
        ?: stringResource(R.string.history_none)
    return stringResource(R.string.history_version_pair, oldVersion, newVersion)
}
