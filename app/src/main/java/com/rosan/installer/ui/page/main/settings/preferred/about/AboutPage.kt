// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.widget.card.StatusWidget
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.NavigationItemWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.setting.UpdateLoadingIndicator
import com.rosan.installer.ui.page.main.widget.util.LogEventCollector
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutPage(
    useBlur: Boolean,
    viewModel: AboutViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val uriHandler = LocalUriHandler.current
    var showBottomSheet by remember { mutableStateOf(false) }

    LogEventCollector(viewModel)

    val topBarBackdrop = rememberMaterial3BlurBackdrop(useBlur)
    val upgradeIndicatorBackdrop = rememberMaterial3BlurBackdrop(useBlur)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(upgradeIndicatorBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(topBarBackdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(text = stringResource(id = R.string.about)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { navigator.pop() }
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBackdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = topBarBackdrop.getMaterial3AppBarColor()
                )
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = paddingValues,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    StatusWidget(viewModel, useBlur)
                }
            }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.about)
                ) {
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.ViewSourceCode,
                            title = stringResource(R.string.get_source_code),
                            description = stringResource(R.string.get_source_code_detail),
                            onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.OpenSourceLicense,
                            title = stringResource(R.string.open_source_license),
                            description = stringResource(R.string.open_source_license_settings_description),
                            onClick = { navigator.push(Route.OpenSourceLicense) }
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.Update,
                            title = stringResource(R.string.get_update),
                            description = stringResource(R.string.get_update_detail),
                            onClick = { showBottomSheet = true }
                        )
                    }
                    if (uiState.hasUpdate)
                        item {
                            NavigationItemWidget(
                                icon = AppIcons.Download,
                                title = stringResource(R.string.get_update_directly),
                                description = stringResource(R.string.get_update_directly_desc),
                                onClick = { viewModel.dispatch(AboutAction.PerformUpdate) }
                            )
                        }
                }
            }
            if (AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID)
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.debug)
                    ) {
                        item {
                            SwitchWidget(
                                icon = AppIcons.BugReport,
                                title = stringResource(R.string.save_logs),
                                description = stringResource(R.string.save_logs_desc),
                                checked = uiState.enableFileLogging,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        AboutAction.SetEnableFileLogging(
                                            it
                                        )
                                    )
                                }
                            )
                        }
                        item(animatedVisibility = uiState.enableFileLogging) {
                            BaseWidget(
                                icon = AppIcons.BugReport,
                                title = stringResource(R.string.export_logs),
                                description = stringResource(R.string.export_logs_desc),
                                onClick = { viewModel.dispatch(AboutAction.ShareLog) }
                            )
                        }
                    }
                }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            BottomSheetContent(
                title = stringResource(R.string.get_update),
                hasUpdate = uiState.hasUpdate,
                onDirectUpdateClick = {
                    showBottomSheet = false
                    viewModel.dispatch(AboutAction.PerformUpdate)
                }
            )
        }
    }
    UpdateLoadingIndicator(backdrop = upgradeIndicatorBackdrop, viewModel = viewModel)
}

@Composable
private fun BottomSheetContent(
    title: String,
    hasUpdate: Boolean,
    canDirectUpdate: Boolean = true,
    onDirectUpdateClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (hasUpdate && canDirectUpdate) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onDirectUpdateClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = AppIcons.Update,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.get_update_directly),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived/releases")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_github),
                contentDescription = "GitHub Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "GitHub")
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                uriHandler.openUri("https://t.me/installerx_revived")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_telegram),
                contentDescription = "Telegram Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Telegram")
        }
        Spacer(modifier = Modifier.size(60.dp))
    }
}
