// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.miuix.settings.home.installer

import android.annotation.SuppressLint
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.Authorizer
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Navigator
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewEvent
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.miuix.widgets.ErrorDisplaySheet
import com.rosan.installer.ui.page.miuix.widgets.MiuixBackButton
import com.rosan.installer.ui.page.miuix.widgets.MiuixSettingsTipCard
import com.rosan.installer.ui.page.miuix.widgets.MiuixSwitchWidget
import com.rosan.installer.ui.theme.getMiuixAppBarColor
import com.rosan.installer.ui.theme.installerMiuixBlurEffect
import com.rosan.installer.ui.theme.rememberMiuixBlurBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixDefaultInstallerPage(
    useBlur: Boolean,
    navigator: Navigator = LocalNavigator.current,
    viewModel: HomePageViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val backdrop = rememberMiuixBlurBackdrop(useBlur)

    val snackbarHostState = remember { SnackbarHostState() }
    var errorSheetInfo by remember {
        mutableStateOf<HomePageViewEvent.ShowDefaultInstallerErrorDetail?>(null)
    }
    val showErrorSheet = remember { mutableStateOf(false) }

    @SuppressLint("LocalContextGetResourceValueCall") LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomePageViewEvent.ShowDefaultInstallerResult -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }

                is HomePageViewEvent.ShowDefaultInstallerErrorDetail -> {
                    errorSheetInfo = event
                    showErrorSheet.value = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = stringResource(R.string.default_installer),
                navigationIcon = {
                    MiuixBackButton(onClick = { navigator.pop() })
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(state = snackbarHostState) }
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
                val tip = when {
                    uiState.isSystemApp -> stringResource(R.string.config_authorizer_none_system_app_tips)
                    uiState.globalAuthorizer == Authorizer.None -> stringResource(R.string.config_authorizer_none_tips)
                    else -> stringResource(R.string.working_status_default_installer_tip)
                }
                MiuixSettingsTipCard(text = tip)
            }

            item { SmallTitle(stringResource(R.string.basic)) }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    MiuixAutoLockInstaller(
                        checked = uiState.autoLockInstaller,
                        enabled = uiState.globalAuthorizer != Authorizer.None,
                        onCheckedChange = {
                            viewModel.dispatch(HomePageViewAction.ChangeAutoLockInstaller(it))
                        }
                    )
                    MiuixDefaultInstaller(
                        lock = true,
                        enabled = uiState.globalAuthorizer != Authorizer.None
                    ) { viewModel.dispatch(HomePageViewAction.SetDefaultInstaller(true)) }
                    MiuixDefaultInstaller(
                        lock = false,
                        enabled = uiState.globalAuthorizer != Authorizer.None
                    ) { viewModel.dispatch(HomePageViewAction.SetDefaultInstaller(false)) }
                }
            }

            if (!uiState.isSystemApp) {
                item { SmallTitle(stringResource(R.string.home_label_lsp)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.setting_lsposed_module_title),
                            description = stringResource(R.string.setting_lsposed_module_desc),
                            checked = uiState.userSetLSPosedActive,
                            onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeUserSetLSPosedActive(it)) }
                        )
                    }
                }

                item { SmallTitle(stringResource(R.string.home_label_lsp_link)) }
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.home_lsp_inxlocker_title),
                            titleColor = BasicComponentColors(
                                color = MiuixTheme.colorScheme.primary,
                                disabledColor = MiuixTheme.colorScheme.disabledOnSecondaryVariant
                            ),
                            onClick = { uriHandler.openUri("https://github.com/Chimioo/InxLocker") }
                        )
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    errorSheetInfo?.let { info ->
        ErrorDisplaySheet(
            showState = showErrorSheet,
            exception = info.exception,
            onDismissRequest = { showErrorSheet.value = false },
            onRetry = {
                showErrorSheet.value = false
                viewModel.dispatch(info.retryAction)
            },
            title = stringResource(info.titleResId)
        )
    }
}

@Composable
private fun MiuixAutoLockInstaller(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MiuixSwitchWidget(
        title = stringResource(R.string.auto_lock_default_installer),
        description = stringResource(R.string.auto_lock_default_installer_desc),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun MiuixDefaultInstaller(
    lock: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    BasicComponent(
        title = stringResource(
            if (lock) R.string.lock_default_installer else R.string.unlock_default_installer
        ),
        summary = stringResource(
            if (lock) R.string.lock_default_installer_desc else R.string.unlock_default_installer_desc
        ),
        enabled = enabled,
        onClick = onClick
    )
}
