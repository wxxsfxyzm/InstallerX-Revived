// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.history

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.ui.icons.AppMiuixIcons
import com.rosan.installer.ui.page.main.settings.history.HistoryViewAction
import com.rosan.installer.ui.page.main.settings.history.HistoryViewModel
import com.rosan.installer.ui.page.main.settings.history.formatHistoryTime
import com.rosan.installer.ui.page.main.settings.history.historyAuthorizerText
import com.rosan.installer.ui.page.main.settings.history.labelRes
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

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
    var selectedRecord by remember { mutableStateOf<OperationHistoryModel?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

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
                        onClick = { showClearConfirmDialog = true }
                    ) {
                        Icon(
                            imageVector = AppMiuixIcons.Delete,
                            contentDescription = stringResource(R.string.history_clear)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        if (state.isLoading && state.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = outerPadding.calculateBottomPadding()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfiniteProgressIndicator()
                    Text(
                        text = stringResource(id = R.string.loading),
                        style = MiuixTheme.textStyles.main
                    )
                }
            }
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.records.isEmpty()) {
                    item {
                        EmptyHistory(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = 8.dp)
                        )
                    }
                } else {
                    items(state.records, key = { it.id }) { record ->
                        HistoryRecordBriefCard(
                            record = record,
                            onClick = { selectedRecord = record }
                        )
                    }
                }
            }
        }
    }

    HistoryClearConfirmDialog(
        show = showClearConfirmDialog,
        onDismiss = { showClearConfirmDialog = false },
        onConfirm = {
            showClearConfirmDialog = false
            viewModel.dispatch(HistoryViewAction.ClearHistory)
        }
    )

    selectedRecord?.let { record ->
        WindowBottomSheet(
            show = true,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
            startAction = {
                MiuixBackButton(
                    icon = AppMiuixIcons.Close,
                    iconTint = MiuixTheme.colorScheme.onSurface,
                    onClick = { selectedRecord = null }
                )
            },
            title = record.appLabel ?: record.packageName,
            insideMargin = DpSize(24.dp, 0.dp),
            onDismissRequest = { selectedRecord = null }
        ) {
            HistoryRecordDetailContent(
                record = record,
                isSystemApp = state.isSystemApp,
                modifier = Modifier
                    .fillMaxWidth()
            )
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
private fun HistoryRecordBriefCard(
    record: OperationHistoryModel,
    onClick: () -> Unit
) {
    val statusColor = if (record.status == OperationStatus.SUCCESS) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.appLabel ?: record.packageName,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(record.status.labelRes()),
                    style = MiuixTheme.textStyles.subtitle,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.packageName,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = record.timestamp.formatHistoryTime(),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordDetailContent(
    record: OperationHistoryModel,
    isSystemApp: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_package_name),
                value = record.packageName
            )
        }
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_operation_type),
                value = stringResource(record.operationType.labelRes())
            )
        }
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_time),
                value = record.timestamp.formatHistoryTime()
            )
        }
        if (record.installMethod != InstallMethod.SESSION) {
            item {
                HistoryInfoLine(
                    title = stringResource(R.string.history_version_change),
                    value = stringResource(record.versionChange.labelRes())
                )
            }
            item {
                HistoryInfoLine(
                    title = stringResource(R.string.history_version_name),
                    value = versionNameText(record)
                )
            }
            item {
                HistoryInfoLine(
                    title = stringResource(R.string.history_version_code),
                    value = versionCodeText(record)
                )
            }
        }
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_initiator),
                value = record.initiatorPackageName ?: stringResource(R.string.history_unknown)
            )
        }
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_installer_package),
                value = record.installerPackageName ?: stringResource(R.string.history_unknown)
            )
        }
        if (record.installMethod != InstallMethod.SESSION) {
            item {
                HistoryInfoLine(
                    title = stringResource(R.string.history_apk_path),
                    value = sourcePathText(record)
                )
            }
        }
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_method),
                value = stringResource(record.installMethod.labelRes())
            )
        }
        item {
            HistoryInfoLine(
                title = stringResource(R.string.history_authorizer),
                value = historyAuthorizerText(record.authorizer, isSystemApp)
            )
        }
        if (record.status == OperationStatus.FAILED) {
            item {
                HistoryInfoLine(
                    title = stringResource(R.string.history_error),
                    value = listOfNotNull(record.errorType, record.errorSummary).joinToString(": ")
                        .ifBlank { stringResource(R.string.history_unknown) },
                    valueColor = MiuixTheme.colorScheme.error
                )
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    .padding(
                        bottom = 24.dp +
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                    )
            )
        }
    }
}

@Composable
private fun HistoryInfoLine(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurface
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(value) {
                detectTapGestures(
                    onLongPress = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipData.newPlainText(title, value).toClipEntry())
                        }
                        context.toast(R.string.copied_format, value)
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.main,
            color = valueColor
        )
    }
}

@Composable
private fun versionNameText(record: OperationHistoryModel): String =
    stringResource(
        R.string.history_version_pair,
        record.oldVersionName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.history_none),
        record.newVersionName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.history_none)
    )

@Composable
private fun versionCodeText(record: OperationHistoryModel): String =
    stringResource(
        R.string.history_version_pair,
        record.oldVersionCode?.toString() ?: stringResource(R.string.history_none),
        record.newVersionCode?.toString() ?: stringResource(R.string.history_none)
    )

@Composable
private fun sourcePathText(record: OperationHistoryModel): String =
    record.sourcePaths.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
        ?: stringResource(R.string.history_none)

@Composable
private fun HistoryClearConfirmDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    WindowDialog(
        show = show,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.history_clear_confirm_title),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.history_clear_confirm_desc),
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.clear),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}
