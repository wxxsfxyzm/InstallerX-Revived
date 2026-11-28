// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.home

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiCard
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewEvent
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.settings.home.HomePageViewState
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.miuixHomeStatusCardColorActivated
import com.rosan.installer.ui.theme.miuixHomeStatusCardColorDeactivated
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiHomePage(
    enableBlur: Boolean,
    viewModel: HomePageViewModel = koinViewModel(),
    title: String,
    configCount: Int = 0,
    outerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onNavigateToProfiles: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomePageViewEvent.ShowDefaultInstallerResult -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is HomePageViewEvent.ShowDefaultInstallerErrorDetail -> {
                    snackbarHostState.showSnackbar(context.getString(event.titleResId))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = title) },
                scrollBehavior = scrollBehavior,
                useCouiBlur = enableBlur,
            )
        },
    ) { innerPadding ->
        val contentPadding = remember(scrollBehavior.state, innerPadding, density) {
            scrollBehavior.state.contentPadding(innerPadding, density)
        }
        val bottomPadding = maxOf(
            outerPadding.calculateBottomPadding(),
            contentPadding.calculateBottomPadding(),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = bottomPadding + 16.dp,
            ),
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = CouiPageDefaults.horizontalMargin)) {
                    CouiStatusGrid(
                        uiState = uiState,
                        configCount = configCount,
                        onInstallerClick = { navigator.push(Route.DefaultInstaller) },
                        onAuthorizersClick = { navigator.push(Route.Priv) },
                        onProfilesClick = onNavigateToProfiles,
                    )
                }
            }

            item {
                val isCustomizeAuthorizer = uiState.globalAuthorizer == Authorizer.Customize
                val authorizerText = when {
                    isCustomizeAuthorizer ->
                        uiState.customizeAuthorizer
                            .takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.config_authorizer_command_desc, it) }
                            ?: stringResource(R.string.config_authorizer_customize)

                    uiState.isSystemApp -> stringResource(R.string.working_status_system_installer)

                    uiState.globalAuthorizer == Authorizer.Shizuku -> {
                        stringResource(R.string.config_authorizer_shizuku) + " " + when {
                            uiState.shizukuAuthorized -> "(${uiState.shizukuMode.desc})"
                            uiState.shizukuAvailable -> "(${stringResource(R.string.shizuku_not_authorized)})"
                            else -> "(${stringResource(R.string.shizuku_not_available)})"
                        }
                    }

                    uiState.globalAuthorizer == Authorizer.Root -> {
                        stringResource(R.string.config_authorizer_root) + " " + if (uiState.rootMode != RootMode.None) {
                            "(${uiState.rootMode.name})"
                        } else {
                            "(${stringResource(R.string.unavailable)})"
                        }
                    }

                    uiState.globalAuthorizer == Authorizer.Dhizuku -> {
                        stringResource(R.string.config_authorizer_dhizuku) + " " + when {
                            uiState.dhizukuAuthorized -> "(${stringResource(R.string.activate)})"
                            uiState.dhizukuAvailable -> "(${stringResource(R.string.dhizuku_not_authorized)})"
                            else -> "(${stringResource(R.string.dhizuku_not_available)})"
                        }
                    }

                    else -> stringResource(uiState.globalAuthorizer.displayNameRes)
                }

                Group {
                    CouiBasicItem(
                        title = stringResource(R.string.home_device_info_model),
                        summary = uiState.deviceName,
                    )
                    Divider()
                    CouiBasicItem(
                        title = stringResource(R.string.home_device_info_system),
                        summary = DeviceConfig.systemVersion,
                    )
                    Divider()
                    CouiBasicItem(
                        title = stringResource(R.string.home_device_info_active_authorizer),
                        summary = authorizerText,
                    )
                    Divider()
                    CouiBasicItem(
                        title = stringResource(R.string.home_device_info_default_installer),
                        summary = uiState.defaultInstaller,
                    )
                }
            }

            item {
                Group {
                    CouiBasicItem(
                        title = stringResource(R.string.home_learn_more_installerx_title),
                        summary = stringResource(R.string.home_learn_more_installerx_desc),
                        trailing = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_link_icon),
                                contentDescription = null,
                                tint = CouiTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { uriHandler.openUri("https://wxxsfxyzm.github.io/InstallerX-Revived-Website/") },
                    )
                }
            }
        }
    }
}

