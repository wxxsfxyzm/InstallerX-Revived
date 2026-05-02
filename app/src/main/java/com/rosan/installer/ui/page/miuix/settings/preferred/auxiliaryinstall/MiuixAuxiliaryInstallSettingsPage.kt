// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.auxiliaryinstall

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall.AuxiliaryInstallSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall.AuxiliaryInstallSettingsEvent
import com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall.AuxiliaryInstallSettingsState
import com.rosan.installer.ui.page.main.settings.preferred.auxiliaryinstall.AuxiliaryInstallSettingsViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixAuxiliaryInstallSettingsPage(
    useBlur: Boolean,
    viewModel: AuxiliaryInstallSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()

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

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.auxiliary_install_settings),
                navigationIcon = {
                    MiuixBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection)
            ),
            overscrollEffect = null
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { MiuixSettingsTipCard(stringResource(R.string.auxiliary_install_auto_confirm_tip)) }
            item { SmallTitle(stringResource(R.string.auxiliary_install_auto_confirm)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixAccessibilityServiceItem(uiState.accessibilityServiceEnabled) {
                        viewModel.dispatch(AuxiliaryInstallSettingsAction.OpenAccessibilitySettings)
                    }
                    MiuixAutoConfirmUsbInstallWidget(uiState, viewModel)
                }
            }
            item { SmallTitle(stringResource(R.string.auxiliary_install_behavior)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp)
                ) {
                    MiuixShowToastWidget(uiState, viewModel)
                    MiuixDelayedRetryWidget(uiState, viewModel)
                    MiuixRequireScreenOnWidget(uiState, viewModel)
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun MiuixAccessibilityServiceItem(
    enabled: Boolean,
    onClick: () -> Unit
) {
    MiuixNavigationItemWidget(
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
private fun MiuixAutoConfirmUsbInstallWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel
) {
    MiuixSwitchWidget(
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
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeAutoConfirmUsbInstall(it))
        }
    )
}

@Composable
private fun MiuixShowToastWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel
) {
    MiuixSwitchWidget(
        icon = AppIcons.Notification,
        title = stringResource(R.string.auxiliary_install_show_toast),
        description = stringResource(R.string.auxiliary_install_show_toast_desc),
        checked = uiState.showToast,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeShowToast(it))
        }
    )
}

@Composable
private fun MiuixDelayedRetryWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel
) {
    MiuixSwitchWidget(
        icon = AppIcons.Retry,
        title = stringResource(R.string.auxiliary_install_delayed_retry),
        description = stringResource(R.string.auxiliary_install_delayed_retry_desc),
        checked = uiState.delayedRetry,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeDelayedRetry(it))
        }
    )
}

@Composable
private fun MiuixRequireScreenOnWidget(
    uiState: AuxiliaryInstallSettingsState,
    viewModel: AuxiliaryInstallSettingsViewModel
) {
    MiuixSwitchWidget(
        icon = AppIcons.BatteryOptimization,
        title = stringResource(R.string.auxiliary_install_require_screen_on),
        description = stringResource(R.string.auxiliary_install_require_screen_on_desc),
        checked = uiState.requireScreenOn,
        onCheckedChange = {
            viewModel.dispatch(AuxiliaryInstallSettingsAction.ChangeRequireScreenOn(it))
        }
    )
}
