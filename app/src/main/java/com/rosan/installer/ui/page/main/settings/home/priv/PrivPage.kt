// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package com.rosan.installer.ui.page.main.settings.home.priv

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.widget.card.TitleTipCard
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.RadioButtonWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrivPage(
    useBlur: Boolean,
    viewModel: HomePageViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

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
                title = {
                    Text(stringResource(R.string.home_stat_authorizers))
                },
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { navigator.pop() }
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
            contentPadding = paddingValues
        ) {
            item {
                SegmentedColumn {
                    item {
                        val selected = uiState.globalAuthorizer == Authorizer.None
                        RadioButtonWidget(
                            icon = AppIcons.None,
                            title = if (uiState.isSystemApp) stringResource(R.string.working_status_system_installer) else stringResource(
                                R.string.config_authorizer_none
                            ),
                            description = if (uiState.isSystemApp) stringResource(R.string.working_status_system_installer_desc)
                            else stringResource(R.string.working_status_none_authorizer_desc),
                            selected = selected,
                            onClick = {
                                viewModel.dispatch(
                                    HomePageViewAction.ChangeAuthorizer(
                                        Authorizer.None
                                    )
                                )
                            }
                        )
                    }
                    item {
                        val isAvailable = uiState.rootMode != RootMode.None
                        val selected = uiState.globalAuthorizer == Authorizer.Root
                        RadioButtonWidget(
                            icon = AppIcons.Root,
                            title = stringResource(R.string.config_authorizer_root),
                            description = if (isAvailable) stringResource(R.string.available) + " (${uiState.rootMode.name})"
                            else stringResource(R.string.unavailable),
                            selected = selected,
                            onClick = {
                                viewModel.dispatch(
                                    HomePageViewAction.ChangeAuthorizer(
                                        Authorizer.Root
                                    )
                                )
                            }
                        )
                    }
                    item {
                        val selected = uiState.globalAuthorizer == Authorizer.Shizuku
                        RadioButtonWidget(
                            icon = ImageVector.vectorResource(R.drawable.ic_shizuku),
                            title = stringResource(R.string.config_authorizer_shizuku),
                            description = when {
                                uiState.shizukuAuthorized -> stringResource(R.string.activate) + " (${uiState.shizukuMode.desc})"
                                uiState.shizukuAvailable -> stringResource(R.string.shizuku_not_authorized)
                                else -> stringResource(R.string.shizuku_not_available)
                            },
                            selected = selected,
                            onClick = {
                                viewModel.dispatch(
                                    HomePageViewAction.ChangeAuthorizer(
                                        Authorizer.Shizuku
                                    )
                                )
                            }
                        )
                    }
                    item {
                        val selected = uiState.globalAuthorizer == Authorizer.Dhizuku
                        RadioButtonWidget(
                            icon = AppIcons.InstallAllowRestrictedPermissions,
                            title = stringResource(R.string.config_authorizer_dhizuku),
                            description = when {
                                uiState.dhizukuAuthorized -> stringResource(R.string.activate)
                                uiState.dhizukuAvailable -> stringResource(R.string.dhizuku_not_authorized)
                                else -> stringResource(R.string.dhizuku_not_available)
                            },
                            selected = selected,
                            onClick = {
                                viewModel.dispatch(
                                    HomePageViewAction.ChangeAuthorizer(
                                        Authorizer.Dhizuku
                                    )
                                )
                            }
                        )
                    }
                }
            }
            item {
                TitleTipCard(
                    title = stringResource(R.string.priv_page_what_is_this_title),
                    text = stringResource(R.string.priv_page_what_is_this_desc)
                )
            }
            item {
                TitleTipCard(
                    title = stringResource(R.string.priv_page_notice_title),
                    text = stringResource(R.string.priv_page_notice_desc)
                )
            }
        }
    }
}
