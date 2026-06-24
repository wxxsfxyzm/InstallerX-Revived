// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
@file:OptIn(ExperimentalMaterial3Api::class)

package com.rosan.installer.ui.page.main.settings.home.installer

import android.annotation.SuppressLint
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewEvent
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.settings.home.delayDefaultInstallerProgressIfNeeded
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.card.TitleTipCard
import com.rosan.installer.ui.page.main.widget.dialog.ErrorDisplayDialog
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.BlockingLoadingIndicator
import com.rosan.installer.ui.page.main.widget.setting.ExpressiveBackButton
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
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

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    var errorDialogInfo by remember {
        mutableStateOf<HomePageViewEvent.ShowDefaultInstallerErrorDetail?>(null)
    }
    var showDefaultInstallerProgress by remember { mutableStateOf(false) }
    var defaultInstallerProgressStartedAt by remember { mutableStateOf(0L) }
    var defaultInstallerProgressTextResId by remember {
        mutableStateOf(R.string.locking_default_installer)
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

    @SuppressLint("LocalContextGetResourceValueCall") LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomePageViewEvent.ShowDefaultInstallerResult -> {
                    dismissDefaultInstallerProgress()
                    Toast.makeText(
                        context,
                        context.getString(event.messageResId),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is HomePageViewEvent.ShowDefaultInstallerErrorDetail -> {
                    dismissDefaultInstallerProgress()
                    errorDialogInfo = event
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
                        ExpressiveBackButton { navigator.pop() }
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = paddingValues
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
                        SwitchWidget(
                            icon = AppIcons.AutoLockDefault,
                            title = stringResource(R.string.auto_lock_default_installer),
                            description = stringResource(R.string.auto_lock_default_installer_desc),
                            checked = uiState.autoLockInstaller,
                            enabled = uiState.globalAuthorizer != Authorizer.None
                        ) { viewModel.dispatch(HomePageViewAction.ChangeAutoLockInstaller(it)) }
                    }
                    item {
                        BaseWidget(
                            icon = AppIcons.LockDefault,
                            title = stringResource(R.string.lock_default_installer),
                            description = stringResource(R.string.lock_default_installer_desc),
                            enabled = uiState.globalAuthorizer != Authorizer.None,
                            onClick = { dispatchSetDefaultInstaller(true) }
                        )
                    }
                    item {
                        BaseWidget(
                            icon = AppIcons.UnlockDefault,
                            title =
                                stringResource(R.string.unlock_default_installer),
                            description =
                                stringResource(R.string.unlock_default_installer_desc),
                            enabled = uiState.globalAuthorizer != Authorizer.None,
                            onClick = { dispatchSetDefaultInstaller(false) }
                        )
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
                                icon = AppIcons.LSPosed,
                                title = stringResource(R.string.setting_lsposed_module_title),
                                description = stringResource(R.string.setting_lsposed_module_desc),
                                checked = uiState.userSetLSPosedActive,
                                onCheckedChange = {
                                    viewModel.dispatch(
                                        HomePageViewAction.ChangeUserSetLSPosedActive(
                                            it
                                        )
                                    )
                                }
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

        }
    }

    errorDialogInfo?.let { dialogInfo ->
        ErrorDisplayDialog(
            exception = dialogInfo.exception,
            onDismissRequest = { errorDialogInfo = null },
            onRetry = {
                errorDialogInfo = null
                val action = dialogInfo.retryAction
                if (action is HomePageViewAction.SetDefaultInstaller) {
                    dispatchSetDefaultInstaller(action.lock)
                } else {
                    viewModel.dispatch(action)
                }
            },
            title = stringResource(dialogInfo.titleResId)
        )
    }

    BlockingLoadingIndicator(
        visible = showDefaultInstallerProgress,
        text = stringResource(defaultInstallerProgressTextResId),
        backdrop = backdrop
    )
}
