// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.BottomSheetContent
import com.rosan.installer.ui.page.main.settings.preferred.ExportLogsWidget
import com.rosan.installer.ui.page.main.settings.preferred.SettingsAboutItemWidget
import com.rosan.installer.ui.page.main.widget.card.StatusWidget
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.LabelWidget
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.setting.UpdateLoadingIndicator
import com.rosan.installer.ui.page.main.widget.util.LogEventCollector
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutPage(
    viewModel: AboutViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    var showBottomSheet by remember { mutableStateOf(false) }

    LogEventCollector(viewModel)

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .hazeSource(state = hazeState),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.about))
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    StatusWidget(viewModel)
                }
            }
            item { LabelWidget(stringResource(R.string.about)) }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.ViewSourceCode,
                    headlineContentText = stringResource(R.string.get_source_code),
                    supportingContentText = stringResource(R.string.get_source_code_detail),
                    onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                )
            }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.OpenSourceLicense,
                    headlineContentText = stringResource(R.string.open_source_license),
                    supportingContentText = stringResource(R.string.open_source_license_settings_description),
                    onClick = { navigator.push(Route.OpenSourceLicense) }
                )
            }
            item {
                SettingsAboutItemWidget(
                    imageVector = AppIcons.Update,
                    headlineContentText = stringResource(R.string.get_update),
                    supportingContentText = stringResource(R.string.get_update_detail),
                    onClick = { showBottomSheet = true }
                )
            }
            if (uiState.hasUpdate)
                item {
                    SettingsAboutItemWidget(
                        imageVector = AppIcons.Download,
                        headlineContentText = stringResource(R.string.get_update_directly),
                        supportingContentText = stringResource(R.string.get_update_directly_desc),
                        onClick = { viewModel.dispatch(AboutAction.PerformUpdate) }
                    )
                }
            if (AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID) {
                item { LabelWidget(stringResource(R.string.debug)) }
                item {
                    SwitchWidget(
                        icon = AppIcons.BugReport,
                        title = stringResource(R.string.save_logs),
                        description = stringResource(R.string.save_logs_desc),
                        checked = uiState.enableFileLogging,
                        onCheckedChange = { viewModel.dispatch(AboutAction.SetEnableFileLogging(it)) }
                    )
                }
                item {
                    AnimatedVisibility(
                        visible = uiState.enableFileLogging,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) { ExportLogsWidget(viewModel) }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
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
    UpdateLoadingIndicator(hazeState = hazeState, viewModel = viewModel)
}
