// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewEvent
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiuixPreferredPage(
    viewModel: PreferredViewModel = koinViewModel(),
    hazeState: HazeState?,
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
    val hazeStyle = rememberMiuixHazeStyle()

    var errorDialogInfo by remember { mutableStateOf<PreferredViewEvent.ShowDefaultInstallerErrorDetail?>(null) }
    val showErrorSheetState = remember { mutableStateOf(false) }

    val defaultInstallerErrorDetailActionLabel = stringResource(R.string.details)
    LaunchedEffect(Unit) {
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
            }
        }
    }

    // Capture layout direction and horizontal safe insets for display cutouts in landscape
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = title,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                // Safely add horizontal paddings to avoid notches in landscape
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
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
                    val tip = if (capabilityProvider.isSystemApp) stringResource(R.string.config_authorizer_none_system_app_tips)
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
                    MiuixAutoLockInstaller(
                        checked = uiState.autoLockInstaller,
                        enabled = uiState.authorizer != Authorizer.None,
                    ) { viewModel.dispatch(PreferredViewAction.ChangeAutoLockInstaller(!uiState.autoLockInstaller)) }
                    MiuixDefaultInstaller(
                        lock = true,
                        enabled = uiState.authorizer != Authorizer.None,
                    ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(true)) }
                    MiuixDefaultInstaller(
                        lock = false,
                        enabled = uiState.authorizer != Authorizer.None,
                    ) { viewModel.dispatch(PreferredViewAction.SetDefaultInstaller(false)) }
                }
            }
            item { SmallTitle(stringResource(R.string.other)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixSettingsAboutItemWidget(
                        title = stringResource(R.string.lab),
                        summary = stringResource(R.string.lab_desc)
                    ) {
                        navigator.push(Route.Lab)
                    }
                    MiuixSettingsAboutItemWidget(
                        title = stringResource(R.string.about_detail),
                        summary = if (uiState.hasUpdate) stringResource(
                            R.string.update_available,
                            uiState.remoteVersion
                        ) else "$revLevel ${AppConfig.VERSION_NAME}",
                        summaryColor = BasicComponentColors(
                            color = if (uiState.hasUpdate) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            disabledColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant
                        )
                    ) {
                        navigator.push(Route.About)
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
    }
}
