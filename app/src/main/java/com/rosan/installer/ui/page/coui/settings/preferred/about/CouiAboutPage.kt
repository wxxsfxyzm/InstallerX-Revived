// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiBadge
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiBlockingLoadingDialog
import com.rosan.installer.ui.page.coui.widgets.CouiErrorDisplaySheet
import com.rosan.installer.ui.page.coui.widgets.CouiNavigationItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.coui.widgets.CouiUpdateDialog
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutAction
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.ui.page.main.widget.util.LogEventCollector
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiAboutPage(
    useBlur: Boolean,
    viewModel: AboutViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val showUpdateDialog = remember { mutableStateOf(false) }
    var showLoadingDialog by remember { mutableStateOf(false) }
    val showUpdateErrorSheet = remember { mutableStateOf(false) }
    var updateErrorInfo by remember { mutableStateOf<AboutEvent.ShowInAppUpdateErrorDetail?>(null) }

    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    LogEventCollector(viewModel)

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.ShowUpdateLoading -> showLoadingDialog = true
                is AboutEvent.HideUpdateLoading -> showLoadingDialog = false
                is AboutEvent.ShowInAppUpdateErrorDetail -> {
                    showLoadingDialog = false
                    updateErrorInfo = event
                    showUpdateErrorSheet.value = true
                }

                else -> {}
            }
        }
    }

    val level = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    val versionInfoText = stringResource(
        id = R.string.app_version_info_format,
        level,
        AppConfig.VERSION_NAME,
        AppConfig.VERSION_CODE,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.about)) },
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
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.appIcon?.let { icon ->
                        Image(
                            bitmap = icon,
                            modifier = Modifier.size(84.dp),
                            contentDescription = stringResource(id = R.string.app_name),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = CouiTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = versionInfoText,
                        fontSize = 14.sp,
                        color = CouiTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (uiState.hasUpdate) {
                        CouiBadge(
                            text = stringResource(R.string.update_available, uiState.remoteVersion),
                        )
                    }
                }
            }

            item {
                Group(title = stringResource(R.string.about)) {
                    CouiNavigationItem(
                        title = stringResource(R.string.get_source_code),
                        description = stringResource(R.string.get_source_code_detail),
                        onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") },
                    )
                    Divider()
                    CouiNavigationItem(
                        title = stringResource(R.string.open_source_license),
                        description = stringResource(R.string.open_source_license_settings_description),
                        onClick = { navigator.push(Route.OpenSourceLicense) },
                    )
                    Divider()
                    CouiNavigationItem(
                        title = stringResource(R.string.get_update),
                        description = stringResource(R.string.get_update_detail),
                        onClick = { showUpdateDialog.value = true },
                    )
                    if (uiState.hasUpdate) {
                        Divider()
                        CouiNavigationItem(
                            title = stringResource(R.string.get_update_directly),
                            description = stringResource(R.string.get_update_directly_desc),
                            onClick = { viewModel.dispatch(AboutAction.PerformUpdate) },
                        )
                    }
                }
            }

            if (AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID) {
                item {
                    Group(title = stringResource(R.string.debug)) {
                        CouiSwitchItem(
                            title = stringResource(R.string.save_logs),
                            description = stringResource(R.string.save_logs_desc),
                            checked = uiState.enableFileLogging,
                            onCheckedChange = { viewModel.dispatch(AboutAction.SetEnableFileLogging(it)) },
                        )
                        AnimatedVisibility(
                            visible = uiState.enableFileLogging,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column {
                                Divider()
                                CouiBasicItem(
                                    title = stringResource(R.string.export_logs),
                                    summary = stringResource(R.string.export_logs_desc),
                                    onClick = { viewModel.dispatch(AboutAction.ShareLog) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    CouiUpdateDialog(
        showState = showUpdateDialog,
        onDismiss = { showUpdateDialog.value = false },
    )

    CouiBlockingLoadingDialog(
        visible = showLoadingDialog,
        message = stringResource(R.string.updating),
    )

    val currentError = updateErrorInfo
    CouiErrorDisplaySheet(
        title = currentError?.title ?: stringResource(R.string.installer_unknown_error),
        showState = showUpdateErrorSheet,
        exception = currentError?.exception ?: RuntimeException(),
        onDismissRequest = { showUpdateErrorSheet.value = false },
    )
}
