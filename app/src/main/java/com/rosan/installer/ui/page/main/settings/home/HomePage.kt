// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.TaskAlt
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.domain.settings.model.RootMode
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Navigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(
    navigator: Navigator = LocalNavigator.current,
    useBlur: Boolean,
    viewModel: HomePageViewModel = koinViewModel(),
    title: String,
    outerPadding: PaddingValues,
    configCount: Int = 0,
    onNavigateToProfiles: () -> Unit = {}
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = 16.dp + horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding() + 16.dp,
                end = 16.dp + horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = outerPadding.calculateBottomPadding()
            )
        ) {
            item {
                InstallerStatusCard(
                    isActive = uiState.isDefaultInstaller,
                    isSystemApp = uiState.isSystemApp,
                    onClick = { navigator.push(Route.DefaultInstaller) }
                )
            }

            item { Spacer(modifier = Modifier.size(12.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_stat_authorizers),
                        value = uiState.availableAuthorizerCount.toString(),
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        onClick = { navigator.push(Route.Priv) }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_stat_profiles),
                        value = configCount.toString(),
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        onClick = onNavigateToProfiles
                    )
                }
            }

            item {
                SegmentedColumn(
                    title = stringResource(R.string.home_device_info_title),
                    contentPadding = PaddingValues(top = 16.dp)
                ) {
                    item {
                        BaseWidget(
                            title = stringResource(R.string.home_device_info_model),
                            description = DeviceConfig.deviceName,
                            iconPlaceholder = false
                        )
                    }
                    item {
                        BaseWidget(
                            title = stringResource(R.string.home_device_info_system),
                            description = DeviceConfig.systemVersion,
                            iconPlaceholder = false
                        )
                    }
                    item {
                        val authorizerText = when {
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
                                } else "(${stringResource(R.string.unavailable)})"
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
                        BaseWidget(
                            title = stringResource(R.string.home_device_info_active_authorizer),
                            description = authorizerText,
                            iconPlaceholder = false
                        )
                    }
                    item {
                        BaseWidget(
                            title = stringResource(R.string.home_device_info_default_installer),
                            description = uiState.defaultInstaller,
                            iconPlaceholder = false
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InstallerStatusCard(
    isActive: Boolean,
    isSystemApp: Boolean = false,
    onClick: () -> Unit = {}
) {
    val displayAsActive = isActive || isSystemApp

    val containerColor = if (displayAsActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = if (displayAsActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    val icon = if (displayAsActive) Icons.TwoTone.TaskAlt else Icons.TwoTone.Warning
    val contentDescRes = if (displayAsActive) R.string.activate else R.string.inactivate
    val titleRes = when {
        isSystemApp -> R.string.working_status_system_installer
        isActive -> R.string.working_status_default_installer
        else -> R.string.working_status_not_default_installer
    }

    val descRes = when {
        isSystemApp -> R.string.working_status_system_installer_desc
        isActive -> R.string.working_status_default_installer_desc
        else -> R.string.working_status_not_default_installer_click_details
    }

    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(contentDescRes),
                tint = contentColor,
                modifier = Modifier
                    .size(28.dp)
                    .padding(horizontal = 4.dp),
            )
            Column(Modifier.padding(start = 20.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = contentColor,
                )
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmallEmphasized,
                    color = contentColor,
                )
            }
        }
    }
}

// Unified preview functions
@Composable
private fun PreviewOfInstallerStatusCard(isActive: Boolean) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Authorizer.entries.forEach { _ ->
            item {
                InstallerStatusCard(isActive = isActive)
            }
        }
    }
}

@Preview(name = "preview_active")
@Composable
private fun ActiveStatusCardPreview() {
    PreviewOfInstallerStatusCard(isActive = true)
}

@Preview(name = "preview_active", locale = "zh-rCN")
@Composable
private fun ActiveStatusCardPreviewTranslate() {
    PreviewOfInstallerStatusCard(isActive = true)
}

@Preview(name = "preview_inactive")
@Composable
private fun InactiveStatusCardPreview() {
    PreviewOfInstallerStatusCard(isActive = false)
}

@Preview(name = "preview_inactive", locale = "zh-rCN")
@Composable
private fun InactiveStatusCardPreviewTranslate() {
    PreviewOfInstallerStatusCard(isActive = false)
}
