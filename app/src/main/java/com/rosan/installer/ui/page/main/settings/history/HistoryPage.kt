// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.history

import android.content.ClipData
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.theme.bottomShape
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.middleShape
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.ui.theme.singleShape
import com.rosan.installer.ui.theme.topShape
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    useBlur: Boolean,
    viewModel: HistoryViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val layoutDirection = LocalLayoutDirection.current
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    var selectedRecord by remember { mutableStateOf<OperationHistoryModel?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    IconButton(
                        enabled = state.records.isNotEmpty(),
                        onClick = { showClearConfirmDialog = true }
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
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = outerPadding.calculateBottomPadding()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                        Text(
                            text = stringResource(id = R.string.loading),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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
                        itemsIndexed(
                            items = state.records,
                            key = { _, record -> record.id },
                            contentType = { _, _ -> "history_record" }
                        ) { index, record ->
                            val shape = when {
                                state.records.size == 1 -> singleShape
                                index == 0 -> topShape
                                index == state.records.lastIndex -> bottomShape
                                else -> middleShape
                            }

                            HistoryRecordBriefCard(
                                record = record,
                                shape = shape,
                                onClick = { selectedRecord = record }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.dispatch(HistoryViewAction.ClearHistory)
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (selectedRecord != null) {
        ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            onDismissRequest = { selectedRecord = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentWindowInsets = {
                WindowInsets(
                    left = 0.dp,
                    top = 0.dp,
                    right = 0.dp,
                    bottom = 0.dp
                )
            }
        ) {
            HistoryRecordDetailContent(
                record = selectedRecord!!,
                isSystemApp = state.isSystemApp,
                modifier = Modifier.fillMaxWidth()
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
private fun HistoryRecordBriefCard(
    record: OperationHistoryModel,
    shape: Shape,
    onClick: () -> Unit
) {
    val statusColor = if (record.status == OperationStatus.SUCCESS) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        shape = shape
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
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(record.status.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = record.timestamp.formatHistoryTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryRecordDetailContent(
    record: OperationHistoryModel,
    isSystemApp: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 24.dp,
            top = 16.dp,
            end = 24.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = record.appLabel ?: record.packageName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryInfoLine(
                    title = stringResource(R.string.history_package_name),
                    value = record.packageName
                )
                HistoryInfoLine(
                    title = stringResource(R.string.history_operation_type),
                    value = stringResource(record.operationType.labelRes())
                )
                HistoryInfoLine(
                    title = stringResource(R.string.history_time),
                    value = record.timestamp.formatHistoryTime()
                )

                if (record.installMethod != InstallMethod.SESSION) {
                    HistoryInfoLine(
                        title = stringResource(R.string.history_version_change),
                        value = stringResource(record.versionChange.labelRes())
                    )
                    HistoryInfoLine(
                        title = stringResource(R.string.history_version_name),
                        value = versionNameText(record)
                    )
                    HistoryInfoLine(
                        title = stringResource(R.string.history_version_code),
                        value = versionCodeText(record)
                    )
                }

                HistoryInfoLine(
                    title = stringResource(R.string.history_initiator),
                    value = record.initiatorPackageName ?: stringResource(R.string.history_unknown)
                )
                HistoryInfoLine(
                    title = stringResource(R.string.history_installer_package),
                    value = record.installerPackageName ?: stringResource(R.string.history_unknown)
                )

                if (record.installMethod != InstallMethod.SESSION) {
                    HistoryInfoLine(
                        title = stringResource(R.string.history_apk_path),
                        value = sourcePathText(record)
                    )
                }

                HistoryInfoLine(
                    title = stringResource(R.string.history_method),
                    value = stringResource(record.installMethod.labelRes())
                )
                HistoryInfoLine(
                    title = stringResource(R.string.history_authorizer),
                    value = historyAuthorizerText(record.authorizer, isSystemApp)
                )

                if (record.status == OperationStatus.FAILED) {
                    HistoryInfoLine(
                        title = stringResource(R.string.history_error),
                        value = listOfNotNull(record.errorType, record.errorSummary)
                            .joinToString(": ")
                            .ifBlank { stringResource(R.string.history_unknown) },
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun historyAuthorizerText(authorizer: Authorizer, isSystemApp: Boolean): String =
    if (authorizer == Authorizer.None && isSystemApp) {
        stringResource(R.string.working_status_system_installer)
    } else {
        stringResource(authorizer.displayNameRes)
    }

@Composable
private fun HistoryInfoLine(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
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
                            clipboard.setClipEntry(
                                ClipData.newPlainText(title, value).toClipEntry()
                            )
                        }
                        context.toast(R.string.copied_format, value)
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
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
