// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.backup.BackupRestorePreview
import com.rosan.installer.domain.settings.model.backup.BackupValidationIssue
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.page.miuix.widgets.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import com.rosan.installer.ui.util.readBackupText
import com.rosan.installer.ui.util.writeBackupText
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog


@Composable
fun MiuixPreferredPage(
    enableBlur: Boolean,
    viewModel: PreferredViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus)
    }

    val revLevel = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }
    val scrollBehavior = MiuixScrollBehavior()

    var errorDialogInfo by remember {
        mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(
            null
        )
    }
    val showErrorSheetState = remember { mutableStateOf(false) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingRestorePreview by remember { mutableStateOf<BackupRestorePreview?>(null) }
    val showRestoreConfirmDialog = remember { mutableStateOf(false) }
    var backupValidationErrorText by remember { mutableStateOf<String?>(null) }
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
                snackbarHostState.showSnackbar(context.getString(R.string.backup_settings_export_success))
            }.onFailure {
                snackbarHostState.showSnackbar(context.getString(R.string.backup_settings_file_write_failed))
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
                snackbarHostState.showSnackbar(context.getString(R.string.backup_settings_file_read_failed))
            }
        }
    }

    val defaultInstallerErrorDetailActionLabel = stringResource(R.string.details)
    @SuppressLint("LocalContextGetResourceValueCall") LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            //snackBarHostState.newestSnackbarData()?.dismiss()
            when (event) {
                is PreferredViewEvent.ShowDefaultInstallerResult -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is PreferredViewEvent.ShowDefaultInstallerErrorDetail -> {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = context.getString(event.titleResId),
                        actionLabel = defaultInstallerErrorDetailActionLabel,
                        duration = SnackbarDuration.Short
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
                    showRestoreConfirmDialog.value = true
                }

                is PreferredViewEvent.ShowBackupValidationError -> {
                    backupValidationErrorText = event.issues.formatBackupValidationIssues(context)
                }
            }
        }
    }

    // Capture layout direction and horizontal safe insets for display cutouts in landscape
    val layoutDirection = LocalLayoutDirection.current

    val topBarBackdrop = rememberMiuixBlurBackdrop(enableBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = title,
                scrollBehavior = scrollBehavior
            )

        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                // Safely add horizontal paddings to avoid notches in landscape
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = outerPadding.calculateBottomPadding()
            ),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.personalization)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixNavigationItemWidget(
                        icon = AppIcons.Theme,
                        title = stringResource(R.string.theme_settings),
                        description = stringResource(R.string.theme_settings_desc),
                        onClick = {
                            navigator.push(Route.Theme)
                        }
                    )
                    MiuixNavigationItemWidget(
                        icon = AppIcons.InstallMode,
                        title = stringResource(R.string.installer_settings),
                        description = stringResource(R.string.installer_settings_desc),
                        onClick = {
                            navigator.push(Route.InstallerGlobal)
                        }
                    )
                    MiuixNavigationItemWidget(
                        icon = AppIcons.InstallMode,
                        title = stringResource(R.string.uninstaller_settings),
                        description = stringResource(R.string.uninstaller_settings_desc),
                        onClick = {
                            navigator.push(Route.UninstallerGlobal)
                        }
                    )
                }
            }
            if (uiState.authorizer == Authorizer.None)
                item {
                    val tip =
                        if (capabilityProvider.isSystemApp) stringResource(R.string.config_authorizer_none_system_app_tips)
                        else stringResource(R.string.config_authorizer_none_tips)
                    MiuixSettingsTipCard(text = tip)
                }
            item { SmallTitle(stringResource(R.string.basic)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixDisableAdbVerify(
                        checked = !uiState.adbVerifyEnabled,
                        isError = uiState.authorizer == Authorizer.Dhizuku,
                        enabled = uiState.authorizer != Authorizer.Dhizuku &&
                                uiState.authorizer != Authorizer.None,
                        onCheckedChange = { isDisabled ->
                            viewModel.dispatch(
                                PreferredViewAction.SetAdbVerifyEnabledState(!isDisabled)
                            )
                        }
                    )
                    MiuixIgnoreBatteryOptimizationSetting(
                        checked = uiState.isIgnoringBatteryOptimizations,
                        enabled = !uiState.isIgnoringBatteryOptimizations,
                    ) { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) }
                }
            }
            item { SmallTitle(stringResource(R.string.backup_settings)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    BasicComponent(
                        title = stringResource(R.string.backup_settings_export),
                        summary = stringResource(R.string.backup_settings_export_desc),
                        enabled = !uiState.backupBusy,
                        onClick = { viewModel.dispatch(PreferredViewAction.RequestExportBackup) }
                    )
                    BasicComponent(
                        title = stringResource(R.string.backup_settings_restore),
                        summary = stringResource(R.string.backup_settings_restore_desc),
                        enabled = !uiState.backupBusy,
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "*/*")) }
                    )
                }
            }
            item { SmallTitle(stringResource(R.string.other)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    BasicComponent(
                        title = stringResource(R.string.lab),
                        summary = stringResource(R.string.lab_desc),
                        onClick = { navigator.push(Route.Lab) }
                    )
                    BasicComponent(
                        title = stringResource(R.string.about_detail),
                        summary = if (uiState.hasUpdate) stringResource(
                            R.string.update_available,
                            uiState.remoteVersion
                        ) else "$revLevel ${AppConfig.VERSION_NAME}",
                        summaryColor = BasicComponentColors(
                            color = if (uiState.hasUpdate) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            disabledColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant
                        ),
                        onClick = { navigator.push(Route.About) }
                    )
                }
            }
        }
    }
    errorDialogInfo?.let { dialogInfo ->
        ErrorDisplaySheet(
            showState = showErrorSheetState,
            exception = dialogInfo.exception,
            onDismissRequest = { showErrorSheetState.value = false },
            onRetry = errorDialogInfo?.retryAction?.let { retryAction ->
                {
                    showErrorSheetState.value = false
                    viewModel.dispatch(retryAction)
                }
            },
            title = stringResource(dialogInfo.titleResId)
        )
    }
    MiuixRestoreBackupConfirmDialog(
        show = showRestoreConfirmDialog.value,
        preview = pendingRestorePreview,
        onDismiss = {
            showRestoreConfirmDialog.value = false
            pendingRestorePreview = null
        },
        onConfirm = {
            viewModel.dispatch(PreferredViewAction.ConfirmRestoreBackup)
            pendingRestorePreview = null
            showRestoreConfirmDialog.value = false
        }
    )
    MiuixBackupValidationErrorDialog(
        errorText = backupValidationErrorText,
        onDismiss = { backupValidationErrorText = null }
    )
}

