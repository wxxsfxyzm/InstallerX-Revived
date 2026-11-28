// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.home.priv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiCustomAuthorizerDialog
import com.rosan.installer.ui.page.coui.widgets.CouiRadioItem
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiPrivPage(useBlur: Boolean, viewModel: HomePageViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current
    val showCustomizeAuthorizerDialog = remember { mutableStateOf(false) }
    var selectCustomizeOnConfirm by remember { mutableStateOf(false) }

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

    CouiCustomAuthorizerDialog(
        showState = showCustomizeAuthorizerDialog,
        initialAuthorizer = uiState.customizeAuthorizer,
        onDismiss = {
            showCustomizeAuthorizerDialog.value = false
            selectCustomizeOnConfirm = false
        },
        onConfirm = { authorizer ->
            val customizeAuthorizer = authorizer.trim()
            if (customizeAuthorizer.isBlank()) return@CouiCustomAuthorizerDialog

            if (selectCustomizeOnConfirm) {
                viewModel.dispatch(HomePageViewAction.EnableCustomizeAuthorizer(customizeAuthorizer))
            } else {
                viewModel.dispatch(HomePageViewAction.ChangeCustomizeAuthorizer(customizeAuthorizer))
            }
            selectCustomizeOnConfirm = false
        },
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.home_stat_authorizers)) },
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
                CouiSettingsTipCard(text = stringResource(R.string.priv_page_what_is_this_desc))
            }

            item {
                CouiSettingsTipCard(text = stringResource(R.string.priv_page_notice_desc))
            }

            item {
                Group(title = stringResource(R.string.config_authorizer)) {
                    Column(modifier = Modifier.selectableGroup()) {
                        CouiRadioItem(
                            title = if (uiState.isSystemApp) {
                                stringResource(R.string.working_status_system_installer)
                            } else {
                                stringResource(R.string.config_authorizer_none)
                            },
                            description = when {
                                !uiState.isSessionInstallSupported -> stringResource(R.string.unavailable)
                                uiState.isSystemApp -> stringResource(R.string.working_status_system_installer_desc)
                                else -> stringResource(R.string.working_status_none_authorizer_desc)
                            },
                            selected = uiState.globalAuthorizer == Authorizer.None,
                            enabled = uiState.isSessionInstallSupported,
                            onClick = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.None)) },
                        )
                        Divider()

                        val isRootAvailable = uiState.rootMode != RootMode.None
                        CouiRadioItem(
                            title = stringResource(R.string.config_authorizer_root),
                            description = if (isRootAvailable) {
                                stringResource(R.string.available) + " (${uiState.rootMode.name})"
                            } else {
                                stringResource(R.string.unavailable)
                            },
                            selected = uiState.globalAuthorizer == Authorizer.Root,
                            onClick = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Root)) },
                        )
                        Divider()

                        CouiRadioItem(
                            title = stringResource(R.string.config_authorizer_shizuku),
                            description = when {
                                uiState.shizukuAuthorized -> stringResource(R.string.running) + " (${uiState.shizukuMode.desc})"
                                uiState.shizukuAvailable -> stringResource(R.string.shizuku_not_authorized)
                                else -> stringResource(R.string.shizuku_not_available)
                            },
                            selected = uiState.globalAuthorizer == Authorizer.Shizuku,
                            onClick = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Shizuku)) },
                        )
                        Divider()

                        CouiRadioItem(
                            title = stringResource(R.string.config_authorizer_dhizuku),
                            description = when {
                                uiState.dhizukuAuthorized -> stringResource(R.string.running)
                                uiState.dhizukuAvailable -> stringResource(R.string.dhizuku_not_authorized)
                                else -> stringResource(R.string.dhizuku_not_available)
                            },
                            selected = uiState.globalAuthorizer == Authorizer.Dhizuku,
                            onClick = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Dhizuku)) },
                        )
                        Divider()

                        val isCustomizeSelected = uiState.globalAuthorizer == Authorizer.Customize
                        CouiRadioItem(
                            title = stringResource(R.string.config_authorizer_customize),
                            description = uiState.customizeAuthorizer.ifBlank {
                                stringResource(R.string.config_authorizer_customize)
                            },
                            selected = isCustomizeSelected,
                            onClick = {
                                if (isCustomizeSelected) {
                                    selectCustomizeOnConfirm = false
                                    showCustomizeAuthorizerDialog.value = true
                                } else if (uiState.customizeAuthorizer.isBlank()) {
                                    selectCustomizeOnConfirm = true
                                    showCustomizeAuthorizerDialog.value = true
                                } else {
                                    viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Customize))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
