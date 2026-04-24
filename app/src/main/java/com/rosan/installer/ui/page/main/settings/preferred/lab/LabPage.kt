// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.preferred.lab

import android.os.Build
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.GithubUpdateChannel
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.main.settings.preferred.LabHttpProfileWidget
import com.rosan.installer.ui.page.main.settings.preferred.LabRootImplementationWidget
import com.rosan.installer.ui.page.main.widget.card.InfoTipCard
import com.rosan.installer.ui.page.main.widget.dialog.CustomGithubProxyUrlDialog
import com.rosan.installer.ui.page.main.widget.dialog.GithubUpdateChannelSelectionDialog
import com.rosan.installer.ui.page.main.widget.dialog.RootImplementationSelectionDialog
import com.rosan.installer.ui.page.main.widget.setting.AppBackButton
import com.rosan.installer.ui.page.main.widget.setting.BaseWidget
import com.rosan.installer.ui.page.main.widget.setting.SegmentedColumn
import com.rosan.installer.ui.page.main.widget.setting.SwitchWidget
import com.rosan.installer.ui.theme.getMaterial3AppBarColor
import com.rosan.installer.ui.theme.installerMaterial3BlurEffect
import com.rosan.installer.ui.theme.rememberMaterial3BlurBackdrop
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LabPage(
    useBlur: Boolean,
    viewModel: LabSettingsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val showRootImplementationDialog = remember { mutableStateOf(false) }
    val showChannelDialog = remember { mutableStateOf(false) }
    val showCustomProxyDialog = remember { mutableStateOf(false) }

    if (showChannelDialog.value)
        GithubUpdateChannelSelectionDialog(
            currentSelection = uiState.githubUpdateChannel,
            onDismiss = { showChannelDialog.value = false },
            onConfirm = { channel ->
                showChannelDialog.value = false
                viewModel.dispatch(LabSettingsAction.LabChangeGithubUpdateChannel(channel))
                if (channel == GithubUpdateChannel.CUSTOM)
                    showCustomProxyDialog.value = true
            }
        )

    if (showCustomProxyDialog.value)
        CustomGithubProxyUrlDialog(
            initialUrl = uiState.customGithubProxyUrl,
            onDismiss = {
                showCustomProxyDialog.value = false
                if (uiState.customGithubProxyUrl.isEmpty())
                    viewModel.dispatch(LabSettingsAction.LabChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL))
            },
            onConfirm = { url ->
                showCustomProxyDialog.value = false
                viewModel.dispatch(LabSettingsAction.LabChangeCustomGithubProxyUrl(url))
                if (url.isEmpty())
                    viewModel.dispatch(LabSettingsAction.LabChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL))
            }
        )

    if (showRootImplementationDialog.value) {
        RootImplementationSelectionDialog(
            currentSelection = uiState.labRootMode,
            onDismiss = { showRootImplementationDialog.value = false },
            onConfirm = { selectedImplementation ->
                showRootImplementationDialog.value = false
                // 1. Save the selected implementation
                viewModel.dispatch(
                    LabSettingsAction.LabChangeRootImplementation(
                        selectedImplementation
                    )
                )
                // 2. Enable the flash module feature
                viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
            }
        )
    }

    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()

    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

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
                    Text(stringResource(R.string.lab))
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
        }
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
            item { InfoTipCard(text = stringResource(R.string.lab_tip)) }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.config_authorizer_root)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Root,
                            title = stringResource(R.string.lab_module_flashing),
                            description = stringResource(R.string.lab_module_flashing_desc),
                            checked = uiState.labRootEnableModuleFlash,
                            onCheckedChange = { isChecking ->
                                if (isChecking) {
                                    showRootImplementationDialog.value = true
                                } else {
                                    viewModel.dispatch(
                                        LabSettingsAction.LabChangeRootModuleFlash(
                                            false
                                        )
                                    )
                                }
                            }
                        )
                    }
                    item(visible = uiState.labRootEnableModuleFlash) {
                        LabRootImplementationWidget(viewModel)
                    }
                    item(visible = uiState.labRootEnableModuleFlash) {
                        SwitchWidget(
                            icon = AppIcons.Terminal,
                            title = stringResource(R.string.lab_module_flashing_show_art),
                            description = stringResource(R.string.lab_module_flashing_show_art_desc),
                            checked = uiState.labRootShowModuleArt,
                            onCheckedChange = {
                                viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it))
                            }
                        )
                    }
                }
            }
            item {
                SegmentedColumn(
                    title = stringResource(R.string.lab_unstable_features)
                ) {
                    item {
                        SwitchWidget(
                            icon = AppIcons.Share,
                            title = stringResource(R.string.lab_tap_icon_to_share),
                            description = stringResource(R.string.lab_tap_icon_to_share_desc),
                            checked = uiState.labTapIconToShare,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    LabSettingsAction.LabChangeTapIconToShare(
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item {
                        SwitchWidget(
                            icon = AppIcons.InstallRequester,
                            title = stringResource(R.string.lab_set_install_requester),
                            description = stringResource(R.string.lab_set_install_requester_desc),
                            checked = uiState.labSetInstallRequester,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    LabSettingsAction.LabChangeSetInstallRequester(
                                        it
                                    )
                                )
                            }
                        )
                    }
                    if (!capabilityProvider.isSystemApp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) item {
                        SwitchWidget(
                            icon = AppIcons.InstallSilent,
                            title = stringResource(R.string.lab_install_without_user_action),
                            description = stringResource(R.string.lab_install_without_user_action_desc),
                            checked = uiState.labAllowInstallWithoutUserAction,
                            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeAllowInstallWithoutUserAction(it)) }
                        )
                    }
                }
            }

            if (AppConfig.isInternetAccessEnabled)
                item {
                    SegmentedColumn(
                        title = stringResource(R.string.internet_access_enabled)
                    ) {
                        /*item {
                            SwitchWidget(
                                icon = Icons.Default.Download,
                                title = stringResource(R.string.lab_http_save_file),
                                description = stringResource(R.string.lab_http_save_file_desc),
                                checked = uiState.labHttpSaveFile,
                                isM3E = false,
                                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeHttpSaveFile(it)) }
                            )
                        }*/
                        item { LabHttpProfileWidget(viewModel) }

                        val currentChannel = uiState.githubUpdateChannel
                        item {
                            val channelSummary = when (currentChannel) {
                                GithubUpdateChannel.OFFICIAL -> stringResource(R.string.lab_update_github_proxy_official)
                                GithubUpdateChannel.PROXY_7ED -> stringResource(R.string.lab_update_github_proxy_7ed)
                                GithubUpdateChannel.CUSTOM -> uiState.customGithubProxyUrl.ifBlank {
                                    stringResource(R.string.lab_update_github_proxy_custom)
                                }
                            }
                            BaseWidget(
                                title = stringResource(R.string.lab_update_github_proxy),
                                description = channelSummary,
                                onClick = { showChannelDialog.value = true }
                            ) {}
                        }
                    }
                }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
