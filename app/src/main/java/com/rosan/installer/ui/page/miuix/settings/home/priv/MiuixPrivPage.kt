// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.home.priv

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.RootMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Navigator
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixPrivPage(
    useBlur: Boolean,
    navigator: Navigator = LocalNavigator.current,
    viewModel: HomePageViewModel = koinViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

    val backdrop = rememberMiuixBlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.home_stat_authorizers),
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
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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
            item {
                MiuixSettingsTipCard(text = stringResource(R.string.priv_page_what_is_this_desc))
            }

            item {
                MiuixSettingsTipCard(text = stringResource(R.string.priv_page_notice_desc))
            }

            item {
                SmallTitle(text = stringResource(id = R.string.config_authorizer))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    // None / System App
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = if (uiState.isSystemApp) stringResource(R.string.working_status_system_installer) else stringResource(R.string.config_authorizer_none),
                        summary = if (uiState.isSystemApp) stringResource(R.string.working_status_system_installer_desc)
                        else stringResource(R.string.working_status_none_authorizer_desc),
                        checked = uiState.globalAuthorizer == Authorizer.None,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.None)) }
                    )

                    // Root
                    val isRootAvailable = uiState.rootMode != RootMode.None
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_root),
                        summary = if (isRootAvailable) stringResource(R.string.available) + " (${uiState.rootMode.name})"
                        else stringResource(R.string.unavailable),
                        checked = uiState.globalAuthorizer == Authorizer.Root,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Root)) }
                    )

                    // Shizuku
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_shizuku),
                        summary = when {
                            uiState.shizukuAuthorized -> stringResource(R.string.running) + " (${uiState.shizukuMode.desc})"
                            uiState.shizukuAvailable -> stringResource(R.string.shizuku_not_authorized)
                            else -> stringResource(R.string.shizuku_not_available)
                        },
                        checked = uiState.globalAuthorizer == Authorizer.Shizuku,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Shizuku)) }
                    )

                    // Dhizuku
                    CheckboxPreference(
                        checkboxLocation = CheckboxLocation.End,
                        title = stringResource(R.string.config_authorizer_dhizuku),
                        summary = when {
                            uiState.dhizukuAuthorized -> stringResource(R.string.running)
                            uiState.dhizukuAvailable -> stringResource(R.string.dhizuku_not_authorized)
                            else -> stringResource(R.string.dhizuku_not_available)
                        },
                        checked = uiState.globalAuthorizer == Authorizer.Dhizuku,
                        onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(Authorizer.Dhizuku)) }
                    )
                }
            }

            item {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