@Composable
private fun CouiStatusGrid(
    uiState: HomePageViewState,
    configCount: Int,
    onInstallerClick: () -> Unit,
    onAuthorizersClick: () -> Unit,
    onProfilesClick: () -> Unit,
) {
    val isActive = uiState.isDefaultInstaller || uiState.isSystemApp
    val isSystemInstallerActive = uiState.isSystemApp && uiState.globalAuthorizer == Authorizer.None
    val useMonet = InstallerTheme.useCouiMonet
    val m3Scheme = InstallerTheme.colorScheme
    val dark = CouiTheme.isDark

    val containerColor = if (isActive) {
        when {
            useMonet -> m3Scheme.secondaryContainer
            dark -> Color(0xFF1A3825)
            else -> Color(0xFFDFFAE4)
        }
    } else {
        when {
            useMonet -> m3Scheme.errorContainer
            dark -> Color(0xFF381A1A)
            else -> Color(0xFFFAEEEE)
        }
    }

    val textContentColor = if (isActive) {
        if (useMonet) m3Scheme.onSecondaryContainer else CouiTheme.colorScheme.onSurface
    } else {
        if (useMonet) m3Scheme.onErrorContainer else CouiTheme.colorScheme.onSurface
    }
    val descTextColor = textContentColor.copy(alpha = 0.8f)

    val statusSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CouiCard(
            modifier = Modifier
                .fillMaxWidth()
                .couiButtonFeedback(dark = dark, interactionSource = statusSource)
                .clickable(
                    interactionSource = statusSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onInstallerClick,
                ),
            color = containerColor,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(50.dp, 38.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = if (isActive) AppIcons.Active else Icons.Rounded.ErrorOutline,
                        tint = if (isActive) {
                            if (useMonet) {
                                m3Scheme.primary.copy(alpha = 0.8f)
                            } else {
                                miuixHomeStatusCardColorActivated
                            }
                        } else {
                            if (useMonet) {
                                m3Scheme.error.copy(alpha = 0.8f)
                            } else {
                                miuixHomeStatusCardColorDeactivated
                            }
                        },
                        contentDescription = null,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            when {
                                uiState.isSystemApp -> R.string.working_status_system_installer
                                uiState.isDefaultInstaller -> R.string.working_status_default_installer
                                else -> R.string.working_status_not_default_installer
                            },
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textContentColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            if (uiState.isSystemApp) {
                                R.string.working_status_system_installer_desc
                            } else if (uiState.isDefaultInstaller) {
                                R.string.working_status_default_installer_desc
                            } else {
                                R.string.working_status_not_default_installer_click_details
                            },
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = descTextColor,
                    )

                    Spacer(Modifier.height(36.dp))

                    if (!isSystemInstallerActive) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(uiState.globalAuthorizer.displayNameRes),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = descTextColor,
                        )
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CouiStatCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                title = stringResource(R.string.home_stat_authorizers),
                value = uiState.availableAuthorizerCount.toString(),
                onClick = onAuthorizersClick,
            )
            CouiStatCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                title = stringResource(R.string.home_stat_profiles),
                value = configCount.toString(),
                onClick = onProfilesClick,
            )
        }
    }
}

@Composable
private fun CouiStatCard(modifier: Modifier = Modifier, title: String, value: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val dark = CouiTheme.isDark

    CouiCard(
        modifier = modifier
            .couiButtonFeedback(dark = dark, interactionSource = source)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = CouiTheme.colorScheme.onSurface,
            )
        }
    }
}
