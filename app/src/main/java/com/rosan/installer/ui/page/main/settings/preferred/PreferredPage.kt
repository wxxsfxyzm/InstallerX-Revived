// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.Level
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Navigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.snackbar.SwipeableSnackbarHost
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.layerBackdrop

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferredPage(
    navigator: Navigator = LocalNavigator.current,
    useBlur: Boolean,
    viewModel: PreferredViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
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

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(title) },
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
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = outerPadding.calculateBottomPadding()
            )
        ) {
            // --- Global Settings Group ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.personalization)
                ) {
                    item {
                        SettingsNavigationItemWidget(
                            icon = AppIcons.Theme,
                            title = stringResource(R.string.theme_settings),
                            description = stringResource(R.string.theme_settings_desc),
                            onClick = {
                                navigator.push(Route.Theme)
                            }
                        )
                    }
                    item {
                        SettingsNavigationItemWidget(
                            icon = AppIcons.InstallMode,
                            title = stringResource(R.string.installer_settings),
                            description = stringResource(R.string.installer_settings_desc),
                            onClick = {
                                navigator.push(Route.InstallerGlobal)
                            }
                        )
                    }
                    item {
                        SettingsNavigationItemWidget(
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
                        DisableAdbVerify(
                            checked = !uiState.adbVerifyEnabled,
                            isError = uiState.authorizer == Authorizer.Dhizuku,
                            enabled = uiState.authorizer != Authorizer.Dhizuku &&
                                    uiState.authorizer != Authorizer.None,
                            onCheckedChange = { isDisabled ->
                                viewModel.dispatch(PreferredViewAction.SetAdbVerifyEnabledState(!isDisabled))
                            }
                        )
                    }
                    item {
                        IgnoreBatteryOptimizationSetting(
                            checked = uiState.isIgnoringBatteryOptimizations,
                            enabled = !uiState.isIgnoringBatteryOptimizations,
                        ) { viewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) }
                    }
                }
            }
            // --- Other Settings Group ---
            item {
                SegmentedColumn(
                    title = stringResource(R.string.other)
                ) {
                    item {
                        SettingsAboutItemWidget(
                            imageVector = AppIcons.Lab,
                            headlineContentText = stringResource(R.string.lab),
                            supportingContentText = stringResource(R.string.lab_desc),
                            onClick = {
                                navigator.push(Route.Lab)
                            }
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
                            onClick = {
                                navigator.push(Route.About)
                            }
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
}
