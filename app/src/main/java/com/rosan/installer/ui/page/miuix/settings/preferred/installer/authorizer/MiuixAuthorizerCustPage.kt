// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.installer.authorizer

import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustViewModel
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixIntNumberPickerWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixAuthorizerCustPage(
    useBlur: Boolean,
    viewModel: AuthorizerCustViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val scrollBehavior = MiuixScrollBehavior()

    val isSystemApp = capabilityProvider.isSystemApp

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val topBarBackdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(topBarBackdrop),
                color = topBarBackdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.authorizer_customization),
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

            item {
                SmallTitle(
                    text = if (isSystemApp) stringResource(R.string.working_status_system_installer)
                    else stringResource(R.string.config_authorizer_none)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    if (isSystemApp) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.config_always_use_root_in_system),
                            description = stringResource(R.string.config_always_use_root_in_system_desc),
                            checked = uiState.alwaysUseRootInSystem,
                            onCheckedChange = { viewModel.dispatch(AuthorizerCustAction.ChangeAlwaysUseRootInSystem(it)) }
                        )
                    }
                    if (!isSystemApp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.lab_install_without_user_action),
                            description = stringResource(R.string.lab_install_without_user_action_desc),
                            checked = uiState.allowInstallWithoutUserAction,
                            onCheckedChange = { viewModel.dispatch(AuthorizerCustAction.ChangeAllowInstallWithoutUserAction(it)) }
                        )
                    }
                }
            }

            if (!isSystemApp) {
                item { SmallTitle(stringResource(R.string.extras)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        MiuixIntNumberPickerWidget(
                            title = stringResource(R.string.close_session_countdown),
                            description = stringResource(R.string.close_session_countdown_desc),
                            value = uiState.closeSessionCountDown,
                            startInt = 1,
                            endInt = 10,
                            onValueChange = {
                                viewModel.dispatch(AuthorizerCustAction.ChangeCloseSessionCountDown(it))
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
