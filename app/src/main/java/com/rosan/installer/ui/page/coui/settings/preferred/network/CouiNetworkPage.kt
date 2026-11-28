// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.coui.settings.preferred.network

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.Dialog
import com.itos.coui.layout.Divider
import com.itos.coui.layout.Group
import com.itos.coui.layout.Scaffold
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.LargeTopAppBar
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.preferences.NetworkAppNameSource
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.page.coui.widgets.CouiBackButton
import com.rosan.installer.ui.page.coui.widgets.CouiBasicItem
import com.rosan.installer.ui.page.coui.widgets.CouiCustomGithubProxyUrlDialog
import com.rosan.installer.ui.page.coui.widgets.CouiGithubUpdateChannelSelectionDialog
import com.rosan.installer.ui.page.coui.widgets.CouiSpinnerItem
import com.rosan.installer.ui.page.coui.widgets.CouiSwitchItem
import com.rosan.installer.ui.page.coui.widgets.rememberCouiDialogMaterial
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CouiNetworkPage(useBlur: Boolean, viewModel: NetworkSettingsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior {
        listState.canScrollForward || listState.canScrollBackward
    }
    val density = LocalDensity.current

    val showChannelDialog = remember { mutableStateOf(false) }
    val showCustomProxyDialog = remember { mutableStateOf(false) }
    var pendingNetworkSourceMode by rememberSaveable { mutableStateOf<NetworkSourceMode?>(null) }
    var exitAfterInternetDisable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(exitAfterInternetDisable, uiState.allowInternetAccess) {
        if (exitAfterInternetDisable && !uiState.allowInternetAccess) {
            navigator.pop()
        }
    }

    val backdrop = rememberCouiWindowBackdrop()
    val dialogMaterial = rememberCouiDialogMaterial()

    Dialog(
        visible = pendingNetworkSourceMode != null,
        onDismissRequest = { pendingNetworkSourceMode = null },
        title = stringResource(R.string.network_source_mode_warning_title),
        message = stringResource(R.string.network_source_mode_warning_desc),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.confirm),
            onClick = {
                val mode = pendingNetworkSourceMode
                pendingNetworkSourceMode = null
                if (mode != null) {
                    viewModel.dispatch(NetworkSettingsAction.ConfirmNetworkSourceMode(mode))
                }
            },
            style = CouiDialogActionStyle.Primary,
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.cancel),
            onClick = { pendingNetworkSourceMode = null },
        ),
        material = dialogMaterial,
        backdrop = backdrop,
    )

    CouiGithubUpdateChannelSelectionDialog(
        showState = showChannelDialog,
        currentSelection = uiState.githubUpdateChannel,
        onDismiss = { showChannelDialog.value = false },
        onConfirm = { channel ->
            showChannelDialog.value = false
            viewModel.dispatch(NetworkSettingsAction.ChangeGithubUpdateChannel(channel))
            if (channel == GithubUpdateChannel.CUSTOM) {
                showCustomProxyDialog.value = true
            }
        },
    )

    CouiCustomGithubProxyUrlDialog(
        showState = showCustomProxyDialog,
        initialUrl = uiState.customGithubProxyUrl,
        onDismiss = {
            showCustomProxyDialog.value = false
            if (uiState.customGithubProxyUrl.isEmpty()) {
                viewModel.dispatch(
                    NetworkSettingsAction.ChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL),
                )
            }
        },
        onConfirm = { url ->
            showCustomProxyDialog.value = false
            viewModel.dispatch(NetworkSettingsAction.ChangeCustomGithubProxyUrl(url))
            if (url.isEmpty()) {
                viewModel.dispatch(
                    NetworkSettingsAction.ChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL),
                )
            }
        },
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CouiTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.network_settings)) },
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
                Group {
                    CouiSwitchItem(
                        title = stringResource(R.string.network_access),
                        checked = uiState.allowInternetAccess,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                exitAfterInternetDisable = true
                            }
                            viewModel.dispatch(NetworkSettingsAction.ChangeInternetAccess(enabled))
                        },
                    )
                }
            }

            if (uiState.allowInternetAccess) {
                item {
                    Group(title = stringResource(R.string.internet_access_enabled)) {
                        CouiNetworkSourceModePreference(
                            currentMode = uiState.networkSourceMode,
                            onModeChange = { mode ->
                                if (mode != NetworkSourceMode.Cache &&
                                    !uiState.networkSourceModeWarningAcknowledged
                                ) {
                                    pendingNetworkSourceMode = mode
                                } else {
                                    viewModel.dispatch(NetworkSettingsAction.ChangeNetworkSourceMode(mode))
                                }
                            },
                        )

                        if (uiState.networkSourceMode == NetworkSourceMode.Automatic) {
                            Divider()
                            CouiNetworkAppNameSourcePreference(
                                source = uiState.networkAppNameSource,
                                onChange = { viewModel.dispatch(NetworkSettingsAction.ChangeAppNameSource(it)) },
                            )
                            Divider()
                            CouiSwitchItem(
                                title = stringResource(R.string.network_load_app_icon),
                                description = stringResource(R.string.network_load_app_icon_desc),
                                checked = uiState.networkLoadAppIcon,
                                onCheckedChange = { viewModel.dispatch(NetworkSettingsAction.ChangeLoadAppIcon(it)) },
                            )
                        }

                        Divider()

                        val allowSecureString = stringResource(R.string.lab_http_profile_secure)
                        val allowLocalString = stringResource(R.string.lab_http_profile_local)
                        val allowAllString = stringResource(R.string.lab_http_profile_all)
                        val profileData = remember(allowSecureString, allowLocalString, allowAllString) {
                            linkedMapOf(
                                HttpProfile.ALLOW_SECURE to allowSecureString,
                                HttpProfile.ALLOW_LOCAL to allowLocalString,
                                HttpProfile.ALLOW_ALL to allowAllString,
                            )
                        }
                        val profileKeys = profileData.keys.toList()
                        val profileIndex = profileKeys.indexOf(uiState.httpProfile).coerceAtLeast(0)

                        CouiSpinnerItem(
                            title = stringResource(R.string.lab_http_profile),
                            items = profileData.values.toList(),
                            selectedIndex = profileIndex,
                            onSelectedIndexChange = { newIndex ->
                                profileKeys.getOrNull(newIndex)?.let { profile ->
                                    viewModel.dispatch(NetworkSettingsAction.ChangeHttpProfile(profile))
                                }
                            },
                        )

                        Divider()

                        val channelSummary = when (uiState.githubUpdateChannel) {
                            GithubUpdateChannel.OFFICIAL -> stringResource(R.string.lab_update_github_proxy_official)
                            GithubUpdateChannel.PROXY_7ED -> stringResource(R.string.lab_update_github_proxy_7ed)
                            GithubUpdateChannel.CUSTOM -> uiState.customGithubProxyUrl.ifBlank {
                                stringResource(R.string.lab_update_github_proxy_custom)
                            }
                        }
                        CouiBasicItem(
                            title = stringResource(R.string.lab_update_github_proxy),
                            summary = channelSummary,
                            onClick = { showChannelDialog.value = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CouiNetworkAppNameSourcePreference(
    source: NetworkAppNameSource,
    onChange: (NetworkAppNameSource) -> Unit,
) {
    CouiSpinnerItem(
        title = stringResource(R.string.network_app_name_source),
        summary = stringResource(
            if (source == NetworkAppNameSource.ServerFilename) {
                R.string.network_app_name_server_desc
            } else {
                R.string.network_app_name_resources_desc
            },
        ),
        items = listOf(
            stringResource(R.string.network_app_name_server),
            stringResource(R.string.network_app_name_resources),
        ),
        selectedIndex = NetworkAppNameSource.entries.indexOf(source).coerceAtLeast(0),
        onSelectedIndexChange = { NetworkAppNameSource.entries.getOrNull(it)?.let(onChange) },
    )
}

@Composable
private fun CouiNetworkSourceModePreference(
    currentMode: NetworkSourceMode,
    onModeChange: (NetworkSourceMode) -> Unit,
) {
    val modes = linkedMapOf(
        NetworkSourceMode.Cache to stringResource(R.string.config_network_source_cache),
        NetworkSourceMode.Automatic to stringResource(R.string.config_network_source_smart),
        NetworkSourceMode.StreamOnly to stringResource(R.string.config_network_source_low_storage),
    )
    val descriptions = mapOf(
        NetworkSourceMode.Cache to stringResource(R.string.config_network_source_cache_desc),
        NetworkSourceMode.Automatic to stringResource(R.string.config_network_source_smart_desc),
        NetworkSourceMode.StreamOnly to stringResource(R.string.config_network_source_low_storage_desc),
    )
    val keys = modes.keys.toList()

    CouiSpinnerItem(
        title = stringResource(R.string.config_network_source_mode),
        summary = descriptions.getValue(currentMode),
        items = modes.values.toList(),
        selectedIndex = keys.indexOf(currentMode).coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            keys.getOrNull(index)?.let(onModeChange)
        },
    )
}
