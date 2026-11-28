// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.home.installer

import android.annotation.SuppressLint
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Item
import com.itos.coui.layout.Scaffold
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiBlockingLoadingDialog
import com.rosan.installer.ui.page.coui.widgets.CouiErrorDisplaySheet
import com.rosan.installer.ui.page.coui.widgets.CouiSettingsTipCard
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewEvent
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.settings.home.delayDefaultInstallerProgressIfNeeded
import org.koin.androidx.compose.koinViewModel
import com.itos.coui.text.Text as CouiText

@Composable
fun CouiDefaultInstallerPage(useBlur: Boolean, viewModel: HomePageViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    var errorSheetInfo by remember {
        mutableStateOf<HomePageViewEvent.ShowDefaultInstallerErrorDetail?>(null)
    }
    val showErrorSheet = remember { mutableStateOf(false) }
    var showDefaultInstallerProgress by remember { mutableStateOf(false) }
    var defaultInstallerProgressStartedAt by remember { mutableLongStateOf(0L) }
    var defaultInstallerProgressTextResId by remember {
        mutableIntStateOf(R.string.locking_default_installer)
    }

    fun dispatchSetDefaultInstaller(lock: Boolean) {
        defaultInstallerProgressStartedAt = SystemClock.elapsedRealtime()
        defaultInstallerProgressTextResId =
            if (lock) R.string.locking_default_installer else R.string.unlocking_default_installer
        showDefaultInstallerProgress = true
        viewModel.dispatch(HomePageViewAction.SetDefaultInstaller(lock))
    }

    suspend fun dismissDefaultInstallerProgress() {
        if (showDefaultInstallerProgress) {
            delayDefaultInstallerProgressIfNeeded(defaultInstallerProgressStartedAt)
            showDefaultInstallerProgress = false
        }
    }

    @SuppressLint("LocalContextGetResourceValueCall")
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomePageViewEvent.ShowDefaultInstallerResult -> {
                    dismissDefaultInstallerProgress()
                    Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_SHORT).show()
                }

                is HomePageViewEvent.ShowDefaultInstallerErrorDetail -> {
                    dismissDefaultInstallerProgress()
                    errorSheetInfo = event
                    showErrorSheet.value = true
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
                title = { Text(text = stringResource(R.string.default_installer)) },
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
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                val tip = when {
                    uiState.isSystemApp -> stringResource(R.string.config_authorizer_none_system_app_tips)
                    uiState.globalAuthorizer == Authorizer.None -> stringResource(R.string.config_authorizer_none_tips)
                    else -> stringResource(R.string.working_status_default_installer_tip)
                }
                CouiSettingsTipCard(text = tip)
            }

            item {
                Group(title = stringResource(R.string.basic)) {
                    val defaultInstallerActionsEnabled =
                        uiState.globalAuthorizer != Authorizer.None && !uiState.userSetLSPosedActive
                    CouiBasicItem(
                        title = stringResource(R.string.lock_default_installer),
                        summary = stringResource(R.string.lock_default_installer_desc),
                        enabled = defaultInstallerActionsEnabled,
                        onClick = { dispatchSetDefaultInstaller(true) },
                    )
                    Divider()
                    CouiBasicItem(
                        title = stringResource(R.string.unlock_default_installer),
                        summary = stringResource(R.string.unlock_default_installer_desc),
                        enabled = defaultInstallerActionsEnabled,
                        onClick = { dispatchSetDefaultInstaller(false) },
                    )
                }
            }

            if (!uiState.isSystemApp) {
                item {
                    Group(title = stringResource(R.string.advanced)) {
                        CouiSwitchItem(
                            title = stringResource(R.string.setting_lsposed_module_title),
                            description = stringResource(R.string.setting_lsposed_module_desc),
                            checked = uiState.userSetLSPosedActive,
                            onCheckedChange = {
                                viewModel.dispatch(HomePageViewAction.ChangeUserSetLSPosedActive(it))
                            },
                        )
                    }
                }

                item {
                    Group(title = stringResource(R.string.home_label_lsp_link)) {
                        Item(
                            onClick = { uriHandler.openUri("https://github.com/Chimioo/InxLocker") },
                        ) {
                            CouiText(
                                text = stringResource(R.string.home_lsp_inxlocker_title),
                                kind = CouiTextKind.ItemTitle,
                                color = CouiTheme.colorScheme.accentOnSurface,
                            )
                        }
                    }
                }
            }
        }
    }

    val currentError = errorSheetInfo
    CouiErrorDisplaySheet(
        showState = showErrorSheet,
        exception = currentError?.exception ?: RuntimeException(),
        onDismissRequest = { showErrorSheet.value = false },
        onRetry = currentError?.let { info ->
            {
                showErrorSheet.value = false
                val action = info.retryAction
                if (action is HomePageViewAction.SetDefaultInstaller) {
                    dispatchSetDefaultInstaller(action.lock)
                } else {
                    viewModel.dispatch(action)
                }
            }
        },
        title = currentError?.let { stringResource(it.titleResId) } ?: stringResource(R.string.installer_unknown_error),
    )

    CouiBlockingLoadingDialog(
        visible = showDefaultInstallerProgress,
        message = stringResource(defaultInstallerProgressTextResId),
    )
}
