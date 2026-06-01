// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package com.rosan.installer.ui.page.main.settings.preferred

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupValidationIssue
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.NavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.snackbar.SwipeableSnackbarHost
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.ui.util.readBackupText
import com.rosan.installer.ui.util.writeBackupText
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferredPage(
    useBlur: Boolean,
    viewModel: PreferredViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus)
    }

    val revLevel = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    val snackBarHostState = remember { SnackbarHostState() }
    var errorDialogInfo by remember {
        mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(null)
    }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingRestorePreview by remember { mutableStateOf<BackupRestorePreview?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var backupValidationErrorText by remember { mutableStateOf<String?>(null) }

    val detailLabel = stringResource(id = R.string.details)
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingExportContent ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            pendingExportContent = null
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            runCatching {
                context.writeBackupText(uri, content)
            }.onSuccess {
                snackBarHostState.showSnackbar(context.getString(R.string.backup_settings_export_success))
            }.onFailure {
                snackBarHostState.showSnackbar(context.getString(R.string.backup_settings_file_write_failed))
            }
            pendingExportContent = null
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                context.readBackupText(uri)
            }.onSuccess { content ->
                viewModel.dispatch(PreferredViewAction.PrepareRestoreBackup(content))
            }.onFailure {
                snackBarHostState.showSnackbar(context.getString(R.string.backup_settings_file_read_failed))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            snackBarHostState.currentSnackbarData?.dismiss()
            when (event) {
                is PreferredViewEvent.ShowDefaultInstallerResult -> {
                    snackBarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is PreferredViewEvent.ShowDefaultInstallerErrorDetail -> {
                    val snackbarResult = snackBarHostState.showSnackbar(
                        message = context.getString(event.titleResId),
                        actionLabel = detailLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        errorDialogInfo = event
                    }
                }

                is PreferredViewEvent.LaunchBackupExport -> {
                    pendingExportContent = event.content
                    exportLauncher.launch(event.fileName)
                }

                is PreferredViewEvent.ShowBackupMessage -> {
                    snackBarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is PreferredViewEvent.ShowBackupError -> {
                    snackBarHostState.showSnackbar(context.getString(event.titleResId))
                }

                is PreferredViewEvent.ShowBackupRestorePreview -> {
                    pendingRestorePreview = event.preview
                    showRestoreConfirmDialog = true
                }

                is PreferredViewEvent.ShowBackupValidationError -> {
                    backupValidationErrorText = event.issues.formatBackupValidationIssues(context)
                }
            }
        }
    }

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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding()),
                hostState = snackBarHostState
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                    layoutDirection
                ),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                    layoutDirection
                ),
                bottom = outerPadding.calculateBottomPadding()
            )
        ) {
            // --- Global Settings Group ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.personalization)
                ) {
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.Theme,
                            title = stringResource(R.string.theme_settings),
                            description = stringResource(R.string.theme_settings_desc),
                            onClick = {
                                navigator.push(Route.Theme)
                            }
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.InstallMode,
                            title = stringResource(R.string.installer_settings),
                            description = stringResource(R.string.installer_settings_desc),
                            onClick = {
                                navigator.push(Route.InstallerGlobal)
                            }
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.Delete,
                            title = stringResource(R.string.uninstaller_settings),
                            description = stringResource(R.string.uninstaller_settings_desc),
                            onClick = {
                                navigator.push(Route.UninstallerGlobal)
                            }
                        )
                    }
                }
            }

            // --- Basic Settings Group ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.basic)
                ) {
                    item {
                        val isError = uiState.authorizer == Authorizer.Dhizuku
                        SwitchWidget(
                            icon = AppIcons.DisableAdbVerify,
                            title = stringResource(R.string.disable_adb_install_verify),
                            description = if (!isError) stringResource(R.string.disable_adb_install_verify_desc)
                            else stringResource(R.string.disable_adb_install_verify_not_support_dhizuku_desc),
                            checked = !uiState.adbVerifyEnabled,
                            isError = isError,
                            enabled = uiState.authorizer != Authorizer.Dhizuku &&
                                    uiState.authorizer != Authorizer.None
                        ) { viewModel.dispatch(PreferredViewAction.SetAdbVerifyEnabledState(!it)) }
                    }
                    item {
                        val enabled = !uiState.isIgnoringBatteryOptimizations
                        SwitchWidget(
                            icon = AppIcons.BatteryOptimization,
                            title = stringResource(R.string.ignore_battery_optimizations),
                            description = if (enabled) stringResource(R.string.ignore_battery_optimizations_desc)
                            else stringResource(R.string.ignore_battery_optimizations_desc_disabled),
                            checked = uiState.isIgnoringBatteryOptimizations,
                            enabled = enabled,
                        ) { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) }
                    }
                }
            }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.backup_settings)
                ) {
                    item {
                        BaseWidget(
                            icon = AppIcons.Save,
                            title = stringResource(R.string.backup_settings_export),
                            description = stringResource(R.string.backup_settings_export_desc),
                            enabled = !uiState.backupBusy,
                            onClick = { viewModel.dispatch(PreferredViewAction.RequestExportBackup) }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = AppIcons.Download,
                            title = stringResource(R.string.backup_settings_restore),
                            description = stringResource(R.string.backup_settings_restore_desc),
                            enabled = !uiState.backupBusy,
                            onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "*/*")) }
                        )
                    }
                }
            }
            // --- Other Settings Group ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.other)
                ) {
                    item {
                        BaseWidget(
                            icon = AppIcons.Lab,
                            title = stringResource(R.string.lab),
                            description = stringResource(R.string.lab_desc),
                            onClick = { navigator.push(Route.Lab) }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = AppIcons.Info,
                            title = stringResource(R.string.about_detail),
                            description = if (uiState.hasUpdate) stringResource(
                                R.string.update_available,
                                uiState.remoteVersion
                            ) else "$revLevel ${AppConfig.VERSION_NAME}",
                            descriptionColor = if (uiState.hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { navigator.push(Route.About) }
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    errorDialogInfo?.let { dialogInfo ->
        ErrorDisplayDialog(
            exception = dialogInfo.exception,
            onDismissRequest = { errorDialogInfo = null },
            onRetry = {
                errorDialogInfo = null
                viewModel.dispatch(dialogInfo.retryAction)
            },
            title = stringResource(dialogInfo.titleResId)
        )
    }

    if (showRestoreConfirmDialog) {
        val preview = pendingRestorePreview
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestorePreview = null
            },
            title = { Text(stringResource(R.string.backup_settings_restore_confirm_title)) },
            text = {
                Text(
                    preview?.formatBackupRestorePreview(context)
                        ?: stringResource(R.string.backup_settings_restore_confirm_desc)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dispatch(PreferredViewAction.ConfirmRestoreBackup)
                        pendingRestorePreview = null
                        showRestoreConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestorePreview = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    backupValidationErrorText?.let { errorText ->
        AlertDialog(
            onDismissRequest = { backupValidationErrorText = null },
            title = { Text(stringResource(R.string.backup_settings_validation_failed_title)) },
            text = { Text(errorText) },
            confirmButton = {
                TextButton(onClick = { backupValidationErrorText = null }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}

private fun BackupRestorePreview.formatBackupRestorePreview(context: Context): String =
    buildString {
        append(
            context.getString(
                R.string.backup_settings_restore_preview_desc,
                profileCount,
                scopeCount,
                settingCount,
                historyCount
            )
        )
        if (ignoredSettingCount > 0) {
            append("\n")
            append(context.getString(R.string.backup_settings_restore_ignored_settings, ignoredSettingCount))
        }
        if (warnings.isNotEmpty()) {
            append("\n\n")
            append(context.getString(R.string.backup_settings_restore_warnings_title))
            append("\n")
            append(warnings.formatBackupValidationIssues(context))
        }
    }

private fun List<BackupValidationIssue>.formatBackupValidationIssues(
    context: Context
): String =
    joinToString(separator = "\n") { issue ->
        context.getString(issue.messageResId, *issue.args.toTypedArray())
    }
