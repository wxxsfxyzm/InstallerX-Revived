// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.history

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.sheet.BottomSheet
import com.itos.coui.sheet.CouiSheetButton
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.ui.page.coui.widgets.CouiBadge
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.coui.widgets.CouiTopBarIconButton
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDestructiveActionColors
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial
import com.rosan.installer.ui.page.main.settings.history.HistoryBadgeType
import com.rosan.installer.ui.page.main.settings.history.HistoryViewAction
import com.rosan.installer.ui.page.main.settings.history.HistoryViewModel
import com.rosan.installer.ui.page.main.settings.history.formatHistoryTime
import com.rosan.installer.ui.page.main.settings.history.historyAuthorizerText
import com.rosan.installer.ui.page.main.settings.history.historyBadgeType
import com.rosan.installer.ui.page.main.settings.history.labelRes
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.itos.coui.text.Text as CouiText

private enum class HistoryMenuAction {
    ToggleEnabled,
    ToggleIndicators,
}

@Composable
fun CouiHistoryPage(
    enableBlur: Boolean,
    viewModel: HistoryViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    var showHistoryMenu by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<OperationHistoryModel?>(null) }
    val showRecordDetailSheet = remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDisableConfirmDialog by remember { mutableStateOf(false) }

    val historyEnableText = stringResource(R.string.history_enable)
    val historyShowIndicatorsText = stringResource(R.string.history_show_indicators)
    val historyMenuItems = remember(
        state.isHistoryEnabled,
        state.areIndicatorsEnabled,
        historyEnableText,
        historyShowIndicatorsText,
    ) {
        listOf(
            CouiMenuItem(
                value = HistoryMenuAction.ToggleEnabled,
                text = historyEnableText,
                checked = state.isHistoryEnabled,
            ),
            CouiMenuItem(
                value = HistoryMenuAction.ToggleIndicators,
                text = historyShowIndicatorsText,
                checked = state.areIndicatorsEnabled,
            ),
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = title) },
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CouiTopBarIconButton(
                            enabled = state.isHistoryEnabled && state.records.isNotEmpty(),
                            onClick = { showClearConfirmDialog = true },
                        ) {
                            Icon(
                                imageVector = CouiIcons.Outlined.Delete,
                                contentDescription = stringResource(R.string.history_clear),
                            )
                        }
                        Box {
                            CouiTopBarIconButton(onClick = { showHistoryMenu = true }) {
                                Icon(
                                    imageVector = CouiIcons.Outlined.SliderCustom,
                                    contentDescription = stringResource(R.string.history_recording),
                                )
                            }
                            Menu(
                                expanded = showHistoryMenu,
                                onDismissRequest = { showHistoryMenu = false },
                                items = historyMenuItems,
                                onItemClick = { item ->
                                    when (item.value) {
                                        HistoryMenuAction.ToggleEnabled -> {
                                            if (state.isHistoryEnabled) {
                                                showHistoryMenu = false
                                                showDisableConfirmDialog = true
                                            } else {
                                                viewModel.dispatch(HistoryViewAction.SetHistoryEnabled(enabled = true))
                                            }
                                        }

                                        HistoryMenuAction.ToggleIndicators -> {
                                            viewModel.dispatch(
                                                HistoryViewAction.SetIndicatorsEnabled(enabled = !state.areIndicatorsEnabled),
                                            )
                                        }
                                    }
                                },
                                offset = DpOffset(0.dp, 4.dp),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = enableBlur,
            )
        },
    ) { innerPadding ->
        val contentPadding = remember(scrollBehavior.state, innerPadding, density) {
            scrollBehavior.state.contentPadding(innerPadding, density)
        }
        val bottomPadding = maxOf(
            outerPadding.calculateBottomPadding(),
            contentPadding.calculateBottomPadding(),
        )

        if (state.isLoading && state.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .padding(
                        top = contentPadding.calculateTopPadding(),
                        bottom = bottomPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(
                        color = CouiTheme.colorScheme.accent,
                        strokeWidth = 3.dp,
                    )
                    Text(
                        text = stringResource(id = R.string.loading),
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                contentPadding = PaddingValues(
                    start = CouiPageDefaults.horizontalMargin,
                    end = CouiPageDefaults.horizontalMargin,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = bottomPadding + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!state.isHistoryEnabled || state.records.isEmpty()) {
                    item {
                        CouiEmptyHistory(
                            isHistoryEnabled = state.isHistoryEnabled,
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = 8.dp),
                        )
                    }
                } else {
                    items(state.records, key = { it.id }) { record ->
                        CouiHistoryRecordBriefCard(
                            record = record,
                            showIndicator = state.areIndicatorsEnabled,
                            onClick = {
                                selectedRecord = record
                                showRecordDetailSheet.value = true
                            },
                        )
                    }
                }
            }
        }
    }

    val backdrop = rememberCouiWindowBackdrop()
    val material = rememberCouiDialogMaterial()
    val destructiveColors = rememberCouiDestructiveActionColors()

    Dialog(
        visible = showClearConfirmDialog,
        onDismissRequest = { showClearConfirmDialog = false },
        title = stringResource(R.string.history_clear_confirm_title),
        message = stringResource(R.string.history_clear_confirm_desc),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.clear),
            onClick = {
                showClearConfirmDialog = false
                viewModel.dispatch(HistoryViewAction.ClearHistory)
            },
            style = CouiDialogActionStyle.Primary,
            colors = destructiveColors,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { showClearConfirmDialog = false },
        ),
        material = material,
        backdrop = backdrop,
    )

    Dialog(
        visible = showDisableConfirmDialog,
        onDismissRequest = { showDisableConfirmDialog = false },
        title = stringResource(R.string.history_disable_confirm_title),
        message = stringResource(R.string.history_disable_confirm_desc),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.history_disable_and_clear),
            onClick = {
                showDisableConfirmDialog = false
                viewModel.dispatch(HistoryViewAction.SetHistoryEnabled(enabled = false, clearHistory = true))
            },
            style = CouiDialogActionStyle.Primary,
            colors = destructiveColors,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.history_disable_and_keep),
            onClick = {
                showDisableConfirmDialog = false
                viewModel.dispatch(HistoryViewAction.SetHistoryEnabled(enabled = false))
            },
        ),
        material = material,
        backdrop = backdrop,
    )

    BottomSheet(
        visible = showRecordDetailSheet,
        onDismissRequest = { showRecordDetailSheet.value = false },
        scrollableContent = false,
        leadingButton = CouiSheetButton(
            onClick = { showRecordDetailSheet.value = false },
            icon = {
                Icon(
                    imageVector = CouiIcons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                )
            },
        ),
        titleContent = {
            CouiText(
                text = selectedRecord?.let { it.appLabel ?: it.packageName } ?: "",
                kind = CouiTextKind.SheetTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    ) {
        selectedRecord?.let { record ->
            CouiHistoryRecordDetailContent(
                record = record,
                isSystemApp = state.isSystemApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun CouiEmptyHistory(isHistoryEnabled: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                if (isHistoryEnabled) R.string.history_empty_title else R.string.history_disabled_title,
            ),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = CouiTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isHistoryEnabled) R.string.history_empty_desc else R.string.history_disabled_desc,
            ),
            fontSize = 14.sp,
            color = CouiTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CouiHistoryRecordBriefCard(
    record: OperationHistoryModel,
    showIndicator: Boolean,
    onClick: () -> Unit,
) {
    val dark = CouiTheme.isDark
    val scheme = CouiTheme.colorScheme
    val errorColor = if (dark) Color(0xFFFF6C61) else Color(0xFFDB382C)
    val statusColor = if (record.status == OperationStatus.SUCCESS) scheme.accentOnSurface else errorColor
    val badgeType = record.historyBadgeType(LocalContext.current.packageName)
    val badgeColor = if (badgeType == HistoryBadgeType.DOWNGRADE) errorColor else scheme.accentOnSurface
    val source = remember { MutableInteractionSource() }

    CouiCard(
        modifier = Modifier
            .fillMaxWidth()
            .couiButtonFeedback(dark = dark, interactionSource = source)
            .clickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = record.appLabel ?: record.packageName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showIndicator && badgeType != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CouiBadge(
                            text = stringResource(badgeType.labelRes()),
                            textColor = badgeColor,
                            containerColor = badgeColor.copy(alpha = 0.16f),
                        )
                    }
                }
                Text(
                    text = stringResource(record.status.labelRes()),
                    fontSize = 13.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = record.packageName,
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = record.timestamp.formatHistoryTime(),
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CouiHistoryRecordDetailContent(
    record: OperationHistoryModel,
    isSystemApp: Boolean,
    modifier: Modifier = Modifier,
) {
    val errorColor = if (CouiTheme.isDark) Color(0xFFFF6C61) else Color(0xFFDB382C)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_package_name),
                value = record.packageName,
            )
        }
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_operation_type),
                value = stringResource(record.operationType.labelRes()),
            )
        }
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_time),
                value = record.timestamp.formatHistoryTime(),
            )
        }
        if (record.installMethod != InstallMethod.SESSION) {
            item {
                CouiHistoryInfoLine(
                    title = stringResource(R.string.history_version_change),
                    value = stringResource(record.versionChange.labelRes()),
                )
            }
            item {
                CouiHistoryInfoLine(
                    title = stringResource(R.string.history_version_name),
                    value = couiVersionNameText(record),
                )
            }
            item {
                CouiHistoryInfoLine(
                    title = stringResource(R.string.history_version_code),
                    value = couiVersionCodeText(record),
                )
            }
        }
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_initiator),
                value = record.initiatorPackageName ?: stringResource(R.string.history_unknown),
            )
        }
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_installer_package),
                value = record.installerPackageName ?: stringResource(R.string.history_unknown),
            )
        }
        if (record.installMethod != InstallMethod.SESSION) {
            item {
                CouiHistoryInfoLine(
                    title = stringResource(R.string.history_apk_path),
                    value = couiSourcePathText(record),
                )
            }
        }
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_method),
                value = stringResource(record.installMethod.labelRes()),
            )
        }
        item {
            CouiHistoryInfoLine(
                title = stringResource(R.string.history_authorizer),
                value = historyAuthorizerText(record.authorizer, isSystemApp),
            )
        }
        if (record.status == OperationStatus.FAILED) {
            item {
                CouiHistoryInfoLine(
                    title = stringResource(R.string.history_error),
                    value = listOfNotNull(record.errorType, record.errorSummary).joinToString(": ")
                        .ifBlank { stringResource(R.string.history_unknown) },
                    valueColor = errorColor,
                )
            }
        }
    }
}

@Composable
private fun CouiHistoryInfoLine(
    title: String,
    value: String,
    valueColor: Color = CouiTheme.colorScheme.onSurface,
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
                    },
                )
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CouiText(
            text = title,
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurfaceVariant,
        )
        CouiText(
            text = value,
            kind = CouiTextKind.ItemTitle,
            color = valueColor,
        )
    }
}

@Composable
private fun couiVersionNameText(record: OperationHistoryModel): String = stringResource(
    R.string.history_version_pair,
    record.oldVersionName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.history_none),
    record.newVersionName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.history_none),
)

@Composable
private fun couiVersionCodeText(record: OperationHistoryModel): String = stringResource(
    R.string.history_version_pair,
    record.oldVersionCode?.toString() ?: stringResource(R.string.history_none),
    record.newVersionCode?.toString() ?: stringResource(R.string.history_none),
)

@Composable
private fun couiSourcePathText(record: OperationHistoryModel): String =
    record.sourcePaths.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
        ?: stringResource(R.string.history_none)
