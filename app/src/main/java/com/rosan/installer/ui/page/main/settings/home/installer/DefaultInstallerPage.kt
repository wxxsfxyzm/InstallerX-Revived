// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.home.installer

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.twotone.Healing
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewEvent
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.settings.preferred.AutoLockInstaller
import com.rosan.installer.ui.page.main.settings.preferred.DefaultInstaller
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.card.TitleTipCard
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.page.main.widget.snackbar.SwipeableSnackbarHost
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DefaultInstallerPage(
    useBlur: Boolean,
    viewModel: HomePageViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    val snackBarHostState = remember { SnackbarHostState() }
    var errorDialogInfo by remember {
        mutableStateOf<HomePageViewEvent.ShowDefaultInstallerErrorDetail?>(null)
    }

    val detailLabel = stringResource(id = R.string.details)

    @SuppressLint("LocalContextGetResourceValueCall") LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            snackBarHostState.currentSnackbarData?.dismiss()
            when (event) {
                is HomePageViewEvent.ShowDefaultInstallerResult -> {
                    snackBarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is HomePageViewEvent.ShowDefaultInstallerErrorDetail -> {
                    val snackbarResult = snackBarHostState.showSnackbar(
                        message = context.getString(event.titleResId),
                        actionLabel = detailLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        errorDialogInfo = event
                    }
                }
            }
        }
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
                title = {
                    Text(stringResource(R.string.default_installer))
                },
                navigationIcon = {
                    Row {
                        AppBackButton(
                            onClick = { navigator.pop() },
                            icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                            modifier = Modifier.size(36.dp),
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
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
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackBarHostState
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = PaddingValues(
                start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            when {
                uiState.isSystemApp -> item { InfoTipCard(stringResource(R.string.config_authorizer_none_system_app_tips)) }
                uiState.globalAuthorizer == Authorizer.None -> item { InfoTipCard(stringResource(R.string.config_authorizer_none_tips)) }
                else -> item {
                    TitleTipCard(
                        title = stringResource(R.string.priv_page_what_is_this_title),
                        text = stringResource(R.string.working_status_default_installer_tip)
                    )
                }
            }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.basic)
                ) {
                    item {
                        AutoLockInstaller(
                            checked = uiState.autoLockInstaller,
                            enabled = uiState.globalAuthorizer != Authorizer.None,
                            onCheckedChange = {
                                viewModel.dispatch(HomePageViewAction.ChangeAutoLockInstaller(it))
                            }
                        )
                    }
                    item {
                        DefaultInstaller(
                            lock = true,
                            enabled = uiState.globalAuthorizer != Authorizer.None
                        ) { viewModel.dispatch(HomePageViewAction.SetDefaultInstaller(true)) }
                    }
                    item {
                        DefaultInstaller(
                            lock = false,
                            enabled = uiState.globalAuthorizer != Authorizer.None
                        ) { viewModel.dispatch(HomePageViewAction.SetDefaultInstaller(false)) }
                    }
                }
            }

            if (!uiState.isSystemApp) {
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.home_label_lsp)
                    ) {
                        item {
                            SwitchWidget(
                                icon = Icons.TwoTone.Healing,
                                title = stringResource(R.string.setting_lsposed_module_title),
                                description = stringResource(R.string.setting_lsposed_module_desc),
                                checked = uiState.userSetLSPosedActive,
                                onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeUserSetLSPosedActive(it)) }
                            )
                        }
                    }
                }

                item {
                    val uriHandler = LocalUriHandler.current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.home_label_lsp_link),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.home_lsp_inxlocker_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://github.com/Chimioo/InxLocker")
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    errorDialogInfo?.let { dialogInfo ->
        ErrorDisplayDialog(
            exception = dialogInfo.exception,
            onDismissRequest = { errorDialogInfo = null },
            onRetry = {
                errorDialogInfo = null
                viewModel.dispatch(dialogInfo.retryAction)
            },
            title = stringResource(dialogInfo.titleResId)
        )
    }
}
