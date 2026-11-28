// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.installer.authorizer

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiIntNumberPickerWidget
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CouiAuthorizerCustPage(
    useBlur: Boolean,
    viewModel: AuthorizerCustViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val isSystemApp = capabilityProvider.isSystemApp
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.authorizer_customization)) },
                navigationIcon = {
                    CouiBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior,
                useCouiBlur = useBlur,
            )
        },
    ) { insets ->
        val contentPadding = remember(scrollBehavior.state, insets, density) {
            scrollBehavior.state.contentPadding(insets, density)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(insets),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Group(
                    title = if (isSystemApp) {
                        stringResource(R.string.working_status_system_installer)
                    } else {
                        stringResource(R.string.config_authorizer_none)
                    },
                ) {
                    if (isSystemApp) {
                        CouiSwitchItem(
                            title = stringResource(R.string.config_always_use_root_in_system),
                            description = stringResource(R.string.config_always_use_root_in_system_desc),
                            checked = uiState.alwaysUseRootInSystem,
                            onCheckedChange = {
                                viewModel.dispatch(AuthorizerCustAction.ChangeAlwaysUseRootInSystem(it))
                            },
                        )
                    }
                    if (!isSystemApp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        CouiSwitchItem(
                            title = stringResource(R.string.lab_install_without_user_action),
                            description = stringResource(R.string.lab_install_without_user_action_desc),
                            checked = uiState.allowInstallWithoutUserAction,
                            onCheckedChange = {
                                viewModel.dispatch(AuthorizerCustAction.ChangeAllowInstallWithoutUserAction(it))
                            },
                        )
                    }
                }
            }

            if (!isSystemApp) {
                item {
                    Group(title = stringResource(R.string.extras)) {
                        CouiIntNumberPickerWidget(
                            title = stringResource(R.string.close_session_countdown),
                            description = stringResource(R.string.close_session_countdown_desc),
                            value = uiState.closeSessionCountDown,
                            startInt = 1,
                            endInt = 10,
                            onValueChange = {
                                viewModel.dispatch(AuthorizerCustAction.ChangeCloseSessionCountDown(it))
                            },
                        )
                    }
                }
            }
        }
    }
}
