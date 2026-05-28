// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryPage(
    useBlur: Boolean,
    viewModel: HistoryViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val layoutDirection = LocalLayoutDirection.current
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                    ?: TopAppBarDefaults.windowInsets,
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        }
    ) { paddingValues ->
        if (state.records.isEmpty()) {
            EmptyHistory(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                            layoutDirection
                        ) + 24.dp,
                        top = paddingValues.calculateTopPadding(),
                        end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                            layoutDirection
                        ) + 24.dp,
                        bottom = outerPadding.calculateBottomPadding()
                    )
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                contentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                        layoutDirection
                    ) + 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
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
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = stringResource(R.string.history_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HistoryRecordCard(record: OperationHistoryModel) {
    val statusColor = if (record.status == OperationStatus.SUCCESS) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(record.operationType.labelRes())) }
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(record.status.labelRes()),
                            color = statusColor
                        )
                    }
                )
            }
            Text(
                text = record.appLabel ?: record.packageName,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = record.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun HistoryInfoLine(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
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