@Composable
private fun MiuixRestoreBackupConfirmDialog(
    show: Boolean,
    preview: BackupRestorePreview?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    WindowDialog(
        show = show,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.backup_settings_restore_confirm_title),
        content = {
            Column {
                Text(
                    text = preview?.formatBackupRestorePreview(context)
                        ?: stringResource(R.string.backup_settings_restore_confirm_desc),
                    color = MiuixTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        text = stringResource(R.string.cancel)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        text = stringResource(R.string.confirm),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

@Composable
private fun MiuixBackupValidationErrorDialog(
    errorText: String?,
    onDismiss: () -> Unit
) {
    WindowDialog(
        show = errorText != null,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.backup_settings_validation_failed_title),
        content = {
            Column {
                Text(
                    text = errorText.orEmpty(),
                    color = MiuixTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                    text = stringResource(R.string.confirm),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    )
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

private fun List<BackupValidationIssue>.formatBackupValidationIssues(context: Context): String =
    joinToString(separator = "\n") { issue ->
        context.getString(issue.messageResId, *issue.args.toTypedArray())
    }

@Composable
private fun MiuixDisableAdbVerify(
    checked: Boolean,
    isError: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MiuixSwitchWidget(
        title = stringResource(R.string.disable_adb_install_verify),
        description = if (!isError) stringResource(R.string.disable_adb_install_verify_desc)
        else stringResource(R.string.disable_adb_install_verify_not_support_dhizuku_desc),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

/**
 * A setting pkg for requesting to ignore battery optimizations.
 *
 * @param checked Whether the app is currently ignoring battery optimizations.
 * @param onCheckedChange Callback invoked when the user toggles the switch.
 */
@Composable
private fun MiuixIgnoreBatteryOptimizationSetting(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MiuixSwitchWidget(
        title = stringResource(R.string.ignore_battery_optimizations),
        description = if (enabled) stringResource(R.string.ignore_battery_optimizations_desc)
        else stringResource(R.string.ignore_battery_optimizations_desc_disabled),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}
