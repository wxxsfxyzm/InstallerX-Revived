// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.SettingsNavigationItemWidget
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SplicedColumnGroup
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.none
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyAuxiliaryInstallSettingsPage(
    viewModel: AuxiliaryInstallSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(AuxiliaryInstallSettingsAction.RefreshAccessibilityServiceStatus)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AuxiliaryInstallSettingsEvent.ShowMessage -> context.toast(event.resId)
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.none,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auxiliary_install_settings)) },
                navigationIcon = {
                    AppBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            item { InfoTipCard(text = stringResource(R.string.auxiliary_install_auto_confirm_tip)) }
            item { LabelWidget(label = stringResource(R.string.auxiliary_install_auto_confirm)) }
            item {
                AccessibilityServiceItem(uiState.accessibilityServiceEnabled) {
                    viewModel.dispatch(AuxiliaryInstallSettingsAction.OpenAccessibilitySettings)
                }
            }
            item { AutoConfirmUsbInstallWidget(uiState, viewModel, isM3E = false) }
            item { LabelWidget(label = stringResource(R.string.auxiliary_install_behavior)) }
            item { ShowToastWidget(uiState, viewModel, isM3E = false) }
            item { DelayedRetryWidget(uiState, viewModel, isM3E = false) }
            item { RequireScreenOnWidget(uiState, viewModel, isM3E = false) }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewAuxiliaryInstallSettingsPage(
    useBlur: Boolean,
    viewModel: AuxiliaryInstallSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(AuxiliaryInstallSettingsAction.RefreshAccessibilityServiceStatus)
    }

    LaunchedEffect(Unit) {
        topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AuxiliaryInstallSettingsEvent.ShowMessage -> context.toast(event.resId)
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
        contentWindowInsets = WindowInsets.none,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(stringResource(R.string.auxiliary_install_settings)) },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navigator.pop() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.size(16.dp))
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            item { InfoTipCard(text = stringResource(R.string.auxiliary_install_auto_confirm_tip)) }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.auxiliary_install_auto_confirm)
                ) {
                    item {
                        AccessibilityServiceItem(uiState.accessibilityServiceEnabled) {
                            viewModel.dispatch(AuxiliaryInstallSettingsAction.OpenAccessibilitySettings)
                        }
                    }
                    item { AutoConfirmUsbInstallWidget(uiState, viewModel) }
                }
            }
            item {
                SplicedColumnGroup(
                    title = stringResource(R.string.auxiliary_install_behavior)
                ) {
                    item { ShowToastWidget(uiState, viewModel) }
                    item { DelayedRetryWidget(uiState, viewModel) }
                    item { RequireScreenOnWidget(uiState, viewModel) }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun AccessibilityServiceItem(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SettingsNavigationItemWidget(
        icon = AppIcons.Permission,
        title = stringResource(R.string.auxiliary_install_accessibility_service),
        description = stringResource(
            if (enabled)
                R.string.auxiliary_install_accessibility_service_enabled
            else
                R.string.auxiliary_install_accessibility_service_disabled
        ),
        onClick = onClick
    )
}

@Composable
private fun AutoConfirmUsbInstallWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel,
    isM3E: Boolean = true
) {
    SwitchWidget(
        icon = AppIcons.AutoFixHigh,
        title = stringResource(R.string.auxiliary_install_auto_confirm_usb),
        description = stringResource(
            if (uiState.accessibilityServiceEnabled)
                R.string.auxiliary_install_auto_confirm_usb_desc
            else
                R.string.auxiliary_install_accessibility_required
        ),
        checked = uiState.autoConfirmUsbInstall,
        enabled = uiState.accessibilityServiceEnabled || uiState.autoConfirmUsbInstall,
        isM3E = isM3E,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeAutoConfirmUsbInstall(it))
        }
    )
}

@Composable
private fun ShowToastWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel,
    isM3E: Boolean = true
) {
    SwitchWidget(
        icon = AppIcons.Notification,
        title = stringResource(R.string.auxiliary_install_show_toast),
        description = stringResource(R.string.auxiliary_install_show_toast_desc),
        checked = uiState.showToast,
        isM3E = isM3E,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeShowToast(it))
        }
    )
}

@Composable
private fun DelayedRetryWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel,
    isM3E: Boolean = true
) {
    SwitchWidget(
        icon = AppIcons.Retry,
        title = stringResource(R.string.auxiliary_install_delayed_retry),
        description = stringResource(R.string.auxiliary_install_delayed_retry_desc),
        checked = uiState.delayedRetry,
        isM3E = isM3E,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeDelayedRetry(it))
        }
    )
}

@Composable
private fun RequireScreenOnWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel,
    isM3E: Boolean = true
) {
    SwitchWidget(
        icon = AppIcons.BatteryOptimization,
        title = stringResource(R.string.auxiliary_install_require_screen_on),
        description = stringResource(R.string.auxiliary_install_require_screen_on_desc),
        checked = uiState.requireScreenOn,
        isM3E = isM3E,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeRequireScreenOn(it))
        }
    )
}
