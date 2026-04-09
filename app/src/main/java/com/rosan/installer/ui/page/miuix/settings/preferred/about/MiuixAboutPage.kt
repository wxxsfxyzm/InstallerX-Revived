// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.preferred.about

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.Level
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutAction
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.ui.page.main.widget.util.LogEventCollector
import com.rosan.installer.ui.page.miuix.settings.preferred.MiuixNavigationItemWidget
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.page.miuix.widgets.MiuixUpdateDialog
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerHazeEffect
import com.rosan.installer.ui.theme.rememberMiuixHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun MiuixAboutPage(
    viewModel: AboutViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = if (uiState.useBlur) remember { HazeState() } else null
    val hazeStyle = rememberMiuixHazeStyle()
    val showUpdateDialog = remember { mutableStateOf(false) }

    val internetAccessHint = if (AppConfig.isInternetAccessEnabled) stringResource(R.string.internet_access_enabled)
    else stringResource(R.string.internet_access_disabled)

    val level = when (AppConfig.LEVEL) {
        Level.STABLE -> stringResource(id = R.string.stable)
        Level.PREVIEW -> stringResource(id = R.string.preview)
        Level.UNSTABLE -> stringResource(id = R.string.unstable)
    }

    val versionInfoText = stringResource(
        id = R.string.app_version_info_format,
        internetAccessHint,
        level,
        AppConfig.VERSION_NAME,
        AppConfig.VERSION_CODE
    )

    MiuixUpdateDialog(
        showState = showUpdateDialog,
        onDismiss = { showUpdateDialog.value = false }
    )

    val showLoadingDialog = remember { mutableStateOf(false) }
    val showUpdateErrorDialog = remember { mutableStateOf(false) }
    var updateErrorInfo by remember { mutableStateOf<AboutEvent.ShowInAppUpdateErrorDetail?>(null) }

    LogEventCollector(viewModel)

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.ShowUpdateLoading -> {
                    showLoadingDialog.value = true
                }

                is AboutEvent.HideUpdateLoading -> {
                    showLoadingDialog.value = false
                }

                is AboutEvent.ShowInAppUpdateErrorDetail -> {
                    showLoadingDialog.value = false
                    updateErrorInfo = event
                    showUpdateErrorDialog.value = true
                }

                else -> {}
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerHazeEffect(hazeState, hazeStyle),
                color = hazeState.getMiuixAppBarColor(),
                title = stringResource(id = R.string.about),
                scrollBehavior = scrollBehavior,
                navigationIcon = { MiuixBackButton(onClick = { navigator.pop() }) }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeState?.let { Modifier.hazeSource(it) } ?: Modifier)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.appIcon != null) {
                        Image(
                            bitmap = uiState.appIcon!!,
                            modifier = Modifier.size(80.dp),
                            contentDescription = stringResource(id = R.string.app_name)
                        )
                    } else {
                        Box(modifier = Modifier.size(80.dp))
                    }
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MiuixTheme.textStyles.title2,
                    )
                    Text(
                        text = versionInfoText,
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (uiState.hasUpdate)
                        Text(
                            text = stringResource(R.string.update_available, uiState.remoteVersion),
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.primary
                        )
                    Spacer(modifier = Modifier.size(12.dp))
                }
            }
            item { Spacer(modifier = Modifier.size(12.dp)) }
            item { SmallTitle(stringResource(R.string.about)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.get_source_code),
                        description = stringResource(R.string.get_source_code_detail),
                        onClick = { uriHandler.openUri("https://github.com/wxxsfxyzm/InstallerX-Revived") }
                    )
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.open_source_license),
                        description = stringResource(R.string.open_source_license_settings_description),
                        onClick = { navigator.push(Route.OpenSourceLicense) }
                    )
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.get_update),
                        description = stringResource(R.string.get_update_detail),
                        onClick = { showUpdateDialog.value = true }
                    )
                    if (uiState.hasUpdate)
                        MiuixNavigationItemWidget(
                            title = stringResource(R.string.get_update_directly),
                            description = stringResource(R.string.get_update_directly_desc),
                            onClick = { viewModel.dispatch(AboutAction.PerformUpdate) }
                        )
                }
            }
            if (AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID) {
                item { SmallTitle(stringResource(R.string.debug)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.save_logs),
                            description = stringResource(R.string.save_logs_desc),
                            checked = uiState.enableFileLogging,
                            onCheckedChange = { viewModel.dispatch(AboutAction.SetEnableFileLogging(it)) }
                        )
                        AnimatedVisibility(
                            visible = uiState.enableFileLogging,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            BasicComponent(
                                title = stringResource(R.string.export_logs),
                                summary = stringResource(R.string.export_logs_desc),
                                onClick = { viewModel.dispatch(AboutAction.ShareLog) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    WindowDialog(
        show = showLoadingDialog.value
    ) {
        BackHandler { }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfiniteProgressIndicator()
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.updating)
            )
        }
    }

    updateErrorInfo?.let { sheetInfo ->
        ErrorDisplaySheet(
            title = sheetInfo.title,
            showState = showUpdateErrorDialog,
            exception = sheetInfo.exception,
            onDismissRequest = {
                showUpdateErrorDialog.value = false
                updateErrorInfo = null
            }
        )
    }
}
