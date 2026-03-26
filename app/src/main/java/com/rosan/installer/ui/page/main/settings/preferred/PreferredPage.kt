// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.Level
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.SettingsScreen
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.main.widget.setting.AutoLockInstaller
import com.rosan.installer.ui.page.main.widget.setting.DefaultInstaller
import com.rosan.installer.ui.page.main.widget.setting.DisableAdbVerify
import com.rosan.installer.ui.page.main.widget.setting.IgnoreBatteryOptimizationSetting
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SettingsAboutItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.none
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferredPage(
    navController: NavController,
    viewModel: PreferredViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
        mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(
            null
        )
    }

    val detailLabel = stringResource(id = R.string.details)

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
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(text = stringResource(id = R.string.preferred))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding()),
                hostState = snackBarHostState
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = outerPadding.calculateBottomPadding()
            )
        ) {
            item { LabelWidget(stringResource(R.string.global)) }
            item {
                SettingsNavigationItemWidget(
                    icon = AppIcons.Theme,
                    title = stringResource(R.string.theme_settings),
                    description = stringResource(R.string.theme_settings_desc),
                    onClick = {
                        // Navigate using NavController instead of changing state
                        navController.navigate(SettingsScreen.Theme.route)
                    }
                )
            }
            item {
                SettingsNavigationItemWidget(
                    icon = AppIcons.InstallMode,
                    title = stringResource(R.string.installer_settings),
                    description = stringResource(R.string.installer_settings_desc),
                    onClick = {
                        // Navigate using NavController
                        navController.navigate(SettingsScreen.InstallerGlobal.route)
                    }
                )
            }
            item {
                SettingsNavigationItemWidget(
                    icon = AppIcons.Delete,
                    title = stringResource(R.string.uninstaller_settings),
                    description = stringResource(R.string.uninstaller_settings_desc),
                    onClick = {
                        navController.navigate(SettingsScreen.UninstallerGlobal.route)
                    }
                )
            }
            if (uiState.authorizer == Authorizer.None)
                item {
                    val tip = if (capabilityProvider.isSystemApp) stringResource(R.string.config_authorizer_none_system_app_tips)
                    else stringResource(R.string.config_authorizer_none_tips)
                    InfoTipCard(text = tip)
                }
            item { LabelWidget(stringResource(R.string.basic)) }
            item {
                DisableAdbVerify(
                    checked = !uiState.adbVerifyEnabled,
                    isError = uiState.authorizer == Authorizer.Dhizuku,
                    enabled = uiState.authorizer != Authorizer.Dhizuku &&
                            uiState.authorizer != Authorizer.None,
                    isM3E = false,
                    onCheckedChange = { isDisabled ->
                        viewModel.dispatch(
                            PreferredViewAction.SetAdbVerifyEnabledState(!isDisabled)
                        )
                    }
                )
            }
            item {
                IgnoreBatteryOptimizationSetting(
                    checked = uiState.isIgnoringBatteryOptimizations,
                    enabled = !uiState.isIgnoringBatteryOptimizations,
                    isM3E = false,
                ) { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) }
            }
            item {
                AutoLockInstaller(
                    checked = uiState.autoLockInstaller,
                    enabled = uiState.authorizer != Authorizer.None,
                    isM3E = false
                ) { viewModel.dispatch(PreferredViewAction.ChangeAutoLockInstaller(!uiState.autoLockInstaller)) }
            }
            item {
                DefaultInstaller(
                    lock = true,
                    enabled = uiState.authorizer != Authorizer.None
                ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(true)) }
            }
            item {
                DefaultInstaller(
                    lock = false,
                    enabled = uiState.authorizer != Authorizer.None
                ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(false)) }
            }
            item { LabelWidget(stringResource(R.string.other)) }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.Lab,
                    headlineContentText = stringResource(R.string.lab),
                    supportingContentText = stringResource(R.string.lab_desc),
                    onClick = { navController.navigate(SettingsScreen.Lab.route) }
                )
            }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.Info,
                    headlineContentText = stringResource(R.string.about_detail),
                    supportingContentText = if (uiState.hasUpdate) stringResource(
                        R.string.update_available,
                        uiState.remoteVersion
                    ) else "$revLevel ${AppConfig.VERSION_NAME}",
                    supportingContentColor = if (uiState.hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { navController.navigate(SettingsScreen.About.route) }
                )
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    errorDialogInfo?.let { dialogInfo ->
        ErrorDisplayDialog(
            exception = dialogInfo.exception,
            onDismissRequest = { errorDialogInfo = null },
            onRetry = {
                errorDialogInfo = null // Dismiss dialog
                viewModel.dispatch(dialogInfo.retryAction) // Dispatch the retry action
            },
            title = stringResource(dialogInfo.titleResId)
        )
    }
}