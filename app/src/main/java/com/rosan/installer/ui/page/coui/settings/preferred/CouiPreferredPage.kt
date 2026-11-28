// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Item
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupValidationIssue
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiErrorDisplaySheet
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.util.readBackupText
import com.rosan.installer.ui.util.writeBackupText
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.itos.coui.text.Text as CouiText

@Composable
fun CouiPreferredPage(
    enableBlur: Boolean,
    viewModel: PreferredViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus)
    }

    val revLevel = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    var errorDialogInfo by remember {
        mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(null)
    }
    val showErrorSheetState = remember { mutableStateOf(false) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingRestorePreview by remember { mutableStateOf<BackupRestorePreview?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showHideLauncherIconDialog by remember { mutableStateOf(false) }
    var backupValidationErrorText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
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
                snackbarHostState.showSnackbar(context.getString(R.string.backup_settings_export_success))
            }.onFailure {
                snackbarHostState.showSnackbar(context.getString(R.string.backup_settings_file_write_failed))
            }
            pendingExportContent = null
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                context.readBackupText(uri)
            }.onSuccess { content ->
                viewModel.dispatch(PreferredViewAction.PrepareRestoreBackup(content))
            }.onFailure {
                snackbarHostState.showSnackbar(context.getString(R.string.backup_settings_file_read_failed))
            }
        }
    }

    val defaultInstallerErrorDetailActionLabel = stringResource(R.string.details)
    @SuppressLint("LocalContextGetResourceValueCall")
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PreferredViewEvent.ShowDefaultInstallerResult -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is PreferredViewEvent.ShowDefaultInstallerErrorDetail -> {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = context.getString(event.titleResId),
                        actionLabel = defaultInstallerErrorDetailActionLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        errorDialogInfo = event
                        showErrorSheetState.value = true
                    }
                }

                is PreferredViewEvent.LaunchBackupExport -> {
                    pendingExportContent = event.content
                    exportLauncher.launch(event.fileName)
                }

                is PreferredViewEvent.ShowBackupMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is PreferredViewEvent.ShowBackupError -> {
                    snackbarHostState.showSnackbar(context.getString(event.titleResId))
                }

                is PreferredViewEvent.ShowBackupRestorePreview -> {
                    pendingRestorePreview = event.preview
                    showRestoreConfirmDialog = true
                }

                is PreferredViewEvent.ShowBackupValidationError -> {
                    backupValidationErrorText = event.issues.formatCouiBackupValidationIssues(context)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = title) },
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = bottomPadding + 16.dp,
            ),
        ) {
            item {
                Group(title = stringResource(R.string.personalization)) {
                    CouiNavigationItem(
                        icon = AppIcons.Theme,
                        title = stringResource(R.string.theme_settings),
                        description = stringResource(R.string.theme_settings_desc),
                        onClick = { navigator.push(Route.Theme) },
                    )
                    Divider()
                    CouiNavigationItem(
                        icon = AppIcons.InstallMode,
                        title = stringResource(R.string.installer_settings),
                        description = stringResource(R.string.installer_settings_desc),
                        onClick = { navigator.push(Route.InstallerGlobal) },
                    )
                    Divider()
                    CouiNavigationItem(
                        icon = AppIcons.InstallMode,
                        title = stringResource(R.string.uninstaller_settings),
                        description = stringResource(R.string.uninstaller_settings_desc),
                        onClick = { navigator.push(Route.UninstallerGlobal) },
                    )
                }
            }

            if (uiState.authorizer == Authorizer.None) {
                item {
                    val tip = if (capabilityProvider.isSystemApp) {
                        stringResource(R.string.config_authorizer_none_system_app_tips)
                    } else {
                        stringResource(R.string.config_authorizer_none_tips)
                    }
                    CouiSettingsTipCard(text = tip)
                }
            }

            item {
                Group(title = stringResource(R.string.basic)) {
                    CouiNavigationItem(
                        title = stringResource(R.string.network_settings),
                        description = stringResource(
                            if (uiState.allowInternetAccess) {
                                R.string.network_settings_internet_access_enabled_desc
                            } else {
                                R.string.network_settings_internet_access_disabled_desc
                            },
                        ),
                        onClick = { navigator.push(Route.Network) },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.disable_adb_install_verify),
                        description = if (uiState.authorizer != Authorizer.Dhizuku) {
                            stringResource(R.string.disable_adb_install_verify_desc)
                        } else {
                            stringResource(R.string.disable_adb_install_verify_not_support_dhizuku_desc)
                        },
                        checked = !uiState.adbVerifyEnabled,
                        enabled = uiState.authorizer != Authorizer.Dhizuku &&
                            uiState.authorizer != Authorizer.None,
                        onCheckedChange = { isDisabled ->
                            viewModel.dispatch(PreferredViewAction.SetAdbVerifyEnabledState(!isDisabled))
                        },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.ignore_battery_optimizations),
                        description = if (!uiState.isIgnoringBatteryOptimizations) {
                            stringResource(R.string.ignore_battery_optimizations_desc)
                        } else {
                            stringResource(R.string.ignore_battery_optimizations_desc_disabled)
                        },
                        checked = uiState.isIgnoringBatteryOptimizations,
                        enabled = !uiState.isIgnoringBatteryOptimizations,
                        onCheckedChange = { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) },
                    )
                    Divider()
                    CouiSwitchItem(
                        title = stringResource(R.string.theme_settings_hide_launcher_icon),
                        description = stringResource(R.string.theme_settings_hide_launcher_icon_desc),
                        checked = !uiState.showLauncherIcon,
                        onCheckedChange = { newCheckedState ->
                            if (newCheckedState) {
                                showHideLauncherIconDialog = true
                            } else {
                                viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true))
                            }
                        },
                    )
                }
            }

            item {
                Group(title = stringResource(R.string.backup_settings)) {
                    CouiBasicItem(
                        title = stringResource(R.string.backup_settings_export),
                        summary = stringResource(R.string.backup_settings_export_desc),
                        enabled = !uiState.backupBusy,
                        onClick = { viewModel.dispatch(PreferredViewAction.RequestExportBackup) },
                    )
                    Divider()
                    CouiBasicItem(
                        title = stringResource(R.string.backup_settings_restore),
                        summary = stringResource(R.string.backup_settings_restore_desc),
                        enabled = !uiState.backupBusy,
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
                    )
                }
            }

            item {
                Group(title = stringResource(R.string.other)) {
                    CouiNavigationItem(
                        title = stringResource(R.string.lab),
                        description = stringResource(R.string.lab_desc),
                        onClick = { navigator.push(Route.Lab) },
                    )
                    Divider()
                    Item(
                        summary = {
                            CouiText(
                                text = if (uiState.hasUpdate) {
                                    stringResource(R.string.update_available, uiState.remoteVersion)
                                } else {
                                    "$revLevel ${AppConfig.VERSION_NAME}"
                                },
                                kind = CouiTextKind.ItemSummary,
                                color = if (uiState.hasUpdate) {
                                    CouiTheme.colorScheme.accentOnSurface
                                } else {
                                    CouiTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        onClick = { navigator.push(Route.About) },
                    ) {
                        CouiText(
                            text = stringResource(R.string.about_detail),
                            kind = CouiTextKind.ItemTitle,
                            color = CouiTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    val currentError = errorDialogInfo
    CouiErrorDisplaySheet(
        showState = showErrorSheetState,
        exception = currentError?.exception ?: RuntimeException(),
        onDismissRequest = { showErrorSheetState.value = false },
        onRetry = currentError?.retryAction?.let { retryAction ->
            {
                showErrorSheetState.value = false
                viewModel.dispatch(retryAction)
            }
        },
        title = currentError?.let { stringResource(it.titleResId) } ?: stringResource(R.string.installer_unknown_error),
    )

    val backdrop = rememberCouiWindowBackdrop()
    val dialogMaterial = rememberCouiDialogMaterial()

    Dialog(
        visible = showRestoreConfirmDialog,
        onDismissRequest = { showRestoreConfirmDialog = false },
        title = stringResource(R.string.backup_settings_restore_confirm_title),
        message = pendingRestorePreview?.formatCouiBackupRestorePreview(context)
            ?: stringResource(R.string.backup_settings_restore_confirm_desc),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                viewModel.dispatch(PreferredViewAction.ConfirmRestoreBackup)
                showRestoreConfirmDialog = false
            },
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { showRestoreConfirmDialog = false },
        ),
        material = dialogMaterial,
        backdrop = backdrop,
    )

    Dialog(
        visible = backupValidationErrorText != null,
        onDismissRequest = { backupValidationErrorText = null },
        title = stringResource(R.string.backup_settings_validation_failed_title),
        message = backupValidationErrorText.orEmpty(),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = { backupValidationErrorText = null },
            style = CouiDialogActionStyle.Primary,
        ),
        material = dialogMaterial,
        backdrop = backdrop,
    )

    val hideLauncherMessage = buildString {
        append(stringResource(R.string.theme_settings_hide_launcher_icon_warning))
        if (DeviceConfig.currentManufacturer == Manufacturer.XIAOMI) {
            append("\n")
            append(stringResource(R.string.theme_settings_hide_launcher_icon_warning_xiaomi))
        }
    }

    Dialog(
        visible = showHideLauncherIconDialog,
        onDismissRequest = { showHideLauncherIconDialog = false },
        title = stringResource(R.string.warning),
        message = hideLauncherMessage,
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                showHideLauncherIconDialog = false
                viewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(false))
            },
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { showHideLauncherIconDialog = false },
        ),
        material = dialogMaterial,
        backdrop = backdrop,
    )
}

private fun BackupRestorePreview.formatCouiBackupRestorePreview(context: Context): String = buildString {
    append(
        context.getString(
            R.string.backup_settings_restore_preview_desc,
            profileCount,
            scopeCount,
            settingCount,
            historyCount,
        ),
    )
    if (ignoredSettingCount > 0) {
        append("\n")
        append(context.getString(R.string.backup_settings_restore_ignored_settings, ignoredSettingCount))
    }
    if (warnings.isNotEmpty()) {
        append("\n\n")
        append(context.getString(R.string.backup_settings_restore_warnings_title))
        append("\n")
        append(warnings.formatCouiBackupValidationIssues(context))
    }
}

private fun List<BackupValidationIssue>.formatCouiBackupValidationIssues(context: Context): String =
    joinToString(separator = "\n") { issue ->
        context.getString(issue.messageResId, *issue.args.toTypedArray())
    }
